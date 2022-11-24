package no.uio.microobject.ast.stmt

import no.uio.microobject.ast.Location
import no.uio.microobject.ast.Names
import no.uio.microobject.ast.Statement
import no.uio.microobject.ast.VarInit
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