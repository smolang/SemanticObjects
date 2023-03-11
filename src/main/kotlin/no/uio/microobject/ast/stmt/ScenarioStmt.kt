package no.uio.microobject.ast.stmt

import no.uio.microobject.ast.Location
import no.uio.microobject.ast.Names
import no.uio.microobject.ast.Statement
import no.uio.microobject.ast.VarInit
import no.uio.microobject.runtime.*
import no.uio.microobject.type.Type

data class ScenarioStmt(val target : Location, val path: String, val pos : Int = -1, val declares: Type?) : Statement {
    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        val scenObj = SimulationScenario(path)
        val name = Names.getObjName("CoSimulationScenario")
        interpreter.scenMemory[name] = scenObj
        return replaceStmt(AssignStmt(target, name, declares = declares), stackFrame)
    }

    override fun toString(): String = "$target := monitor($path)"
    override fun getRDF(): String {
        //TODO: extend ontology
        return ""
    }
}