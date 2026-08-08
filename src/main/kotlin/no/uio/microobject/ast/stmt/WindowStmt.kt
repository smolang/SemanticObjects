package no.uio.microobject.ast.stmt

import no.uio.microobject.ast.Expression
import no.uio.microobject.ast.Statement
import no.uio.microobject.ast.Location
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry
import no.uio.microobject.type.Type

data class WindowStmt(val target: Location, val monitor: Expression, val pos : Int = -1, val declares: Type?) : Statement {
    override fun toString(): String = "$target := window($monitor)"
    override fun getRDF(): String {
        //TODO: extend ontology
        return ""
    }

    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        val lit = interpreter.eval(monitor, stackFrame)
        val monitorObj = interpreter.streamManager.getMonitor(lit)
        if(monitorObj == null)
            throw Exception("Object $monitorObj is not a monitor object")
        val list = monitorObj.getWindowResults(interpreter)
        return replaceStmt(AssignStmt(target, list, pos=pos, declares=declares), stackFrame)
    }
}