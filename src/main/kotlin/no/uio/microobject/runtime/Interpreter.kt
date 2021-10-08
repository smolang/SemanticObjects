@file:Suppress(
    "LiftReturnOrAssignment"
)

package no.uio.microobject.runtime

import com.influxdb.client.kotlin.InfluxDBClientKotlin
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import com.sksamuel.hoplite.ConfigLoader
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.uio.microobject.data.*
import no.uio.microobject.main.Settings
import no.uio.microobject.type.*
import org.apache.commons.io.IOUtils
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.*
import org.apache.jena.graph.impl.GraphBase
import org.apache.jena.ontology.*
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.ResultSet
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.ReasonerRegistry
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner
import org.apache.jena.reasoner.rulesys.Rule
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.ShaclValidator
import org.apache.jena.shacl.Shapes
import org.apache.jena.util.iterator.*
import org.semanticweb.HermiT.Reasoner
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserImpl
import org.semanticweb.owlapi.model.OntologyConfigurator
import org.semanticweb.owlapi.model.OWLNamedIndividual
import org.semanticweb.owlapi.reasoner.NodeSet

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
                /*for(n in next){
                    // println(n.field + " " + n.measurement + " " + n.time + " " + n.measurement + " " + n.value )
                     println(n)
                }*/
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



// A custom type of (nice)iterator which takes a list as input and iterates over them.
// It iterates through all elements in the list from start to end.
class TripleListIterator(tripleList : List<Triple>) : NiceIterator<Triple>() {
    val tripleList : List<Triple> = tripleList
    var listIndex : Int = 0  // index of next element

    override public fun hasNext(): Boolean {
        if (listIndex < tripleList.size) return true
        return false
    }

    override public fun next(): Triple {
        this.listIndex = this.listIndex + 1
        return tripleList[(listIndex-1)]
    }
}

// Helper method to crate triple with URIs in all three positions
fun uriTriple(s : String, p : String, o : String) : Triple {
    return Triple(NodeFactory.createURI(s), NodeFactory.createURI(p), NodeFactory.createURI(o))
}

// If searchTriple matches candidateTriple, then candidateTriple will be added to matchList
fun addIfMatch(candidateTriple : Triple, searchTriple : Triple, matchList : MutableList<Triple> ) : Unit {
    if (searchTriple.matches(candidateTriple)) matchList.add(candidateTriple)
}



// Graph representing the static table
class StaticTableGraph(interpreter: Interpreter) : GraphBase() {
    var interpreter : Interpreter = interpreter

    // Returns an iterator of all triples in the static table that matches searchTriple
    // graphBaseFind only constructs the triples that match searchTriple.
    override protected fun graphBaseFind(searchTriple : Triple): ExtendedIterator<Triple> {
        val prefixMap : HashMap<String, String> = interpreter.prefixMap
        val fieldTable : Map<String,FieldEntry> = interpreter.staticInfo.fieldTable
        val methodTable : Map<String,Map<String,MethodEntry>> = interpreter.staticInfo.methodTable
        val hierarchy : MutableMap<String, MutableSet<String>> = interpreter.staticInfo.hierarchy

        // Prefixes
        val rdf = prefixMap.get("rdf")
        val rdfs = prefixMap.get("rdfs")
        val owl = prefixMap.get("owl")
        val prog = prefixMap.get("prog")
        val smol = prefixMap.get("smol")

        // Guard clause checking that the subject of the searchTriple starts with prog. Otherwise, return no triples.
        // This assumes that all triples generated by this method uses prog as the prefix for the subject.
        if (searchTriple.getSubject() is Node_URI){
            if (searchTriple.getSubject().getNameSpace() != prog) return TripleListIterator(mutableListOf<Triple>())
        }

        // Guard clause: checking if the predicate of the search triple is one of the given URIs
        if (searchTriple.getPredicate() is Node_URI){
            var possiblePredicates = mutableListOf("${rdf}type", "${smol}hasField", "${rdfs}domain", "${smol}hasMethod", "${rdfs}subClassOf")
            val anyEqual = possiblePredicates.any { it == searchTriple.getPredicate().getURI() }
            if (!anyEqual) return TripleListIterator(mutableListOf<Triple>())
        }

        // Guard clause: set of possible object prefixes it limited
        if (searchTriple.getObject() is Node_URI){
            var possibleObjectPrefixes = mutableListOf(smol, owl, prog)
            val anyEqual = possibleObjectPrefixes.any { it == searchTriple.getObject().getNameSpace() }
            if (!anyEqual) return TripleListIterator(mutableListOf<Triple>())
        }


        var matchingTriples : MutableList<Triple> = mutableListOf<Triple>()

        // Generate triples for classes and fields
        for(classObj in fieldTable){
            val className : String = classObj.key

            addIfMatch(uriTriple("${prog}${className}", "${rdf}type", "${smol}Class"), searchTriple, matchingTriples)
            addIfMatch(uriTriple("${prog}${className}", "${rdf}type", "${owl}Class" ), searchTriple, matchingTriples)

            for(fieldEntry in classObj.value){
                val fieldName : String = classObj.key+"_"+fieldEntry.name

                // Guard clause: Skip this fieldName when the subject of the search triple is different from both "${prog}${className}" and "${prog}$fieldName"
                if (searchTriple.getSubject() is Node_URI){
                    if (searchTriple.getSubject().getURI() != "${prog}${className}" && searchTriple.getSubject().getURI() != "${prog}$fieldName") continue
                }

                addIfMatch(uriTriple("${prog}${className}", "${smol}hasField", "${prog}${fieldName}"), searchTriple, matchingTriples)
                addIfMatch(uriTriple("${prog}${fieldName}", "${rdf}type", "${smol}Field"), searchTriple, matchingTriples)
                addIfMatch(uriTriple("${prog}${fieldName}", "${rdfs}domain", "${prog}${className}"), searchTriple, matchingTriples)

                if(fieldEntry.type == INTTYPE || fieldEntry.type == STRINGTYPE) {
                    addIfMatch(uriTriple("${prog}${fieldName}", "${rdf}type", "${owl}DatatypeProperty"), searchTriple, matchingTriples)
                } else {
                    addIfMatch(uriTriple("${prog}${fieldName}", "${rdf}type", "${owl}FunctionalProperty"), searchTriple, matchingTriples)
                    addIfMatch(uriTriple("${prog}${fieldName}", "${rdf}type", "${owl}ObjectProperty"), searchTriple, matchingTriples)
                }
            }
        }

        // Generate triples for all methods
        for(classObj in methodTable){
            for(method in classObj.value){
                val methodName : String = classObj.key+"_"+method.key
                addIfMatch(uriTriple("${prog}${classObj.key}", "${smol}hasMethod", "${prog}${methodName}"), searchTriple, matchingTriples)
                addIfMatch(uriTriple("${prog}${methodName}", "${rdf}type", "${owl}NamedIndividual"), searchTriple, matchingTriples)
                addIfMatch(uriTriple("${prog}${methodName}", "${rdf}type", "${smol}Method"), searchTriple, matchingTriples)
            }
        }

        // Generate triples for the class hierarchy
        var allClasses : Set<String> = methodTable.keys
        for(classObj in hierarchy.entries){
            for(subClass in classObj.value){
                addIfMatch(uriTriple("${prog}${subClass}", "${rdfs}subClassOf", "${prog}${classObj.key}"), searchTriple, matchingTriples)
                allClasses -= subClass
            }
        }
        // allClasses now only contains classes without any ancestors. They should be subclass of Object
        for(classObj in allClasses) addIfMatch(uriTriple("${prog}${classObj}", "${rdfs}subClassOf", "${prog}Object"), searchTriple, matchingTriples)

        return TripleListIterator(matchingTriples)
    }
}






// Graph representing the heap
class HeapGraph(interpreter: Interpreter) : GraphBase() {
    var interpreter : Interpreter = interpreter

    // Returns an iterator of all triples in the heap that matches searchTriple
    // graphBaseFind only constructs the triples that match searchTriple.
    override protected fun graphBaseFind(searchTriple : Triple): ExtendedIterator<Triple> {
        val settings : Settings = interpreter.settings
        val heap : GlobalMemory = interpreter.heap
        val prefixMap : HashMap<String, String> = interpreter.prefixMap

        // Prefixes
        val rdf = prefixMap.get("rdf")
        val owl = prefixMap.get("owl")
        val prog = prefixMap.get("prog")
        val smol = prefixMap.get("smol")
        val run = prefixMap.get("run")
        val domain = prefixMap.get("domain")

        // Guard clause checking that the subject of the searchTriple starts with "run:" or "domain:". Otherwise, return no triples.
        // This guard should be removed or changed if we change the triples we want to be generated from the heap.
        if (searchTriple.getSubject() is Node_URI){
            if (searchTriple.getSubject().getNameSpace() != run && searchTriple.getSubject().getNameSpace() != domain ) return TripleListIterator(mutableListOf<Triple>())
        }

        var matchingTriples : MutableList<Triple> = mutableListOf<Triple>()

        for(obj in heap.keys){
            val subjectString : String = "${run}${obj.literal}"

            // Guard clause. If this obj does not match to the subject of the search triple, then continue to the next obj
            if (searchTriple.getSubject() is Node_URI){
                if (searchTriple.getSubject().getNameSpace() == run) {
                    if (searchTriple.getSubject().getURI() != subjectString) continue
                }
            }

            addIfMatch(uriTriple(subjectString, "${rdf}type", "${owl}NamedIndividual"), searchTriple, matchingTriples)
            addIfMatch(uriTriple(subjectString, "${rdf}type", "${smol}Object"), searchTriple, matchingTriples)
            addIfMatch(uriTriple(subjectString, "${rdf}type", "${prog}${(obj.tag as BaseType).name}"), searchTriple, matchingTriples)

            // Generating triples for all fields values
            for(store in heap[obj]!!.keys) {

                if (store == "__models") {
                    // Connect object to a model
                    val modelString = heap[obj]!!.getOrDefault(store, LiteralExpr("ERROR")).literal.removeSurrounding("\"")
                    val modelURI = settings.replaceKnownPrefixesNoColon(modelString)
                    addIfMatch(uriTriple(subjectString, "${domain}models", modelURI), searchTriple, matchingTriples)
                }
                else if (store == "__describe") {
                    // Connect model to the description
                    val description : String = heap[obj]!!.getOrDefault(store, LiteralExpr("ERROR")).literal

                    // Guard on the subject of the description.
                    // If the first string in the description (which equals the URI of the model) does not match the searchTriple subject, then continue to the next store
                    val modelURI : String = settings.replaceKnownPrefixesNoColon(description.split(" ")[0])
                    if (searchTriple.getSubject() is Node_URI){
                        if (searchTriple.getSubject().getURI() != modelURI) continue
                    }

                    // Parse and load the description into a jena model.
                    var extendedDescription : String = ""
                    for ((key, value) in prefixMap) extendedDescription += "@prefix $key: <$value> .\n"
                    extendedDescription += description
                    val m : Model = ModelFactory.createDefaultModel().read(IOUtils.toInputStream(extendedDescription, "UTF-8"), null, "TTL");
                    // Consider each triple and add it if it matches the search triple.
                    for (st in m.listStatements()) addIfMatch(st.asTriple(), searchTriple, matchingTriples)
                }
                else {
                    // Generate triples for each of the fields of the object.
                    val predicateString : String = "${prog}${obj.tag}_${store}"

                    // Guard on the predicate. If the current predicate does not match the predicate of the search triple, then continue to the next store
                    if (searchTriple.getPredicate() is Node_URI){
                        if (searchTriple.getPredicate().getURI() != predicateString) continue
                    }

                    val target : LiteralExpr = heap[obj]!!.getOrDefault(store, LiteralExpr("ERROR"))
                    if (target.literal == "null") {
                        val candidateTriple : Triple = Triple(NodeFactory.createURI(subjectString), NodeFactory.createURI(predicateString), NodeFactory.createLiteral("${smol}${target.literal}") )
                        addIfMatch(candidateTriple, searchTriple, matchingTriples)
                    }
                    else if (target.tag == ERRORTYPE || target.tag == STRINGTYPE) {
                        val candidateTriple : Triple = Triple(NodeFactory.createURI(subjectString), NodeFactory.createURI(predicateString), NodeFactory.createLiteral(target.literal.removeSurrounding("\""), XSDDatatype.XSDstring) )
                        addIfMatch(candidateTriple, searchTriple, matchingTriples)
                    }
                    else if (target.tag == INTTYPE) {
                        val candidateTriple : Triple = Triple(NodeFactory.createURI(subjectString), NodeFactory.createURI(predicateString), NodeFactory.createLiteral("${target.literal}", XSDDatatype.XSDinteger) )
                        addIfMatch(candidateTriple, searchTriple, matchingTriples)
                    }
                    else {
                        val candidateTriple : Triple = uriTriple(subjectString, predicateString, "${run}${target.literal}")
                        addIfMatch(candidateTriple, searchTriple, matchingTriples)
                    }
                }
            }
        }

        return TripleListIterator(matchingTriples)
    }


}










class Interpreter(
    val stack: Stack<StackEntry>,               // This is the process stack
    var heap: GlobalMemory,             // This is a map from objects to their heap memory
    var simMemory: SimulationMemory,    // This is a map from simulation objects to their handler
    val staticInfo: StaticTable,                // Class table etc.
    val settings : Settings,                    // Settings from the user
    private val rules : String,                 // Additional rules for jena
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
            settings,
            rules
        )
    }

    private var debug = false

    // Defining prefixes
    val prefixMap = hashMapOf<String, String>(
        "domain" to "${settings.domainPrefix}",
        "smol" to "${settings.langPrefix}",
        "prog" to "${settings.progPrefix}",
        "run" to "${settings.runPrefix}",
        "owl" to "http://www.w3.org/2002/07/owl#",
        "rdf" to "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
        "rdfs" to "http://www.w3.org/2000/01/rdf-schema#",
        "xsd" to "http://www.w3.org/2001/XMLSchema#"
    )


    // Returns the virtual model. I.e., the union of all existing smaller models
    // Including: vocab.owl, background data, heap, static table
    // TODO: add simulation data
    fun getCompleteModel() : Model {
        // var model : Model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM) // use this to turn off inference
        var model : Model = ModelFactory.createOntologyModel()
        var allTriplesString : String = ""
        for ((key, value) in prefixMap) allTriplesString += "@prefix $key: <$value> .\n"
        val vocabURL : java.net.URL = this::class.java.classLoader.getResource("vocab.owl")
        allTriplesString += vocabURL.readText(Charsets.UTF_8) + "\n"
        if(settings.background != "") allTriplesString += settings.background
        val s : InputStream = ByteArrayInputStream(allTriplesString.toByteArray())
        model.read(s, null, "TTL")

        // Construct graph based on the heap, and make a model based on this graph.
        var heapGraphModel : Model = ModelFactory.createModelForGraph(HeapGraph(this))
        model = ModelFactory.createUnion(model, heapGraphModel)
        // Construct graph based on the static table, and make a model based on this graph.
        var staticTableGraphModel : Model = ModelFactory.createModelForGraph(StaticTableGraph(this))
        model = ModelFactory.createUnion(model, staticTableGraphModel)

        // Adding prefixes
        for ((key, value) in prefixMap) model.setNsPrefix(key, value)

        // Turn on reasoning if background knowledge is given.
        if(settings.background != "") {
            if(settings.verbose) println("Using background knowledge...")
            model = ModelFactory.createInfModel(ReasonerRegistry.getOWLReasoner(), model)
        }

        // Add rules to the model if available.
        if(rules != "" || settings.backgroundrules != "") {
            if(settings.verbose) println("Loading generated builtin rules $rules and domain rules ${settings.backgroundrules}")
            val prefixes  = settings.prefixes()
            val reader = (prefixes+"\n"+rules+"\n"+settings.backgroundrules).byteInputStream().bufferedReader()
            val rParsed = Rule.rulesParserFromReader(BufferedReader(reader))
            val reasoner: org.apache.jena.reasoner.Reasoner = GenericRuleReasoner(Rule.parseRules(rParsed))
            val infModel = ModelFactory.createInfModel(reasoner, model)
            //infModel.prepare()
            model = infModel
        }

        // write model to file if the materialize flag is given
        if (settings.materialize) {
            model.write(FileWriter("${settings.outpath}/output.ttl"),"TTL")
        }

        return model
    }


    // Run SPARQL query (str)
    fun query(str: String): ResultSet? {
        // Adding prefixes to the query
        var queryWithPrefixes = ""
        for ((key, value) in prefixMap) queryWithPrefixes += "PREFIX $key: <$value>\n"
        queryWithPrefixes += str

        var model = getCompleteModel()
        if(settings.verbose) println("execute CSSA\n: $queryWithPrefixes")
        val query = QueryFactory.create(queryWithPrefixes)
        val qexec = QueryExecutionFactory.create(query, model)

        return qexec.execSelect()
    }


    private fun owlQuery(str: String): NodeSet<OWLNamedIndividual> {
        val out = settings.replaceKnownPrefixes(str)
       // str.replace("prog:","urn:").replace("run:","urn:").replace("smol:","https://github.com/Edkamb/SemanticObjects#:")
        val m = OWLManager.createOWLOntologyManager()
        val ontology = m.loadOntologyFromOntologyDocument(File("${settings.outpath}/output.ttl"))
        val reasoner = Reasoner.ReasonerFactory().createReasoner(ontology)
        val parser = ManchesterOWLSyntaxParserImpl(OntologyConfigurator(), m.owlDataFactory)
        parser.setDefaultOntology(ontology)
        return reasoner.getInstances(parser.parseClassExpression(out))
    }

    // Dump all triples in the virtual model to output.ttl
    internal fun dump() {
        var model = getCompleteModel()
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

        return true
    }

    internal fun prepareSPARQL(queryExpr : Expression, params : List<Expression>, stackMemory: Memory, heap: GlobalMemory, obj: LiteralExpr) : String{
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
        dump()
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
                        if (!(staticInfo.fieldTable[(obj.tag as BaseType).name]
                                ?: error("")).contains(stmt.target.name)
                        ) throw Exception("This field is unknown: ${stmt.target.name}")
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

                        var found = obres.toString().removePrefix(settings.runPrefix)
                        if(found.startsWith("\\\"")) found = found.replace("\\\"","\"")
                        for (ob in heap.keys) {
                            if (ob.literal == found) {
                                newMemory["content"] = LiteralExpr(found, ob.tag)
                                break
                            }
                        }
                        if (!newMemory.containsKey("content")) {
                            if(found.startsWith("\"")) newMemory["content"] = LiteralExpr(found, STRINGTYPE)
                            else if(found.matches("\\d+".toRegex())) newMemory["content"] = LiteralExpr(found, INTTYPE)
                            else if(found.matches("\\d+.\\d+".toRegex())) newMemory["content"] = LiteralExpr(found, DOUBLETYPE)
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
                dump()
                if(!newFile.exists()) newFile.createNewFile()
                newFile.writeText(settings.prefixes() + "\n"+State.HEADER + "\n@prefix sh: <http://www.w3.org/ns/shacl#>.\n")
                newFile.appendText(file.readText())
                val shapesGraph = RDFDataMgr.loadGraph("${settings.outpath}/shape.ttl")
                val dataGraph = RDFDataMgr.loadGraph("${settings.outpath}/output.ttl")

                val shapes: Shapes = Shapes.parse(shapesGraph)

                val report = ShaclValidator.get().validate(shapes, dataGraph)
                val resLit = if(report.conforms()) TRUEEXPR else FALSEEXPR
                return Pair(StackEntry(AssignStmt(stmt.target, resLit, declares = stmt.declares), stackMemory, obj, id), listOf())
            }
            is OwlStmt -> {
                if (!staticInfo.fieldTable.containsKey("List") || !staticInfo.fieldTable["List"]!!.contains("content") || !staticInfo.fieldTable["List"]!!.contains(
                        "next"
                    )
                ) {
                    throw Exception("Could not find List class in this model")
                }
                if (stmt.query !is LiteralExpr || stmt.query.tag != STRINGTYPE) {
                    throw Exception("Please provide a string as the input to a derive statement")
                }

                //this is duplicated w.r.t. REPL until we figure out how to internally represent the KB
                dump()
                val res : NodeSet<OWLNamedIndividual> = owlQuery(stmt.query.literal)
                var list = LiteralExpr("null")
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
                return Pair(StackEntry(AssignStmt(stmt.target, list, declares = stmt.declares), stackMemory, obj, id), listOf())
            }
            is ReturnStmt -> {
                val over = stack.pop()
                if (over.active is StoreReturnStmt) {
                    val res = eval(stmt.value, stackMemory, heap, simMemory, obj)
                    return Pair(StackEntry(AssignStmt(over.active.target, res, declares = null), over.store, over.obj, id), listOf())
                }
                if (over.active is SequenceStmt && over.active.first is StoreReturnStmt) {
                    val active = over.active.first
                    val next = over.active.second
                    val res = eval(stmt.value, stackMemory, heap, simMemory, obj)
                    return Pair(
                        StackEntry(appendStmt(AssignStmt(active.target, res, declares = null), next), over.store, over.obj, id),
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
