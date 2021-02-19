package microobject.runtime

import microobject.data.INTTYPE
import microobject.data.LiteralExpr
import org.javafmi.wrapper.Simulation

class SimulatorObject(val path : String, memory : Memory){
    fun read(name: String): LiteralExpr {
        val v = sim.modelDescription.getModelVariable(name)
        if(v.typeName != "Integer") throw Exception("Failed to read variable ${v.name}: only Integer variables are supported")
        return LiteralExpr(sim.read(name).asInteger().toString(), INTTYPE.name)
    }
    fun tick(i : Int){
        sim.doStep(i.toDouble())
    }

    fun write(name: String, res: LiteralExpr) {
        for(mVar in sim.modelDescription.modelVariables){
            if(mVar.name == name){
                if(mVar.causality == "input" && mVar.typeName == "Integer"){
                    sim.write(name).with(res.literal.toInt())
                    break
                } else throw Exception("Failed to assign to variable $name")
            }
        }
    }

    private var sim : Simulation = Simulation(path)

    init {
        for(mVar in sim.modelDescription.modelVariables){
            if(mVar.causality == "input" || mVar.causality == "state"){
                if(!mVar.hasStartValue() && !memory.containsKey(mVar.name))
                    throw Exception("Failed to initialize variable ${mVar.name}: no initial value given")
                if(memory.containsKey(mVar.name)) {
                    if (mVar.typeName == "Integer") sim.write(mVar.name).with(memory[mVar.name]!!.literal.toInt())
                    else throw Exception("Failed to initialize variable ${mVar.name}: only Integer variables are supported")
                }
            }
            if((mVar.causality == "output" || mVar.initial == "calculated") && memory.containsKey(mVar.name)) {
                throw Exception("Cannot initialize output or/and calculated variable ${mVar.name}")
            }
        }
    }
}