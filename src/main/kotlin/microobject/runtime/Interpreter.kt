@file:Suppress(
    "LiftReturnOrAssignment"
)

package microobject.runtime

import microobject.data.*
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.ResultSet
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.ReasonerRegistry
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner
import org.apache.jena.reasoner.rulesys.Rule
import org.semanticweb.HermiT.Reasoner
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserImpl
import org.semanticweb.owlapi.model.OWLNamedIndividual
import org.semanticweb.owlapi.model.OntologyConfigurator
import org.semanticweb.owlapi.reasoner.NodeSet
import java.io.BufferedReader
import java.io.File
import java.util.*

//There is probably something in the standard library for this pattern
class InterpreterBridge(var interpreter: Interpreter?)

class Interpreter(
    val stack: Stack<StackEntry>,               // This is the process stack
    private var heap: GlobalMemory,             // This is a map from objects to their heap memory
    private var simMemory: SimulationMemory,    // This is a map from simulation objects to their handler
    val staticInfo: StaticTable,                // Class table etc.
    private val outPath: String,                // Path to the output directory (e.g., /tmp/mo)
    private val back : String,                  // Background knowledge (Should be a string with OWL class definitions)
    private val rules : String,                 // Additional rules for jena
    private val domainPrefix : String           // Interface prefix for the domain
) {

    fun coreCopy() : Interpreter{
        val newHeap = mutableMapOf<LiteralExpr, Memory>()
        for(kv in heap.entries){
            newHeap[kv.key] = kv.value.toMap().toMutableMap()
        }
        return Interpreter(
            Stack<StackEntry>(),
            newHeap,
            mutableMapOf(),
            staticInfo,
            outPath, back, rules, domainPrefix
        )
    }

    private var debug = false
    private val prefix =
        """
                    PREFIX : <urn:>
                    PREFIX smol: <https://github.com/Edkamb/SemanticObjects#>
                    PREFIX prog: <urn:>
                    PREFIX run: <urn:>
                    PREFIX owl: <http://www.w3.org/2002/07/owl#> 
                    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> 
                    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> 
                    PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> 
                    PREFIX domain: <$domainPrefix> 
                """.trimIndent()

    fun query(str: String): ResultSet? {
        val out = "$prefix\n\n $str\n"

        var model : Model = ModelFactory.createOntologyModel()
        val uri = File("$outPath/output.ttl").toURL().toString()
        model.read(uri, "TTL")

        if(back != "") {
            println("Using background knowledge...")
            model = ModelFactory.createInfModel(ReasonerRegistry.getOWLReasoner(), model)
        }

        if(rules != "") {
            println("Loading generated builtin rules $rules...")
            val prefixes  =
            """@prefix : <urn:> .
               @prefix smol: <https://github.com/Edkamb/SemanticObjects#> .
               @prefix prog: <urn:> .
               @prefix domain: <$domainPrefix> .
               @prefix run: <urn:> .""".trimIndent()
            val reader = (prefixes+"\n"+rules).byteInputStream().bufferedReader()
            val rParsed = Rule.rulesParserFromReader(BufferedReader(reader))
            val reasoner: org.apache.jena.reasoner.Reasoner = GenericRuleReasoner(Rule.parseRules(rParsed))
            val infModel = ModelFactory.createInfModel(reasoner, model)
            //infModel.prepare()
            model = infModel
        }
        //println("execute CSSA: $out")
        val query = QueryFactory.create(out)
        val qexec = QueryExecutionFactory.create(query, model)

        return qexec.execSelect()
    }


    private fun owlQuery(str: String): NodeSet<OWLNamedIndividual> {
        val out = str.replace("prog:","urn:").replace("run:","urn:").replace("smol:","https://github.com/Edkamb/SemanticObjects#:")
        val m = OWLManager.createOWLOntologyManager()
        val ontology = m.loadOntologyFromOntologyDocument(File("$outPath/output.ttl"))
        val reasoner = Reasoner.ReasonerFactory().createReasoner(ontology)
        val parser = ManchesterOWLSyntaxParserImpl(OntologyConfigurator(), m.owlDataFactory)
        parser.setDefaultOntology(ontology)
        return reasoner.getInstances(parser.parseClassExpression(out))
    }

    private fun dump() {
        val output = File("$outPath/output.ttl")
        output.parentFile.mkdirs()
        if (!output.exists()) output.createNewFile()
        output.writeText(dumpTtl())
    }

    fun dumpTtl() : String{
        return State(stack, heap, simMemory, staticInfo, back).dump() // snapshot management goes here
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
        debug = false
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

        return !debug
    }

    private fun eval(stmt: Statement, stackMemory: Memory, heap: GlobalMemory, obj: LiteralExpr, id: Int) : Pair<StackEntry?, List<StackEntry>>{
        if(heap[obj] == null) throw Exception("This object is unknown: $obj")

        //get own local memory
        val heapObj: Memory = heap.getOrDefault(obj, mutableMapOf())

        when (stmt){
            is SuperStmt -> {
                val m = staticInfo.getSuperMethod(obj.tag, stmt.methodName) ?: throw Exception("super call impossible, no super method found.")
                val newMemory: Memory = mutableMapOf()
                newMemory["this"] = obj
                for (i in m.second.indices) {
                    newMemory[m.second[i]] = eval(stmt.params[i], stackMemory, heap, simMemory, obj)
                }
                return Pair(
                    StackEntry(StoreReturnStmt(stmt.target), stackMemory, obj, id),
                    listOf(StackEntry(m.first, newMemory, obj, Names.getStackId()))
                )
            }
            is AssignStmt -> {
                val res = eval(stmt.value, stackMemory, heap, simMemory, obj)
                when (stmt.target) {
                    is LocalVar -> stackMemory[stmt.target.name] = res
                    is OwnVar -> {
                        if (!(staticInfo.fieldTable[obj.tag]
                                ?: error("")).contains(stmt.target.name)
                        ) throw Exception("This field is unknown: ${stmt.target.name}")
                        heapObj[stmt.target.name] = res
                    }
                    is OthersVar -> {
                        val key = eval(stmt.target.expr, stackMemory, heap, simMemory, obj)

                        if(heap.containsKey(key)) {
                            val otherHeap = heap[key]
                                ?: throw Exception("This object is unknown: $key")
                            if (!(staticInfo.fieldTable[key.tag]
                                    ?: error("")).any{ it.first == stmt.target.name}
                            ) throw Exception("This field is unknown: $key")
                            otherHeap[stmt.target.name] = res
                        }
                        else if(simMemory.containsKey(key)) {
                            simMemory[key]!!.write(stmt.target.name, res)
                        }
                        else throw Exception("This object is unknown: $key")
                    }
                }
                return Pair(null, emptyList())
            }
            is CallStmt -> {
                val newObj = eval(stmt.callee, stackMemory, heap, simMemory, obj)
                val mt = staticInfo.methodTable[newObj.tag]
                    ?: throw Exception("This class is unknown: ${newObj.tag} when executing $stmt")
                val m = mt[stmt.method]
                    ?: throw Exception("This method is unknown: ${stmt.method}")
                val newMemory: Memory = mutableMapOf()
                newMemory["this"] = newObj
                for (i in m.second.indices) {
                    newMemory[m.second[i]] = eval(stmt.params[i], stackMemory, heap, simMemory, obj)
                }
                return Pair(
                    StackEntry(StoreReturnStmt(stmt.target), stackMemory, obj, id),
                    listOf(StackEntry(m.first, newMemory, newObj, Names.getStackId()))
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
                    newMemory[m[i].first] = eval(stmt.params[i], stackMemory, heap, simMemory, obj)
                }
                heap[name] = newMemory
                return Pair(StackEntry(AssignStmt(stmt.target, name), stackMemory, obj, id), listOf())
            }
            is SparqlStmt -> {
                val query = eval(stmt.query, stackMemory, heap, simMemory, obj)
                if (query.tag != "string")
                    throw Exception("Query is not a string: $query")
                var str = query.literal
                var i = 1
                for (expr in stmt.params) {
                    val p = eval(expr, stackMemory, heap, simMemory, obj)
                    //todo: check is this truly a run:literal
                    str = str.replace("%${i++}", "run:${p.literal}")
                }
                if (!staticInfo.fieldTable.containsKey("List") || !staticInfo.fieldTable["List"]!!.any { it.first == "content" } || !staticInfo.fieldTable["List"]!!.any { it.first == "next" }
                ) {
                    throw Exception("Could not find List class in this model")
                }
                dump()
                val results = query(str.removePrefix("\"").removeSuffix("\""))
                var list = LiteralExpr("null")
                if (results != null) {
                    for (r in results) {
                        val obres = r.getResource("?obj")
                            ?: throw Exception("Could not select ?obj variable from results, please select using only ?obj")
                        val name = Names.getObjName("List")
                        val newMemory: Memory = mutableMapOf()

                        val found = obres.toString().removePrefix("urn:")
                        for (ob in heap.keys) {
                            if (ob.literal == found) {
                                newMemory["content"] = LiteralExpr(found, ob.tag)
                            }
                        }
                        if (!newMemory.containsKey("content")) {
                            if(found.startsWith("\"")) newMemory["content"] = LiteralExpr(found, "string")
                            else if(found.matches("\\d+".toRegex())) newMemory["content"] = LiteralExpr(found, "integer")
                            else throw Exception("Query returned unknown object/literal: $found")
                        }
                        newMemory["next"] = list
                        heap[name] = newMemory
                        list = name
                    }
                }

                return Pair(StackEntry(AssignStmt(stmt.target, list), stackMemory, obj, id), listOf())
            }
            is OwlStmt -> {
                if (!staticInfo.fieldTable.containsKey("List") || !staticInfo.fieldTable["List"]!!.contains("content") || !staticInfo.fieldTable["List"]!!.contains(
                        "next"
                    )
                ) {
                    throw Exception("Could not find List class in this model")
                }
                if (stmt.query !is LiteralExpr || stmt.query.tag != "string") {
                    throw Exception("Please provide a string as the input to a derive statement")
                }

                //this is duplicated w.r.t. REPL until we figure out how to internally represent the KB
                dump()
                val res : NodeSet<OWLNamedIndividual> = owlQuery(stmt.query.literal)
                var list = LiteralExpr("null")
                if (res != null) {
                    for (r in res) {
                        val name = Names.getObjName("List")
                        val newMemory: Memory = mutableMapOf()

                        val found = r.toString().removePrefix("<urn:").removeSuffix(">")
                        for (ob in heap.keys) {
                            if (ob.literal == found) {
                                newMemory["content"] = LiteralExpr(found, ob.tag)
                            }
                        }
                        newMemory["next"] = list
                        heap[name] = newMemory
                        list = name
                    }
                }
                return Pair(StackEntry(AssignStmt(stmt.target, list), stackMemory, obj, id), listOf())
            }
            is ReturnStmt -> {
                val over = stack.pop()
                if (over.active is StoreReturnStmt) {
                    val res = eval(stmt.value, stackMemory, heap, simMemory, obj)
                    return Pair(StackEntry(AssignStmt(over.active.target, res), over.store, over.obj, id), listOf())
                }
                if (over.active is SequenceStmt && over.active.first is StoreReturnStmt) {
                    val active = over.active.first
                    val next = over.active.second
                    val res = eval(stmt.value, stackMemory, heap, simMemory, obj)
                    return Pair(
                        StackEntry(appendStmt(AssignStmt(active.target, res), next), over.store, over.obj, id),
                        listOf()
                    )
                }
                throw Exception("Malformed heap")
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
                val simObj = SimulatorObject(stmt.path,stmt.params.map { Pair(it.name,eval(
                    it.expr,
                    stackMemory,
                    heap, simMemory,
                    obj
                )) }.toMap().toMutableMap())
                val name = Names.getObjName("CoSimulation")
                simMemory[name] = simObj
                return Pair(StackEntry(AssignStmt(stmt.target, name), stackMemory, obj, id), listOf())
            }
            is TickStmt -> {
                val target = eval(stmt.fmu, stackMemory, heap, simMemory, obj)
                if(!simMemory.containsKey(target)) throw Exception("Object $target is no a simulation object")
                val tickTime = eval(stmt.tick, stackMemory, heap, simMemory, obj)
                simMemory[target]!!.tick(tickTime.literal.toInt())
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
                    return expr.params.fold(LiteralExpr("0"), { acc, nx ->
                        val enx = eval(nx, stack, heap, simMemory, obj)
                        LiteralExpr((acc.literal.removePrefix("urn:").toInt() + enx.literal.removePrefix("urn:").toInt()).toString(), "integer")
                    })
                }
                if (expr.Op == Operator.MULT) {
                    return expr.params.fold(LiteralExpr("1"), { acc, nx ->
                        val enx = eval(nx, stack, heap, simMemory, obj)
                        LiteralExpr((acc.literal.removePrefix("urn:").toInt() * enx.literal.removePrefix("urn:").toInt()).toString(), "integer")
                    })
                }
                if (expr.Op == Operator.MINUS) {
                    if (expr.params.size != 2) throw Exception("Operator.MINUS requires two parameters")
                    val first = eval(expr.params[0], stack, heap, simMemory, obj)
                    val second = eval(expr.params[1], stack, heap, simMemory, obj)
                    return LiteralExpr((first.literal.removePrefix("urn:").toInt() - second.literal.removePrefix("urn:").toInt()).toString(), "integer")
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