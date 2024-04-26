@file:Suppress(
    "LiftReturnOrAssignment"
)

package no.uio.microobject.runtime

import com.influxdb.client.kotlin.InfluxDBClientKotlin
import com.influxdb.client.kotlin.InfluxDBClientKotlinFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.uio.microobject.ast.*
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.ast.stmt.ReturnStmt
import no.uio.microobject.data.TripleManager
import no.uio.microobject.main.Settings
import no.uio.microobject.type.*
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.ResultSet
import org.semanticweb.HermiT.Reasoner
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserImpl
import org.semanticweb.owlapi.model.OWLNamedIndividual
import org.semanticweb.owlapi.model.OntologyConfigurator
import org.semanticweb.owlapi.reasoner.NodeSet
import java.io.File
import java.io.FileWriter
import java.util.*
import kotlin.streams.toList

data class InfluxDBConnection(val url : String, val org : String, val token : String, val bucket : String){
    private var influxDBClient : InfluxDBClientKotlin? = null
    private fun connect(){
        influxDBClient = InfluxDBClientKotlinFactory.create(url, token.toCharArray(), org)
    }
    fun queryOneSeries(flux : String, settings: Settings) : List<Double>{
        connect()
        if(settings.verbose) println("RAN QUERY: $flux")
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

data class EvalResult(val current: StackEntry?, val spawns: List<StackEntry>, val debug : Boolean = false)

@Suppress("unused")
class Interpreter(
    val stack: Stack<StackEntry>,               // This is the process stack
    var heap: GlobalMemory,             // This is a map from objects to their heap memory
    var simMemory: SimulationMemory,    // This is a map from simulation objects to their handler
    val staticInfo: StaticTable,                // Class table etc.
    val settings : Settings                    // Settings from the user
) {

    // TripleManager used to provide virtual triples etc.
    val tripleManager : TripleManager = TripleManager(settings, staticInfo, this)

    //evaluates a call on cl.nm on thisVar
    //Must ONLY be called if nm is checked to have no side-effects (i.e., is rule)
    //First return value is the created object, the second the return value
    fun evalCall(objName: String, className: String, metName: String, params: Map<String, LiteralExpr> = mapOf()): Pair<LiteralExpr, LiteralExpr> {
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

        // Add parameters to memory
        if (params.isNotEmpty()) {
            for ((paramName, paramExpr) in params) {
                mem[paramName] = paramExpr
            }
        }

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

    /**
     * Retrieve the list of ObjectNames of a given class from the heap
     * @param className the name of the class
     * @return the list of object names
     */
    fun getObjectNames(className: String): List<String> {
        return heap.keys.filter { it.tag.toString() == className }.map { it.literal }
    }

    // Run SPARQL query (str)
    fun query(str: String): ResultSet? {
        // Adding prefixes to the query
        var queryWithPrefixes = ""
        for ((key, value) in settings.prefixMap()) queryWithPrefixes += "PREFIX $key: <$value>\n"
        queryWithPrefixes += str

        val model = tripleManager.getModel()
        queryWithPrefixes = queryWithPrefixes.replace("\\\"", "\"")
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
        val ontology = tripleManager.getOntology()
        val reasoner = Reasoner.ReasonerFactory().createReasoner(ontology)
        val parser = ManchesterOWLSyntaxParserImpl(OntologyConfigurator(), m.owlDataFactory)
        parser.setDefaultOntology(ontology)
        val expr = parser.parseClassExpression(out)
        return reasoner.getInstances(expr)
    }

    // Dump all triples in the virtual model to ${settings.outdir}/file
    internal fun dump(file: String) {
        val model = tripleManager.getModel()
        File(settings.outdir).mkdirs()
        File("${settings.outdir}/${file}").createNewFile()
        model.write(FileWriter("${settings.outdir}/${file}"),"TTL")
    }

    fun evalTopMost(expr: Expression) : LiteralExpr{
        if(stack.isEmpty()) return LiteralExpr("ERROR") // program terminated
        return eval(expr, stack.peek())
    }

    /**
     * Executes exactly one step of the interpreter, and returns true if
     * another step can be executed.  Note that rewritings also count as one
     * executing step.
     */
    fun makeStep() : Boolean {
        if(stack.isEmpty()) return false // program terminated

        //get current frame
        val current = stack.pop()

        if(heap[current.obj] == null)
            throw Exception("This object is unknown: ${current.obj}")

        //get own local memory
        val heapObj: Memory = heap.getOrDefault(current.obj, mutableMapOf())

        //evaluate
        val eRes = current.active.eval(heapObj, current, this)


        //if the current frame is not finished, push its modification back
        if(eRes.current != null){
            stack.push(eRes.current)
        }

        //in case we spawn more frames, push them as well
        for( se in eRes.spawns){
            stack.push(se)
        }

        return !eRes.debug
    }

    /**
     * This implements the substitution of meta-variables %i
     */
    fun prepareQuery(queryExpr : Expression, params : List<Expression>, stackMemory: Memory, heap: GlobalMemory, obj: LiteralExpr, SPARQL : Boolean = true) : String{
        val query = eval(queryExpr, stackMemory, heap, simMemory, obj)
        if (query.tag != STRINGTYPE)
            throw Exception("Query is not a string: $query")
        var str = query.literal
        var i = 1
        for (expr in params) {
            val p = eval(expr, stackMemory, heap, simMemory, obj)
            str = when (p.tag) {
                INTTYPE     -> str.replace("%${i++}", if(SPARQL) "\"${p.literal}\"^^xsd:integer" else p.literal);
                DOUBLETYPE  -> str.replace("%${i++}", if(SPARQL)"\"${p.literal}\"^^xsd:double" else p.literal);
                STRINGTYPE  -> str.replace("%${i++}", p.literal);
                else        -> str.replace("%${i++}", "run:${p.literal}")
            }
        }
        if (!staticInfo.fieldTable.containsKey("List") || !staticInfo.fieldTable["List"]!!.any { it.name == "content" } || !staticInfo.fieldTable["List"]!!.any { it.name == "next" }
        ) {
            throw Exception("Could not find List class in this model")
        }
        return str
    }


    fun eval(expr: Expression, stackEntry: StackEntry) = eval(expr, stackEntry.store, this.heap, this.simMemory, stackEntry.obj)
    fun eval(expr: Expression, stack: Memory, heap: GlobalMemory, simMemory: SimulationMemory, obj: LiteralExpr) : LiteralExpr
            = expr.eval(stack, heap, simMemory, obj)

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
