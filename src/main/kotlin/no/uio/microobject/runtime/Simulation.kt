package no.uio.microobject.runtime

import no.uio.microobject.type.BOOLEANTYPE
import no.uio.microobject.type.INTTYPE
import no.uio.microobject.data.LiteralExpr
import no.uio.microobject.data.TRUEEXPR
import no.uio.microobject.type.DOUBLETYPE
import no.uio.microobject.type.STRINGTYPE
import no.uio.microobject.type.Type
import org.javafmi.modeldescription.SimpleType
import org.javafmi.wrapper.Simulation
import org.javafmi.wrapper.variables.SingleRead

data class Snapshot( val time : Double, val values : List<Pair<String, Double>>, val role : String?)

class SimulatorObject(val path : String,
                      memory : Memory,
                      // optional variable name transformers: first element is
                      // FMU-side name, third element is SMOL-side name
                      val inVals : List<Triple<String, Type, String>>?,
                      val outVals : List<Triple<String, Type, String>>?)
{
    companion object  {
        const val ROLEFIELDNAME = "role"
        const val PSEUDOOFFSETFIELDNAME = "pseudoOffset"
        const val TIMEFIELDNAME = "time"
    }
    private val series = mutableListOf<Snapshot>()
    private var sim : Simulation = Simulation(path)
    private var time : Double = 0.0

    //additional fields
    private var role : String = ""
    private var pseudoOffset : Double = 0.0

    fun read(name: String): LiteralExpr {
        if(name == ROLEFIELDNAME) return LiteralExpr(role, STRINGTYPE)
        if(name == PSEUDOOFFSETFIELDNAME) return LiteralExpr(pseudoOffset.toString(), DOUBLETYPE)
        if(name == TIMEFIELDNAME) return LiteralExpr(time.toString(), DOUBLETYPE)
        val realname = (inVals?.find { it.third.equals(name)})?.first ?: name
        val v = sim.modelDescription.getModelVariable(realname)
        if(v.typeName == "Integer") return LiteralExpr(sim.read(realname).asInteger().toString(), INTTYPE)
        if(v.typeName == "Boolean") return LiteralExpr(sim.read(realname).asBoolean().toString(), BOOLEANTYPE)
        if(v.typeName == "Real") return LiteralExpr(sim.read(realname).asDouble().toString(), DOUBLETYPE)
        return LiteralExpr(sim.read(realname).asString(), STRINGTYPE)
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
        val realname = (outVals?.find { it.third.equals(name)})?.first ?: name
        for(mVar in sim.modelDescription.modelVariables){
            if(mVar.name == realname){
                if(mVar.causality == "input" && mVar.typeName == "Integer"){
                    sim.write(realname).with(res.literal.toInt())
                    break
                } else if(mVar.causality == "input" && mVar.typeName == "Boolean"){
                    sim.write(realname).with(res == TRUEEXPR)
                    break
                } else if(mVar.causality == "input" && mVar.typeName == "Real"){
                    sim.write(realname).with(res.literal.toDouble())
                    break
                } else if(mVar.causality == "input" && mVar.typeName == "String"){
                    sim.write(realname).with(res.literal )
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
        for(mVar in sim.modelDescription.modelVariables){
            if(mVar.causality == "input" || mVar.causality == "parameter"){
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
        sim.init(0.0)
        addSnapshot()
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
