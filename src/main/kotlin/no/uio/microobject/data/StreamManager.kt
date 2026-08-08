package no.uio.microobject.data

import no.uio.microobject.runtime.*
import no.uio.microobject.ast.expr.*
import no.uio.microobject.ast.Expression
import no.uio.microobject.ast.stmt.MonitorObject
import no.uio.microobject.main.Settings
import no.uio.microobject.type.*

import java.nio.file.*
import java.util.Observer;
import java.util.Observable
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.io.File
import java.io.StringWriter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.apache.log4j.BasicConfigurator
import org.apache.log4j.Level
import org.apache.log4j.Logger as Log4jLogger
import org.apache.log4j.PropertyConfigurator
import org.apache.jena.rdf.model.Model
import org.apache.jena.sparql.algebra.Table
import org.apache.jena.graph.Graph
import kotlin.text.toLong

import org.streamreasoning.rsp4j.csparql2.engine.CSPARQLEngine
import org.streamreasoning.rsp4j.csparql2.engine.JenaContinuousQueryExecution
import org.streamreasoning.rsp4j.csparql2.stream.GraphStreamSchema
import org.streamreasoning.rsp4j.csparql2.sysout.ResponseFormatterFactory
import org.streamreasoning.rsp4j.csparql2.syntax.QueryFactory
import org.streamreasoning.rsp4j.csparql2.sysout.GenericResponseSysOutFormatter
import org.streamreasoning.rsp4j.api.engine.config.EngineConfiguration;
import org.streamreasoning.rsp4j.io.DataStreamImpl
import org.streamreasoning.rsp4j.api.stream.data.DataStream
import org.streamreasoning.rsp4j.api.sds.SDSConfiguration
import org.apache.jena.rdf.model.*;
import org.apache.jena.atlas.lib.tuple.Tuple

// Per-monitor state
data class WindowBuf(var tick: Long? = null, val current: MutableList<Table> = mutableListOf())

// Class managing streams
class StreamManager(private val settings: Settings, val staticTable: StaticTable, private val interpreter: Interpreter?) {

    private var engine: CSPARQLEngine? = null
    private var engineInitialized = false
    private var sdsConfig: SDSConfiguration? = null
    private var ec: EngineConfiguration? = null
    val LOG : Logger? = LoggerFactory.getLogger(StreamManager::class.java)

    private var streamToClass: MutableMap<LiteralExpr, String> = mutableMapOf()
    private var streams: MutableMap<String, MutableMap<LiteralExpr, StreamObject>> = mutableMapOf()
    private var monitors: MutableMap<LiteralExpr, MonitorObject> = mutableMapOf()

    private val windowBufs = ConcurrentHashMap<LiteralExpr, WindowBuf>()
    private val queryResults = ConcurrentHashMap<LiteralExpr, List<Table>>() // read-only snapshots

    var clockVar : String? = null
    var clockTimestampSec : Long? = null

    // stream state
    var lastTriggerTs: MutableMap<LiteralExpr, Long> = mutableMapOf()
    var lastModels: MutableMap<LiteralExpr, Model> = mutableMapOf()

    var nStaticGraphsPushed = 0

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
    
    private fun initEngineIfNeeded() {
        if (!engineInitialized) {
            val ts = getTimestamp()

            val configPath = "src/main/resources/csparql2.properties"
            val defaultPath = "src/main/resources/default-csparql2.properties"

            if (File(configPath).exists()) {
                ec = EngineConfiguration(configPath)
                sdsConfig = SDSConfiguration(configPath)
                println("Loaded csparql2.properties successfully at ts=$ts.")
            } else {
                println("Failed to load csparql2.properties, falling back to default-csparql2.properties at ts=$ts.")
                ec = EngineConfiguration(defaultPath)
                sdsConfig = SDSConfiguration(defaultPath)
            }

            engine = CSPARQLEngine(ts, ec)
            engineInitialized = true
        }
    }

    public fun getQueryResults(name: LiteralExpr): List<Table>? {
        return queryResults[name]
    }

    public fun registerStream(className: String, obj: LiteralExpr) {
        initEngineIfNeeded()
        val streamIri = "${settings.runPrefix}${obj.toString()}"

        val stream = StreamObject(streamIri)
        val reg = engine!!.register(stream)
        stream.setWritable(reg)

        if (!streams.containsKey(className)) streams[className] = mutableMapOf()
        streams[className]!![obj] = stream
        streamToClass[obj] = className
    }
    
    private fun getTimestamp(): Long {
        if (clockTimestampSec != null)
            return secToMs(clockTimestampSec!!)
        return System.currentTimeMillis()
    }

    private fun secToMs(s: Long): Long {
        return s * 1000
    }

    fun updateClock(newTsSec: Long) {
        if (clockTimestampSec != newTsSec) {
            clockTimestampSec = newTsSec

            // push last models to streams if time has advanced
            pushLastModels()
        }
    }

    private fun pushLastModels() {
        for ((obj, model) in lastModels) {
            if (model.isEmpty) continue
            streams[streamToClass[obj]!!]!![obj]!!.putGraph(model.getGraph(), lastTriggerTs[obj]!!)
            lastModels[obj] = ModelFactory.createDefaultModel()
        }
    }

    public fun triggerStream(className: String, obj: LiteralExpr, methodName: String, stackEntry: StackEntry) {
        val stream = streams[className]!![obj]!!
        val expressions = interpreter!!.staticInfo.streamersTable[className]!![methodName]!!

        val timestamp = getTimestamp()
        if (!lastTriggerTs.containsKey(obj) || timestamp > lastTriggerTs[obj]!!) {
            // put model only if time has advanced (guarantee one graph per timestamp)
            if (lastTriggerTs.containsKey(obj))
                stream.putGraph(lastModels[obj]!!.getGraph(), lastTriggerTs[obj]!!)
            lastModels[obj] = ModelFactory.createDefaultModel()
            lastTriggerTs[obj] = timestamp
        } 

        for (expr in expressions) {
            val res = interpreter.eval(expr, stackEntry)

            val exprParts = expr.toString().split('.')
            var subjIri: String = "${settings.runPrefix}${obj.literal}"
            var predIri = "${settings.progPrefix}${className}_${expr.toString().removePrefix("this.").replace('.', '_')}"
            if (exprParts.size < 2) {
                throw Exception("Streamer expression must be of the form this.field or this.obj.field")
            }
            if (exprParts.size > 2) {
                // find the one before last part
                var currentObj: LiteralExpr = obj
                for (i in 1 until exprParts.size - 1) {
                    val fieldName = exprParts[i]
                    currentObj = interpreter.heap[currentObj]!![fieldName]!!
                }
                subjIri = "${settings.runPrefix}${currentObj.literal}"
                predIri = "${settings.progPrefix}${(currentObj.tag as BaseType).name}_${exprParts.last()}"
            }

            val stmt = lastModels[obj]!!.createStatement(lastModels[obj]!!.createResource(subjIri), lastModels[obj]!!.createProperty(predIri), literalToIri(lastModels[obj]!!, res, settings))
            lastModels[obj]!!.add(stmt)
            // println("Added to stream $obj at $timestamp: $stmt")
        }

        // if the clock variable is not used, push the model immediately
        // assume system time (ms) has advanced or will advance before next trigger
        if (clockVar == null) {
            stream.putGraph(lastModels[obj]!!.getGraph(), lastTriggerTs[obj]!!)
            // println("Pushed immediately to stream $obj at ${lastTriggerTs[obj]}: ${lastModels[obj]!!.size()} triples (now: ${getTimestamp()})")
            lastModels[obj] = ModelFactory.createDefaultModel()
        }
    }

    private fun literalToIri(m: Model, lit: LiteralExpr, settings: Settings): RDFNode =
    when (lit.tag) {
        INTTYPE    -> m.createTypedLiteral(lit.literal.toInt())
        DOUBLETYPE -> m.createTypedLiteral(lit.literal.toDouble())
        BOOLEANTYPE-> m.createTypedLiteral(lit.literal.toBoolean())
        STRINGTYPE -> m.createLiteral(lit.literal)
        else       -> m.createResource("${settings.runPrefix}${lit.literal}")
    }

    public fun getMonitor(name: LiteralExpr): MonitorObject? {
        return monitors[name]
    }

    public fun registerQuery(name: LiteralExpr, queryExpr : Expression, params: List<Expression>, stackMemory: Memory, heap: GlobalMemory, obj: LiteralExpr, SPARQL : Boolean = true, declaredType: Type): JenaContinuousQueryExecution {
        initEngineIfNeeded()
        val queryStr = prepareQuery(name, queryExpr, params, stackMemory, heap, obj, SPARQL)
        if (settings.verbose) println("Registering query:\n$queryStr")

        val cqe = engine!!.register(queryStr, sdsConfig) as JenaContinuousQueryExecution
        monitors[name] = MonitorObject(name, declaredType)

        windowBufs.computeIfAbsent(name) { WindowBuf() }

        val outputStream = cqe.outstream()
        outputStream?.addConsumer { arg, ts ->
            val table = arg as Table
            val b = windowBufs.getOrPut(name) { WindowBuf() }

            if (b.tick != ts) {
                b.tick = ts
                b.current.clear()
            }
            b.current += table

            queryResults[name] = b.current.toList()
        }

        return cqe
    }

    private fun prepareQuery(name: LiteralExpr, queryExpr : Expression, params : List<Expression>, 
            stackMemory: Memory, heap: GlobalMemory, obj: LiteralExpr, SPARQL : Boolean = true) : String{

        var prefixes = ""
        for ((key, value) in settings.prefixMap()) prefixes += "PREFIX $key: <$value>\n"

        val queryHeader = "REGISTER RSTREAM <${settings.runPrefix}${name.literal}/out> AS "

        val queryBody = interpreter!!.prepareQuery(queryExpr, params, stackMemory, heap, obj, SPARQL)
            .removePrefix("\"").removeSuffix("\"")

        var queryWithPrefixes = prefixes + queryHeader + queryBody
        queryWithPrefixes = queryWithPrefixes.replace("\\\"", "\"")

        return queryWithPrefixes
    }

    public fun getStaticNamedIri(): String {
        val s = "${settings.runPrefix}loadStatic${nStaticGraphsPushed}"
        nStaticGraphsPushed += 1
        return s
    }

}

class StreamObject(var iri: String) : DataStreamImpl<Graph>(iri) {

    private var s: DataStream<Graph>? = null

    fun setWritable(s: DataStream<Graph>) {
        this.s = s
    }

    fun putGraph(m: Graph, t: Long) {
        if (s == null) throw Exception("Stream $iri is not writable")
        s!!.put(m, t)
    }
}
 