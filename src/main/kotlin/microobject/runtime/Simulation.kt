package microobject.runtime

import microobject.type.BOOLEANTYPE
import microobject.type.INTTYPE
import microobject.data.LiteralExpr
import microobject.data.TRUEEXPR
import microobject.type.DOUBLETYPE
import microobject.type.STRINGTYPE
import org.javafmi.modeldescription.SimpleType
import org.javafmi.wrapper.Simulation
import org.javafmi.wrapper.variables.SingleRead

class SimulatorObject(val path : String, memory : Memory){
    fun read(name: String): LiteralExpr {
        val v = sim.modelDescription.getModelVariable(name)
        if(v.typeName == "Integer") return LiteralExpr(sim.read(name).asInteger().toString(), INTTYPE)
        if(v.typeName == "Boolean") return LiteralExpr(sim.read(name).asBoolean().toString(), BOOLEANTYPE)
        if(v.typeName == "Real") return LiteralExpr(sim.read(name).asDouble().toString(), DOUBLETYPE)
        return LiteralExpr(sim.read(name).asString(), STRINGTYPE)
    }
    fun tick(i : Double){
        sim.doStep(i)
    }

    fun write(name: String, res: LiteralExpr) {
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


    private var sim : Simulation = Simulation(path)
    fun terminate() {
        sim.terminate()
    }

    //TODO: use serialization if available
    fun dump(obj: String): String {
        var res = "$obj smol:modelName '${sim.modelDescription.modelName}'.\n"
        for(mVar in sim.modelDescription.modelVariables) {
            if(mVar.causality == "input") {
                res += "$obj smol:hasInPort prog:${mVar.name}.\n"
            }
            if(mVar.causality == "output"){
                res += "$obj smol:hasOutPort prog:${mVar.name}.\n"
                res += "$obj prog:${mVar.name} ${dumpSingle(sim.read(mVar.name),mVar.type)}.\n"
            }
            if(mVar.causality == "parameter"){
                res += "$obj smol:hasStatePort prog:${mVar.name}.\n"
                res += "$obj prog:${mVar.name} ${dumpSingle(sim.read(mVar.name),mVar.type)}.\n"
                mVar.type
            }
        }
        return res
    }

    init {
        for(mVar in sim.modelDescription.modelVariables){
            if(mVar.causality == "input" || mVar.causality == "state"){
                if(!mVar.hasStartValue() && !memory.containsKey(mVar.name))
                    throw Exception("Failed to initialize variable ${mVar.name}: no initial value given")
                if(memory.containsKey(mVar.name)) {
                    if (mVar.typeName == "Integer") sim.write(mVar.name).with(memory[mVar.name]!!.literal.toInt())
                    else if (mVar.typeName == "Boolean") sim.write(mVar.name).with(memory[mVar.name]!!.literal.toBoolean())
                    else if (mVar.typeName == "Real") sim.write(mVar.name).with(memory[mVar.name]!!.literal.toDouble())
                    else /*if (mVar.typeName == "String")*/ sim.write(mVar.name).with(memory[mVar.name]!!.literal.removeSurrounding("\""))
                } else if(mVar.hasStartValue()){
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
            if((mVar.causality == "output" || mVar.initial == "calculated") && memory.containsKey(mVar.name)) {
                throw Exception("Cannot initialize output or/and calculated variable ${mVar.name}")
            }
        }
        println(sim.init(0.0))
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
            else -> throw java.lang.Exception("Unknown Type")
        }
    }
}