package no.uio.microobject.ast.stmt

import no.uio.microobject.ast.Statement
import no.uio.microobject.ast.appendStmt
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry

// We use a binary tree instead of a list to make the interpreter more simple.
// The value of first is NOT allowed to be another SequenceStmt. Use appendStmt below to build trees.
data class SequenceStmt(val first: Statement, val second : Statement) : Statement {
    override fun getLast(): Statement = second
    override fun toString(): String = "$first; $second"
    override fun getRDF(): String {
        return """
            prog:stmt${this.hashCode()} rdf:type smol:SequenceStatement.
            prog:stmt${this.hashCode()} smol:first prog:stmt${first.hashCode()}.
            prog:stmt${this.hashCode()} smol:second prog:stmt${second.hashCode()}.

        """.trimIndent() + first.getRDF() + second.getRDF()
    }

    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        if (first is ReturnStmt) return first.eval(heapObj, stackFrame, interpreter)
        val res = first.eval(heapObj, stackFrame, interpreter)
        if (res.current != null) {
            val newStmt = appendStmt(res.current.active, second)
            return EvalResult(
                StackEntry(newStmt, res.current.store, res.current.obj, stackFrame.id),
                res.spawns,
                res.debug
            )
        } else return EvalResult(
            StackEntry(second, stackFrame.store, stackFrame.obj, stackFrame.id),
            res.spawns,
            res.debug
        )
    }

}