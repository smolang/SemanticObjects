@file:Suppress(
    "LiftReturnOrAssignment"
)

package no.uio.microobject.runtime

import com.influxdb.client.kotlin.InfluxDBClientKotlin
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import com.sksamuel.hoplite.ConfigLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.uio.microobject.data.*
import no.uio.microobject.main.Settings
import no.uio.microobject.type.*
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.ResultSet
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.ShaclValidator
import org.apache.jena.shacl.Shapes
import org.semanticweb.HermiT.Reasoner
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserImpl
import org.semanticweb.owlapi.model.OWLNamedIndividual
import org.semanticweb.owlapi.model.OntologyConfigurator
import org.semanticweb.owlapi.reasoner.NodeSet
import java.io.File
import java.io.FileWriter
import java.util.*

data class InfluxDBConnection(val url : String, val org : String, val token : String, val bucket : String){
    private var influxDBClient : InfluxDBClientKotlin? = null
    private fun connect(){
        influxDBClient = InfluxDBClientKotlinFactory.create(url, token.toCharArray(), org)
    }
    fun queryOneSeries(flux : String) : List<Double>{
        connect()
        val results = influxDBClient!!.getQueryKotlinApi().query(flux.replace("\\\"","\""))
        var next = emptyList<Double>()
        runBlocking {
            launch(Dispatchers.Unconfined) {
                next = results.consumeAsFlow().toList().map { it.value as Double }
            }
        }
        disconnect()
        return next
    }
    private fun disconnect(){
        influxDBClient?.close()
    }
}
//There is probably something in the standard library for this pattern
class InterpreterBridge(var interpreter: Interpreter?)

class Interpreter(
    val stack: Stack<StackEntry>,               // This is the process stack
    var heap: GlobalMemory,             // This is a map from objects to their heap memory
    var simMemory: SimulationMemory,    // This is a map from simulation objects to their handler
    val staticInfo: StaticTable,                // Class table etc.
    val settings : Settings,                    // Settings from the user
    val rules : String,                 // Additional rules for jena
) {
    private var debug = false

    // TripleManager used to provide virtual triples etc.
    val tripleManager : TripleManager = TripleManager(settings, staticInfo, this)

    //evaluates a call on cl.nm on thisVar
    //Must ONLY be called if nm is checked to have no side-effects (i.e., is rule)
    //First return value is the created object, the second the return value
    fun evalCall(objName: String, className: String, metName: String): Pair<LiteralExpr, LiteralExpr> {
        //Construct initial state
        val classStmt =
            staticInfo.methodTable[className]
                ?: throw Exception("Error during builtin generation")
        val met = classStmt[metName] ?: throw Exception("Error during builtin generation")
        val mem: Memory = mutableMapOf()

        val obj = LiteralExpr(
            objName,
            heap.keys.first { it.literal == objName }.tag //retrieve real class, because rule methods can be inheritated
        )
        mem["this"] = obj
        val myId = Names.getStackId()
        val se = StackEntry(met.stmt, mem, obj, myId)
        stack.push(se)

        //Run your own mini-REPL
        //But 1. We ignore `breakpoint` and
        //    2. we do not terminate the interpreter but stop at the return of the added stack frame so we get the return value
        while (true) {
            if (stack.peek().active is ReturnStmt && stack.peek().id == myId) {
                //Evaluate final return expressions
                val resStmt = stack.peek().active as ReturnStmt
                val res = resStmt.value
                val topmost = evalTopMost(res)
                stack.pop() //clean up
                return Pair(obj, topmost)
            }
            makeStep()
        }
    }

    fun evalClassLevel(expr: Expression, obj: LiteralExpr): Any {
        return eval(expr, mutableMapOf(), heap, simMemory, obj)
    }

    // Run SPARQL query (str)
    fun query(str: String): ResultSet? {
        // Adding prefixes to the query
        var queryWithPrefixes = ""
        for ((key, value) in settings.prefixMap()) queryWithPrefixes += "PREFIX $key: <$value>\n"
        queryWithPrefixes += str

        val model = tripleManager.getCompleteModel()
        if(settings.verbose) println("execute ISSA\n: $queryWithPrefixes")
        val query = QueryFactory.create(queryWithPrefixes)
        val qexec = QueryExecutionFactory.create(query, model)

        return qexec.execSelect()
    }


    // Run OWL query and return all instances of the described class.
    // str should be in Manchester syntax
    fun owlQuery(str: String): NodeSet<OWLNamedIndividual> {
        val out : String = settings.replaceKnownPrefixesNoColon(str.removeSurrounding("\""))
        val m = OWLManager.createOWLOntologyManager()
        val ontology = tripleManager.getCompleteOntology()
        val reasoner = Reasoner.ReasonerFactory().createReasoner(ontology)
        val parser = ManchesterOWLSyntaxParserImpl(OntologyConfigurator(), m.owlDataFactory)
        parser.setDefaultOntology(ontology)
        val expr = parser.parseClassExpression(out)
        return reasoner.getInstances(expr)
    }

    // Dump all triples in the virtual model to ${settings.outpath}/output.ttl
    internal fun dump() {
        val model = tripleManager.getCompleteModel()
        File(settings.outpath).mkdirs()
        File("${settings.outpath}/output.ttl").createNewFile()
        model.write(FileWriter("${settings.outpath}/output.ttl"),"TTL")
    }

    fun evalTopMost(expr: Expression) : LiteralExpr{
        if(stack.isEmpty()) return LiteralExpr("ERROR") // program terminated
        return eval(expr, stack.peek().store, heap, simMemory, stack.peek().obj)
    }

    /*
    This executes exactly one step of the interpreter.
    Note that rewritings are also one executing steps
     */
    fun makeStep() : Boolean {
        if(stack.isEmpty()) return false // program terminated

        //get current frame
        val current = stack.pop()

        //evaluate it
        val res = eval(current.active, current.store, heap, current.obj, current.id)

        //if there frame is not finished, push its modification back
        if(res.first != null){
            stack.push(res.first)
        }

        //in case we spawn more frames, push them as well
        for( se in res.second){
            stack.push(se)
        }

        if(debug){
            debug = false
            return false
        }
        return true
    }

    private fun prepareSPARQL(queryExpr : Expression, params : List<Expression>, stackMemory: Memory, heap: GlobalMemory, obj: LiteralExpr) : String{
        val query = eval(queryExpr, stackMemory, heap, simMemory, obj)
        if (query.tag != STRINGTYPE)
            throw Exception("Query is not a string: $query")
        var str = query.literal
        var i = 1
        for (expr in params) {
            val p = eval(expr, stackMemory, heap, simMemory, obj)
            //todo: check is this truly a run:literal
            if(p.tag == INTTYPE)
                str = str.replace("%${i++}", "\"${p.literal}\"^^xsd:integer")
            else
                str = str.replace("%${i++}", "run:${p.literal}")
        }
        if (!staticInfo.fieldTable.containsKey("List") || !staticInfo.fieldTable["List"]!!.any { it.name == "content" } || !staticInfo.fieldTable["List"]!!.any { it.name == "next" }
        ) {
            throw Exception("Could not find List class in this model")
        }
        return str
    }

    private fun eval(stmt: Statement, stackMemory: Memory, heap: GlobalMemory, obj: LiteralExpr, id: Int) : Pair<StackEntry?, List<StackEntry>>{
        if(heap[obj] == null)
            throw Exception("This object is unknown: $obj")

        //get own local memory
        val heapObj: Memory = heap.getOrDefault(obj, mutableMapOf())

        when (stmt){
            is SuperStmt -> {
                if(obj.tag !is BaseType) throw Exception("This object is unknown: $obj")
                val m = staticInfo.getSuperMethod(obj.tag.name, stmt.methodName) ?: throw Exception("super call impossible, no super method found.")
                val newMemory: Memory = mutableMapOf()
                newMemory["this"] = obj
                for (i in m.params.indices) {
                    newMemory[m.params[i]] = eval(stmt.params[i], stackMemory, heap, simMemory, obj)
                }
                return Pair(
                    StackEntry(StoreReturnStmt(stmt.target), stackMemory, obj, id),
                    listOf(StackEntry(m.stmt, newMemory, obj, Names.getStackId()))
                )
            }
            is AssignStmt -> {
                val res = eval(stmt.value, stackMemory, heap, simMemory, obj)
                when (stmt.target) {
                    is LocalVar -> stackMemory[stmt.target.name] = res
                    is OwnVar -> {
                        val got = staticInfo.fieldTable[(obj.tag as BaseType).name] ?: throw Exception("Cannot find class ${obj.tag.name}")
                        if (!got.map {it.name} .contains(stmt.target.name))
                            throw Exception("This field is unknown: ${stmt.target.name}")
                        heapObj[stmt.target.name] = res
                    }
                    is OthersVar -> {
                        val key = eval(stmt.target.expr, stackMemory, heap, simMemory, obj)

                        when {
                            heap.containsKey(key) -> {
                                val otherHeap = heap[key]
                                    ?: throw Exception("This object is unknown: $key")
                                if (!(staticInfo.fieldTable[(key.tag as BaseType).name]
                                        ?: error("")).any{ it.name == stmt.target.name}
                                ) throw Exception("This field is unknown: $key")
                                otherHeap[stmt.target.name] = res
                            }
                            simMemory.containsKey(key) -> {
                                simMemory[key]!!.write(stmt.target.name, res)
                            }
                            else -> throw Exception("This object is unknown: $key")
                        }
                    }
                }
                return Pair(null, emptyList())
            }
            is CallStmt -> {
                val newObj = eval(stmt.callee, stackMemory, heap, simMemory, obj)
                val mt = staticInfo.methodTable[(newObj.tag as BaseType).name]
                    ?: throw Exception("This class is unknown: ${newObj.tag} when executing $stmt")
                val m = mt[stmt.method]
                    ?: throw Exception("This method is unknown: ${stmt.method}")
                val newMemory: Memory = mutableMapOf()
                newMemory["this"] = newObj
                for (i in m.params.indices) {
                    newMemory[m.params[i]] = eval(stmt.params[i], stackMemory, heap, simMemory, obj)
                }
                return Pair(
                    StackEntry(StoreReturnStmt(stmt.target), stackMemory, obj, id),
                    listOf(StackEntry(m.stmt, newMemory, newObj, Names.getStackId()))
                )
            }
            is CreateStmt -> {
                val name = Names.getObjName(stmt.className)
                val m =
                    staticInfo.fieldTable[stmt.className] ?: throw Exception("This class is unknown: ${stmt.className}")
                val newMemory: Memory = mutableMapOf()
                if (m.size != stmt.params.size) throw Exception(
                    "Creation of an instance of class ${stmt.className} failed, mismatched number of parameters: $stmt. Requires: ${m.size}"
                )
                for (i in m.indices) {
                    if(!m[i].name.startsWith("__"))
                    newMemory[m[i].name] = eval(stmt.params[i], stackMemory, heap, simMemory, obj)
                }
                if(stmt.modeling != null) {
                    val str = eval(stmt.modeling, stackMemory, heap, simMemory, obj).literal
                    val rdfName = Names.getNodeName()
                    newMemory["__describe"] = LiteralExpr(rdfName + " " + str.removeSurrounding("\"") , STRINGTYPE)
                    newMemory["__models"] = LiteralExpr(rdfName , STRINGTYPE)
                }
                heap[name] = newMemory
                return Pair(StackEntry(AssignStmt(stmt.target, name, declares = stmt.declares), stackMemory, obj, id), listOf())
            }
            is AccessStmt -> { // should be refactored once we support more modes
                if(stmt.mode is InfluxDBMode){
                    val path = stmt.mode.config.removeSurrounding("\"")
                    val config = ConfigLoader().loadConfigOrThrow<InfluxDBConnection>(File(path))
                    val vals = config.queryOneSeries((stmt.query as LiteralExpr).literal.removeSurrounding("\""))
                    var list = LiteralExpr("null")
                    for(r in vals){
                        val name = Names.getObjName("List")
                        val newMemory: Memory = mutableMapOf()
                        newMemory["content"] = LiteralExpr(r.toString(), DOUBLETYPE)
                        newMemory["next"] = list
                        heap[name] = newMemory
                        list = name
                    }
                    return Pair(StackEntry(AssignStmt(stmt.target, list, declares = stmt.declares), stackMemory, obj, id), listOf())
                }

                /* stmt.mode == SparqlMode */
                val str = prepareSPARQL(stmt.query, stmt.params, stackMemory, heap, obj)
                val results = query(str.removePrefix("\"").removeSuffix("\""))
                var list = LiteralExpr("null")
                if (results != null) {
                    for (r in results) {
                        val obres = r.get("?obj")
                            ?: throw Exception("Could not select ?obj variable from results, please select using only ?obj")
                        val name = Names.getObjName("List")
                        val newMemory: Memory = mutableMapOf()

                        val found = obres.toString().removePrefix(settings.runPrefix)
                        val objNameCand = if(found.startsWith("\\\"")) found.replace("\\\"","\"") else found
                        for (ob in heap.keys) {
                            if (ob.literal == objNameCand) {
                                newMemory["content"] = LiteralExpr(objNameCand, ob.tag)
                                break
                            }
                        }
                        if (!newMemory.containsKey("content")) {
                            if(obres.isLiteral && obres.asNode().literalDatatype == XSDDatatype.XSDstring) newMemory["content"] = LiteralExpr("\""+found+"\"", STRINGTYPE)
                            else if(objNameCand.matches("\\d+".toRegex())) newMemory["content"] = LiteralExpr(found, INTTYPE)
                            else if(objNameCand.matches("\\d+.\\d+".toRegex())) newMemory["content"] = LiteralExpr(found, DOUBLETYPE)
                            else throw Exception("Query returned unknown object/literal: $found")
                        }
                        newMemory["next"] = list
                        heap[name] = newMemory
                        list = name
                    }
                }
                return Pair(StackEntry(AssignStmt(stmt.target, list, declares = stmt.declares), stackMemory, obj, id), listOf())
            }
            is ConstructStmt -> {
                val str = prepareSPARQL(stmt.query, stmt.params, stackMemory, heap, obj)
                val results = query(str.removePrefix("\"").removeSuffix("\""))
                var list = LiteralExpr("null")
                val targetType = stmt.target.getType()
                if (targetType !is ComposedType || targetType.getPrimary().getNameString() != "List" || targetType.params.first() !is BaseType )
                    throw Exception("Could not perform construction from query results: unknown type $targetType")
                val className = (targetType.params.first() as BaseType).name
                if (results != null) {
                    for (r in results) {
                        val newListName = Names.getObjName("List")
                        val newListMemory: Memory = mutableMapOf()

                        val m = staticInfo.fieldTable[className] ?: throw Exception("This class is unknown: $className")
                        val newObjName = Names.getObjName(className)
                        val newObjMemory: Memory = mutableMapOf()
                        for(f in m){
                            if(!r.varNames().asSequence().contains(f.name))
                                throw Exception("Could find variable for field ${f.name} in query $str")
                            val extractedName  = r.getResource(f.name).toString().removePrefix(settings.runPrefix)
                            if(!Type.isAtomic(f.type)) {
                                val foundAny = heap.keys.any { it.literal == extractedName }
                                if (!foundAny)
                                    throw Exception("Query returned unknown object/literal: $extractedName")
                            }
                            newObjMemory[f.name] = LiteralExpr(extractedName, f.type)
                        }
                        heap[newObjName] = newObjMemory
                        newListMemory["content"] = newObjName
                        newListMemory["next"] = list
                        heap[newListName] = newListMemory
                        list = newListName
                    }
                }
                return Pair(StackEntry(AssignStmt(stmt.target, list, declares = stmt.declares), stackMemory, obj, id), listOf())
            }
            is ValidateStmt -> {
                if(stmt.query !is LiteralExpr) throw Exception("validate takes a file path in a String as a parameter")
                val fileName = stmt.query.literal.removeSurrounding("\"")
                val file = File(fileName)
                if(!file.exists()) throw Exception("file $fileName does not exist")
                val newFile = File("${settings.outpath}/shape.ttl")
                if(!newFile.exists()) {
                    File(settings.outpath).mkdirs()
                    newFile.createNewFile()
                }
                newFile.writeText(settings.prefixes() + "\n"+ settings.getHeader() + "\n@prefix sh: <http://www.w3.org/ns/shacl#>.\n")
                newFile.appendText(file.readText())
                val shapesGraph = RDFDataMgr.loadGraph("${settings.outpath}/shape.ttl")
                val dataGraph = tripleManager.getCompleteGraph()

                val shapes: Shapes = Shapes.parse(shapesGraph)

                val report = ShaclValidator.get().validate(shapes, dataGraph)
                val resLit = if(report.conforms()) TRUEEXPR else FALSEEXPR
                return Pair(StackEntry(AssignStmt(stmt.target, resLit, declares = stmt.declares), stackMemory, obj, id), listOf())
            }
            is OwlStmt -> {
                if (!staticInfo.fieldTable.containsKey("List") ||
                    !staticInfo.fieldTable["List"]!!.any {  it.name == "content" } ||
                    !staticInfo.fieldTable["List"]!!.any {  it.name == "next" }) {
                    throw Exception("Could not find List class in this model")
                }
                if (stmt.query !is LiteralExpr || stmt.query.tag != STRINGTYPE) {
                    throw Exception("Please provide a string as the input to a derive statement")
                }

                val res : NodeSet<OWLNamedIndividual> = owlQuery(stmt.query.literal)
                var list = LiteralExpr("null")
                    for (r in res) {
                        val name = Names.getObjName("List")
                        val newMemory: Memory = mutableMapOf()
                        val found = r.toString().removePrefix("Node( <").split("#")[1].removeSuffix("> )")

                        val foundAny = heap.keys.firstOrNull { it.literal == found }
                        if(foundAny != null) newMemory["content"] = LiteralExpr(found, foundAny.tag)
                        else {
                            if(found.startsWith("\"")) newMemory["content"] = LiteralExpr(found, STRINGTYPE)
                            else if(found.matches("\\d+".toRegex())) newMemory["content"] = LiteralExpr(found, INTTYPE)
                            else if(found.matches("\\d+.\\d+".toRegex())) newMemory["content"] = LiteralExpr(found, DOUBLETYPE)
                            else throw Exception("Concept returned unknown object/literal: $found")
                        }

                        newMemory["next"] = list
                        heap[name] = newMemory
                        list = name
                    }
                return Pair(StackEntry(AssignStmt(stmt.target, list, declares = stmt.declares), stackMemory, obj, id), listOf())
            }
            is ReturnStmt -> {
                val over = stack.pop()
                if (over.active is StoreReturnStmt) {
                    val res = eval(stmt.value, stackMemory, heap, simMemory, obj)
                    return Pair(StackEntry(AssignStmt(over.active.target, res, declares = null), over.store, over.obj, over.id), listOf())
                }
                if (over.active is SequenceStmt && over.active.first is StoreReturnStmt) {
                    val active = over.active.first
                    val next = over.active.second
                    val res = eval(stmt.value, stackMemory, heap, simMemory, obj)
                    return Pair(
                        StackEntry(appendStmt(AssignStmt(active.target, res, declares = null), next), over.store, over.obj, over.id),
                        listOf()
                    )
                }
                throw Exception("Malformed heap")
            }
            is DestroyStmt -> {
                val res = eval(stmt.expr, stackMemory, heap, simMemory, obj)
                if(!heap.containsKey(res) || res.literal == "null")
                    throw Exception("Trying to destroy null or an unknown object: $res")
                heap.remove(res)
                //Replace name with ERROR. This is needed so the RDF dump does have dangling pointers and we cna derive weird things in the open world
                for( mem in heap.values ){
                    var rem :String? = null
                    for( kv in mem ){
                        if (kv.value == res) {
                            rem = kv.key
                            break
                        }
                    }
                    if(rem != null) mem[rem] = LiteralExpr("null")
                }
                for( entry in stack ){
                    var rem :String? = null
                    for( kv in entry.store ){
                        if (kv.value == res) {
                            rem = kv.key
                            break
                        }
                    }
                    if(rem != null) entry.store[rem] = LiteralExpr("null")
                }
                return Pair(null, emptyList())
            }
            is IfStmt -> {
                val res = eval(stmt.guard, stackMemory, heap, simMemory, obj)
                if (res == TRUEEXPR) return Pair(
                    StackEntry(stmt.thenBranch, stackMemory, obj, id),
                    listOf()
                )
                else return Pair(
                    StackEntry(stmt.elseBranch, stackMemory, obj, id),
                    listOf()
                )
            }
            is WhileStmt -> {
                return Pair(
                    StackEntry(
                        IfStmt(
                            stmt.guard,
                            appendStmt(stmt.loopBody, stmt),
                            SkipStmt()
                        ), stackMemory, obj, id
                    ), listOf()
                )
            }
            is SkipStmt -> {
                return Pair(null, emptyList())
            }
            is DebugStmt -> {
                debug = true; return Pair(null, emptyList())
            }
            is PrintStmt -> {
                println(eval(stmt.expr, stackMemory, heap, simMemory, obj))
                return Pair(null, emptyList())
            }
            is SimulationStmt -> {
                val simObj = SimulatorObject(stmt.path, stmt.params.associate {
                    Pair(
                        it.name, eval(
                            it.expr,
                            stackMemory,
                            heap, simMemory,
                            obj
                        )
                    )
                }.toMutableMap())
                val name = Names.getObjName("CoSimulation")
                simMemory[name] = simObj
                return Pair(StackEntry(AssignStmt(stmt.target, name, declares = stmt.declares), stackMemory, obj, id), listOf())
            }
            is TickStmt -> {
                val target = eval(stmt.fmu, stackMemory, heap, simMemory, obj)
                if(!simMemory.containsKey(target)) throw Exception("Object $target is no a simulation object")
                val tickTime = eval(stmt.tick, stackMemory, heap, simMemory, obj)
                simMemory[target]!!.tick(tickTime.literal.toDouble())
                return Pair(null, emptyList())
            }
            is SequenceStmt -> {
                if (stmt.first is ReturnStmt) return eval(stmt.first, stackMemory, heap, obj, id)
                val res = eval(stmt.first, stackMemory, heap, obj, id)
                if (res.first != null) {
                    val newStmt = appendStmt(res.first!!.active, stmt.second)
                    return Pair(StackEntry(newStmt, res.first!!.store, res.first!!.obj, id), res.second)
                } else return Pair(StackEntry(stmt.second, stackMemory, obj, id), res.second)
            }
            else -> throw Exception("This kind of statement is not implemented yet: $stmt")
        }
    }


    private fun eval(expr: Expression, stack: Memory, heap: GlobalMemory, simMemory: SimulationMemory, obj: LiteralExpr) : LiteralExpr {
        if(heap[obj] == null) throw Exception("This object is unknown: $obj$")
        val heapObj: Memory = heap.getOrDefault(obj, mutableMapOf())
        when (expr) {
            is LiteralExpr -> return expr
            is ArithExpr -> {
                if (expr.Op == Operator.EQ) {
                    if (expr.params.size != 2) throw Exception("Operator.EQ requires two parameters")
                    val first = eval(expr.params[0], stack, heap, simMemory, obj)
                    val second = eval(expr.params[1], stack, heap, simMemory, obj)
                    if (first == second) return TRUEEXPR
                    else return FALSEEXPR
                }
                if (expr.Op == Operator.NEQ) {
                    if (expr.params.size != 2) throw Exception("Operator.NEQ requires two parameters")
                    val first = eval(expr.params[0], stack, heap, simMemory, obj)
                    val second = eval(expr.params[1], stack, heap, simMemory, obj)
                    if (first == second) return FALSEEXPR
                    else return TRUEEXPR
                }
                if (expr.Op == Operator.GEQ) {
                    if (expr.params.size != 2) throw Exception("Operator.GEQ requires two parameters")
                    val first = eval(expr.params[0], stack, heap, simMemory, obj)
                    val second = eval(expr.params[1], stack, heap, simMemory, obj)
                    if (first.literal.toInt() >= second.literal.toInt()) return TRUEEXPR
                    else return FALSEEXPR
                }
                if (expr.Op == Operator.LEQ) {
                    if (expr.params.size != 2) throw Exception("Operator.LEQ requires two parameters")
                    val first = eval(expr.params[0], stack, heap, simMemory, obj)
                    val second = eval(expr.params[1], stack, heap, simMemory, obj)
                    if (first.literal.toInt() <= second.literal.toInt()) return TRUEEXPR
                    else return FALSEEXPR
                }
                if (expr.Op == Operator.GT) {
                    if (expr.params.size != 2) throw Exception("Operator.GT requires two parameters")
                    val first = eval(expr.params[0], stack, heap, simMemory, obj)
                    val second = eval(expr.params[1], stack, heap, simMemory, obj)
                    if (first.literal.toInt() > second.literal.toInt()) return TRUEEXPR
                    else return FALSEEXPR
                }
                if (expr.Op == Operator.LT) {
                    if (expr.params.size != 2) throw Exception("Operator.LT requires two parameters")
                    val first = eval(expr.params[0], stack, heap, simMemory, obj)
                    val second = eval(expr.params[1], stack, heap, simMemory, obj)
                    if (first.literal.toInt() < second.literal.toInt()) return TRUEEXPR
                    else return FALSEEXPR
                }
                if (expr.Op == Operator.AND) {
                    if (expr.params.size != 2) throw Exception("Operator.AND requires two parameters")
                    val first = eval(expr.params[0], stack, heap, simMemory, obj)
                    val second = eval(expr.params[1], stack, heap, simMemory, obj)
                    if (first == TRUEEXPR && second == TRUEEXPR) return TRUEEXPR
                    else return FALSEEXPR
                }
                if (expr.Op == Operator.OR) {
                    if (expr.params.size != 2) throw Exception("Operator.OR requires two parameters")
                    val first = eval(expr.params[0], stack, heap, simMemory, obj)
                    val second = eval(expr.params[1], stack, heap, simMemory, obj)
                    if (first == TRUEEXPR || second == TRUEEXPR) return TRUEEXPR
                    else return FALSEEXPR
                }
                if (expr.Op == Operator.NOT) {
                    if (expr.params.size != 1) throw Exception("Operator.NOT requires one parameter")
                    val first = eval(expr.params[0], stack, heap, simMemory, obj)
                    if (first == FALSEEXPR) return TRUEEXPR
                    else return FALSEEXPR
                }
                if (expr.Op == Operator.PLUS) {
                    val first = eval(expr.params.first(), stack, heap, simMemory, obj)
                    if(first.tag == DOUBLETYPE)
                        return expr.params.subList(1, expr.params.count()).fold(first) { acc, nx ->
                            val enx = eval(nx, stack, heap, simMemory, obj)
                            LiteralExpr(
                                (acc.literal.removePrefix("urn:").toDouble() + enx.literal.removePrefix("urn:")
                                    .toDouble()).toString(), DOUBLETYPE
                            )
                        }
                    else return expr.params.subList(1, expr.params.count()).fold(first) { acc, nx ->
                        val enx = eval(nx, stack, heap, simMemory, obj)
                        LiteralExpr(
                            (acc.literal.removePrefix("urn:").toInt() + enx.literal.removePrefix("urn:")
                                .toInt()).toString(), INTTYPE
                        )
                    }
                }
                if (expr.Op == Operator.MULT) {
                    val first = eval(expr.params.first(), stack, heap, simMemory, obj)
                    if(first.tag == DOUBLETYPE)
                        return expr.params.subList(1, expr.params.count()).fold(first) { acc, nx ->
                            val enx = eval(nx, stack, heap, simMemory, obj)
                            LiteralExpr(
                                (acc.literal.removePrefix("urn:").toDouble() * enx.literal.removePrefix("urn:")
                                    .toDouble()).toString(), DOUBLETYPE
                            )
                        }
                    else return expr.params.subList(1, expr.params.count()).fold(first) { acc, nx ->
                        val enx = eval(nx, stack, heap, simMemory, obj)
                        LiteralExpr(
                            (acc.literal.removePrefix("urn:").toInt() * enx.literal.removePrefix("urn:")
                                .toInt()).toString(), INTTYPE
                        )
                    }
                }
                if (expr.Op == Operator.DIV) {
                    if (expr.params.size != 2) throw Exception("Operator.DIV requires two parameters")
                    val enx1 = eval(expr.params[0], stack, heap, simMemory, obj)
                    val enx2 = eval(expr.params[1], stack, heap, simMemory, obj)
                    if(enx1.tag == DOUBLETYPE)
                        return LiteralExpr((enx1.literal.removePrefix("urn:").toDouble() / enx2.literal.removePrefix("urn:").toDouble()).toString(), DOUBLETYPE)
                    else
                        return LiteralExpr((enx1.literal.removePrefix("urn:").toInt() / enx2.literal.removePrefix("urn:").toInt()).toString(), INTTYPE)
                }
                if (expr.Op == Operator.MOD) {
                    if (expr.params.size != 2) throw Exception("Operator.MOD requires two parameters")
                    val enx1 = eval(expr.params[0], stack, heap, simMemory, obj)
                    val enx2 = eval(expr.params[1], stack, heap, simMemory, obj)
                    if(enx1.tag == DOUBLETYPE)
                        return LiteralExpr((enx1.literal.removePrefix("urn:").toDouble() % enx2.literal.removePrefix("urn:").toInt()).toString(), DOUBLETYPE)
                    else
                        return LiteralExpr((enx1.literal.removePrefix("urn:").toInt() % enx2.literal.removePrefix("urn:").toInt()).toString(), INTTYPE)
                }
                if (expr.Op == Operator.MINUS) {
                    if (expr.params.size != 2) throw Exception("Operator.MINUS requires two parameters")
                    val enx1 = eval(expr.params[0], stack, heap, simMemory, obj)
                    val enx2 = eval(expr.params[1], stack, heap, simMemory, obj)
                    if(enx1.tag == DOUBLETYPE)
                        return LiteralExpr((enx1.literal.removePrefix("urn:").toDouble() - enx2.literal.removePrefix("urn:").toDouble()).toString(), DOUBLETYPE)
                    else
                        return LiteralExpr((enx1.literal.removePrefix("urn:").toInt() - enx2.literal.removePrefix("urn:").toInt()).toString(), INTTYPE)
                }
                throw Exception("This kind of operator is not implemented yet")
            }
            is OwnVar -> {
                return heapObj.getOrDefault(expr.name, LiteralExpr("ERROR"))
            }
            is OthersVar -> {
                val oObj = eval(expr.expr, stack, heap, simMemory, obj)
                if(heap.containsKey(oObj)) return heap[oObj]!!.getOrDefault(expr.name, LiteralExpr("ERROR"))
                if(simMemory.containsKey(oObj)) return simMemory[oObj]!!.read(expr.name)
                throw Exception("Unknown object $oObj stored in $expr")
            }
            is LocalVar -> {
                return stack.getOrDefault(expr.name, LiteralExpr("ERROR"))
            }
            else -> throw Exception("This kind of expression is not implemented yet")
        }
    }

    override fun toString() : String =
"""
Global store : $heap
Stack:
${stack.joinToString(
    separator = "",
    transform = { "Prc${it.id}@${it.obj}:\n\t" + it.store.toString() + "\nStatement:\n\t" + it.active.toString() + "\n" })}
""".trimIndent()

    fun terminate() {
        for(sim in simMemory.values)
            sim.terminate()
    }
}
