package no.uio.microobject.data.stmt

import no.uio.microobject.data.Expression
import no.uio.microobject.data.Statement
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry

data class TickStmt(val fmu: Expression, val tick : Expression, val pos : Int = -1) : Statement {
    override fun toString(): String = "tick($fmu, $tick)"
    override fun getRDF(): String {
        //TODO: extend ontology
        return ""
    }

    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        val target = interpreter.eval(fmu, stackFrame)
        if(!interpreter.simMemory.containsKey(target)) throw Exception("Object $target is no a simulation object")
        val tickTime = interpreter.eval(tick, stackFrame)
        interpreter.simMemory[target]!!.tick(tickTime.literal.toDouble())
        return EvalResult(null, emptyList())
    }
}