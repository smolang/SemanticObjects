package no.uio.microobject.data.stmt

import no.uio.microobject.data.Location
import no.uio.microobject.data.Names
import no.uio.microobject.data.Statement
import no.uio.microobject.data.VarInit
import no.uio.microobject.runtime.*
import no.uio.microobject.type.Type

// For simulation interface
data class SimulationStmt(val target : Location, val path: String, val params : List<VarInit>, val pos : Int = -1, val declares: Type?) :
    Statement {
    override fun toString(): String = "$target := simulate($path, ${params.joinToString(",")})"
    override fun getRDF(): String {
        //TODO: extend ontology
        return ""
    }

    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        val simObj = SimulatorObject(path, params.associate {
            Pair(it.name, interpreter.eval(it.expr, stackFrame))
        }.toMutableMap())
        val name = Names.getObjName("CoSimulation")
        interpreter.simMemory[name] = simObj
        return replaceStmt(AssignStmt(target, name, declares = declares), stackFrame)
    }
}