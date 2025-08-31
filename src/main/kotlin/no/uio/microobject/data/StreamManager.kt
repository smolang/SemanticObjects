package no.uio.microobject.data

import no.uio.microobject.runtime.*
import no.uio.microobject.ast.expr.*
import no.uio.microobject.ast.Expression
import no.uio.microobject.ast.stmt.MonitorObject
import no.uio.microobject.main.Settings
import no.uio.microobject.type.*


import eu.larkc.csparql.core.engine.*
import eu.larkc.csparql.cep.api.*
import eu.larkc.csparql.core.*
import eu.larkc.csparql.common.*
import java.util.Observer;
import java.util.Observable
import java.util.concurrent.ConcurrentHashMap
import java.io.File
import java.io.StringWriter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.apache.log4j.BasicConfigurator
import org.apache.log4j.Level
import org.apache.log4j.Logger as Log4jLogger
import org.apache.log4j.PropertyConfigurator
import org.apache.jena.rdf.model.Model
import kotlin.text.toLong

class ResultPusher(private val name: LiteralExpr, private val resultTable: MutableMap<LiteralExpr, RDFTable?>) : Observer {
    public override fun update(o: Observable, arg: Any) {
        val results: RDFTable = arg as RDFTable
        resultTable[name] = results
    }
}

// Class managing streams
class StreamManager(private val settings: Settings, val staticTable: StaticTable, private val interpreter: Interpreter?) {

    private val engine: CsparqlEngineImpl = CsparqlEngineImpl()
    private var engineInitialized = false
    val LOG : Logger? = LoggerFactory.getLogger(StreamManager::class.java)

    private var streams: MutableMap<String, MutableMap<LiteralExpr, RdfStream>> = mutableMapOf()
    private var monitors: MutableMap<LiteralExpr, MonitorObject> = mutableMapOf()
    
    private var queryResults: MutableMap<LiteralExpr, RDFTable?> = ConcurrentHashMap()

    var clockVar : String? = null
    var clockTimestampSec : String? = null

    init {
        
        // configure c-sparql logging
        val propFile = File("src/main/resources/log4j_configuration/csparql_log4j.properties")
        if (propFile.exists()) {
            PropertyConfigurator.configure(propFile.absolutePath)
        } else {
            // only log errors if the logger config file is not present
            BasicConfigurator.configure()
            Log4jLogger.getRootLogger().level = Level.ERROR
            System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "ERROR")
        }
    }

    private fun initEngine() {

        engine.initialize(true) // timestamp enabled
        engineInitialized = true
    }

    public fun getQueryResults(name: LiteralExpr): RDFTable? {
        return queryResults[name]
    }

    public fun registerStream(className: String, obj: LiteralExpr) {
        if (!engineInitialized) initEngine()
        val streamIri = "${settings.runPrefix}${obj.toString()}"
        val stream = RdfStream(streamIri)
        engine.registerStream(stream)
        if (!streams.containsKey(className)) streams[className] = mutableMapOf()
        streams[className]!![obj] = stream
    }
    
    private fun getTimestamp(): Long {
        if (clockTimestampSec != null) return secToMs(clockTimestampSec!!.toLong())
        return System.currentTimeMillis()
    }

    private fun secToMs(s: Long): Long {
        return s * 1000
    }

    public fun triggerStream(className: String, obj: LiteralExpr, methodName: String, stackEntry: StackEntry) {
        val stream = streams[className]!![obj]!!
        val expressions = interpreter!!.staticInfo.streamersTable[className]!![methodName]!!

        val subjIri = "${settings.runPrefix}${obj.literal}"
        val timestamp = getTimestamp()

        for (expr in expressions) {
            val res = interpreter.eval(expr, stackEntry)

            val predIri = "${settings.progPrefix}${className}_${expr.toString().removePrefix("this.").replace('.', '_')}"
            val objIri = literalToIri(res)

            val quad = RdfQuadruple(subjIri, predIri, objIri, timestamp)
            // println(quad.toString())
            stream.put(quad)
        }
    }

    private fun literalToIri(lit: LiteralExpr): String {
        if (lit.tag == INTTYPE) return "\"${lit.literal}\"^^http://www.w3.org/2001/XMLSchema#integer"
        if (lit.tag == BOOLEANTYPE) return "\"${lit.literal}\"^^http://www.w3.org/2001/XMLSchema#boolean"
        if (lit.tag == DOUBLETYPE) return "\"${lit.literal}\"^^http://www.w3.org/2001/XMLSchema#double"
        if (lit.tag == STRINGTYPE) return "\"${lit.literal}\"^^http://www.w3.org/2001/XMLSchema#string"
        return "${settings.runPrefix}${lit.literal}"
    }

    public fun addMonitor(name: LiteralExpr, monitor: MonitorObject) {
        monitors[name] = monitor
    }

    public fun getMonitor(name: LiteralExpr): MonitorObject? {
        return monitors[name]
    }

    public fun registerQuery(name: LiteralExpr, queryExpr : Expression, params: List<Expression>, stackMemory: Memory, heap: GlobalMemory, obj: LiteralExpr, SPARQL : Boolean = true): CsparqlQueryResultProxy {
        if (!engineInitialized) initEngine()
        val queryStr = prepareQuery(name, queryExpr, params, stackMemory, heap, obj, SPARQL)
        var resultProxy = engine.registerQuery(queryStr, true) // reasoning enabled
        resultProxy.addObserver(ResultPusher(name, queryResults)) // each key is only used by one observer
        return resultProxy
    }

    private fun prepareQuery(name: LiteralExpr, queryExpr : Expression, params : List<Expression>, stackMemory: Memory, heap: GlobalMemory, obj: LiteralExpr, SPARQL : Boolean = true) : String{
        val queryHeader = "REGISTER QUERY ${name.literal} AS "

        val queryBody = interpreter!!.prepareQuery(queryExpr, params, stackMemory, heap, obj, SPARQL)
            .removePrefix("\"").removeSuffix("\"")

        var queryWithPrefixes = queryHeader
        // for ((key, value) in settings.prefixMap()) queryWithPrefixes += "PREFIX $key: <$value>\n"
        queryWithPrefixes += queryBody
        queryWithPrefixes = queryWithPrefixes.replace("\\\"", "\"")

        // Replace occurrences of value:x with <keyx> for each prefix in prefixMap
        for ((key, value) in settings.prefixMap()) {
            // Regex to match key: followed by a valid identifier (e.g., obj4)
            val regex = Regex("""${Regex.escape(key)}:([A-Za-z0-9_]+)""")
            queryWithPrefixes = queryWithPrefixes.replace(regex) { matchResult ->
            "<$value${matchResult.groupValues[1]}>"
            }
        }

        return queryWithPrefixes
    }

    public fun putStaticNamedGraph(iri: String, model: Model) {
        if (!engineInitialized) initEngine()

        // serialize the model (RDF/XML matches the engine's first attempt)
        val sw = StringWriter()
        model.write(sw, "RDF/XML")

        // hand it to the C-SPARQL engine
        engine.putStaticNamedModel(iri, sw.toString())
    }

}
 