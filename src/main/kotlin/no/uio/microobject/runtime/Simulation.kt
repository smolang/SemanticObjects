package no.uio.microobject.runtime

import core.FmuConfig
import core.InputPortConfig
import core.OutputPortConfig
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.ast.expr.TRUEEXPR
import no.uio.microobject.type.BOOLEANTYPE
import no.uio.microobject.type.DOUBLETYPE
import no.uio.microobject.type.INTTYPE
import no.uio.microobject.type.STRINGTYPE
import org.javafmi.modeldescription.SimpleType
import org.javafmi.modeldescription.v2.ModelDescription
import org.javafmi.wrapper.Simulation
import org.javafmi.wrapper.variables.SingleRead
import scala.collection.JavaConverters
import scala.collection.immutable.HashMap

data class Snapshot( val time : Double, val values : List<Pair<String, Double>>, val role : String?)

class SimulatorObject(val path : String, memory : Memory){
    companion object  {
        const val ROLEFIELDNAME = "role"
        const val PSEUDOOFFSETFIELDNAME = "pseudoOffset"
        const val TIMEFIELDNAME = "time"
    }
    private val series = mutableListOf<Snapshot>()
    private var sim : Simulation = Simulation(path)
    private var time : Double = 0.0

    //representation for monitor
    private lateinit var scen : FmuConfig;

    //additional fields
    private var role : String = ""
    private var pseudoOffset : Double = 0.0

    fun read(name: String): LiteralExpr {
        if(name == ROLEFIELDNAME) return LiteralExpr(role, STRINGTYPE)
        if(name == PSEUDOOFFSETFIELDNAME) return LiteralExpr(pseudoOffset.toString(), DOUBLETYPE)
        if(name == TIMEFIELDNAME) return LiteralExpr(time.toString(), DOUBLETYPE)
        val v = sim.modelDescription.getModelVariable(name)
        if(v.typeName == "Integer") return LiteralExpr(sim.read(name).asInteger().toString(), INTTYPE)
        if(v.typeName == "Boolean") return LiteralExpr(sim.read(name).asBoolean().toString(), BOOLEANTYPE)
        if(v.typeName == "Real") return LiteralExpr(sim.read(name).asDouble().toString(), DOUBLETYPE)
        return LiteralExpr(sim.read(name).asString(), STRINGTYPE)
    }
    fun tick(i : Double){
        sim.doStep(i)
        time += i
        addSnapshot()
    }

    private fun addSnapshot( ){
        var list = emptyList<Pair<String, Double>>()
        for(mVar in sim.modelDescription.modelVariables){
            if(mVar.causality == "output" && mVar.typeName == "Real"){
                val res = sim.read(mVar.name).asDouble()
                list = list + Pair(mVar.name, res)
            }
        }
        val roleName = if(role == "") null else role
        series.add(Snapshot(time, list, roleName))
    }

    fun write(name: String, res: LiteralExpr) {
        if(name == ROLEFIELDNAME) {
            role = res.literal
            return
        }
        if(name == PSEUDOOFFSETFIELDNAME) {
            pseudoOffset = res.literal.toDouble()
            return
        }
        if(name == TIMEFIELDNAME){
            throw Exception("You cannot write the time of an FMU.")
        }
        for(mVar in sim.modelDescription.modelVariables){
            if(mVar.name == name){
                if(mVar.causality == "input" && mVar.typeName == "Integer"){
                    sim.write(name).with(res.literal.toInt())
                    break
                } else if(mVar.causality == "input" && mVar.typeName == "Boolean"){
                    sim.write(name).with(res == TRUEEXPR)
                    break
                } else if(mVar.causality == "input" && mVar.typeName == "Real"){
                    sim.write(name).with(res.literal.toDouble())
                    break
                } else if(mVar.causality == "input" && mVar.typeName == "String"){
                    sim.write(name).with(res.literal )
                    break
                }
            }
        }
    }


    fun terminate() {
        sim.terminate()
    }

    /* kept for easier lookup */
    /*
    fun dump(obj: String): String {
        var res = "$obj smol:modelName '${sim.modelDescription.modelName}'.\n"
        for(mVar in sim.modelDescription.modelVariables) {
            if(mVar.causality == "input") {
                res += "${obj}_${mVar.name} a smol:InPort.\n"
                res += "$obj smol:hasInPort ${obj}_${mVar.name}.\n"
            }
            if(mVar.causality == "output"){
                res += "${obj}_${mVar.name} a smol:OutPort.\n"
                res += "$obj smol:hasOutPort ${obj}_${mVar.name}.\n"
                res += "$obj ${obj}_${mVar.name} ${dumpSingle(sim.read(mVar.name),mVar.type)}.\n"
                res += "${obj}_${mVar.name} smol:withName '${mVar.name}'.\n"
            }
            if(mVar.causality == "parameter"){
                res += "$obj smol:hasStatePort prog:${mVar.name}.\n"
                res += "$obj ${obj}_${mVar.name} ${dumpSingle(sim.read(mVar.name),mVar.type)}.\n"
                mVar.type
            }
        }
        for((mCounter, snap) in series.withIndex()){
            val name = "measure_${obj.split(":")[1]}_$mCounter"
            res += "run:$name a smol:Measurement.\n"
            if(snap.role != null) res += "run:$name smol:roleName '${snap.role.removeSurrounding("\"")}'.\n"
            res += "run:$name smol:atTime ${snap.time + pseudoOffset}.\n"
            for( data in snap.values) {
                res += "run:$name smol:ofPort ${obj}_${data.first} .\n"
                res += "run:$name smol:withValue ${data.second}.\n"
            }
        }
        return res
    }
*/
    init {
        val inputScenMap = mutableMapOf<String, InputPortConfig>()
        val outputTempScenMap = mutableMapOf<String, scala.collection.immutable.List<String>>()
        for(mVar in sim.modelDescription.modelVariables){
            if(mVar.causality == "input" || mVar.causality == "parameter"){
                inputScenMap.put(mVar.name, InputPortConfig("delayed")) //todo: check what the reactivity models
                if(!mVar.hasStartValue() && !memory.containsKey(mVar.name))
                    throw Exception("Failed to initialize variable ${mVar.name}: no initial value given")
                if(memory.containsKey(mVar.name)) {
                    when (mVar.typeName) {
                        "Integer" -> sim.write(mVar.name).with(memory[mVar.name]!!.literal.toInt())
                        "Boolean" -> sim.write(mVar.name).with(memory[mVar.name]!!.literal.toBoolean())
                        "Real" -> sim.write(mVar.name).with(memory[mVar.name]!!.literal.toDouble())
                        else -> /*if (mVar.typeName == "String")*/ sim.write(mVar.name).with(memory[mVar.name]!!.literal.removeSurrounding("\""))
                    }
                } else if(mVar.hasStartValue()){
                    //println("using default start value for ${sim.modelDescription.modelName}.${mVar.name}")
                    val anyStart = mVar.start
                    when (mVar.typeName) {
                        "Integer" -> sim.write(mVar.name).with(anyStart as Int)
                        "Boolean" -> sim.write(mVar.name).with(anyStart as Boolean)
                        "Real" -> sim.write(mVar.name).with(anyStart as Double)
                        else -> /*if (mVar.typeName == "String")*/ sim.write(mVar.name).with(anyStart as String)
                    }
                }
            }
            if(mVar.causality == "output" && mVar.hasStartValue() && !memory.containsKey(mVar.name)){
                val anyStart = mVar.start
                outputTempScenMap.put(mVar.name, JavaConverters.asScala(listOf<String>()).toList()) //otherwise we miss the outputs without dependencies
                when (mVar.typeName) {
                    "Integer" -> sim.write(mVar.name).with(anyStart as Int)
                    "Boolean" -> sim.write(mVar.name).with(anyStart as Boolean)
                    "Real" -> sim.write(mVar.name).with(anyStart as Double)
                    else -> /*if (mVar.typeName == "String")*/ sim.write(mVar.name).with(anyStart as String)
                }
            }
            if((mVar.causality == "output" && mVar.initial == "calculated") && memory.containsKey(mVar.name)) {
                throw Exception("Cannot initialize output or/and calculated variable ${mVar.name}")
            }
        }

        for (dep in (sim.modelDescription as ModelDescription).modelStructure.outputs){
            val ind = dep.index
            val deps = dep.dependencies.split(" ").map { it.toInt() }
            val name = sim.modelDescription.getModelVariable(ind).name //-1?
            val depVars = deps.map { sim.modelDescription.getModelVariable(it).name }
            outputTempScenMap.put(name, JavaConverters.asScala(depVars).toList())
        }


        val outputScenMap = HashMap(outputTempScenMap.map { Pair(it.key, OutputPortConfig(JavaConverters.asScala(listOf<String>()).toList(), it.value))}.toMap())
        scen = FmuConfig(toScalaMap(inputScenMap), toScalaMap(outputScenMap), false, "" )

        sim.init(0.0)
        addSnapshot()
    }

    private fun <K,V> toScalaMap(inp : MutableMap<K, V>): HashMap<K, V>? {
        val ins1  = JavaConverters.mapAsScalaMapConverter(inp).asScala()
        return scala.collection.immutable.HashMap<K, V>().concat(ins1)
    }

    private fun dumpSingle(read : SingleRead, type : SimpleType) : String{
        return when(type){
            is org.javafmi.modeldescription.v2.IntegerType -> read.asInteger().toString()
            is org.javafmi.modeldescription.v1.IntegerType -> read.asInteger().toString()
            is org.javafmi.modeldescription.v2.StringType -> "'"+read.asString()+"'"
            is org.javafmi.modeldescription.v1.StringType -> "'"+read.asString()+"'"
            is org.javafmi.modeldescription.v2.BooleanType -> if(read.asBoolean()) "'1'" else "'0'"
            is org.javafmi.modeldescription.v1.BooleanType -> if(read.asBoolean()) "'1'" else "'0'"
            is org.javafmi.modeldescription.v2.RealType -> "'"+read.asDouble().toString()+"'"
            is org.javafmi.modeldescription.v1.RealType -> "'"+read.asDouble().toString()+"'"
            else -> throw Exception("Unknown Type")
        }
    }
}
