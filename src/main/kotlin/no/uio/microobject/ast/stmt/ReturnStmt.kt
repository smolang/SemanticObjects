package no.uio.microobject.ast.stmt

import no.uio.microobject.ast.*
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry

// Return statement
data class ReturnStmt(var value : Expression, val pos : Int = -1) : Statement {
    override fun toString(): String = "return $value"
    override fun getRDF(): String {
        return """
            prog:stmt${this.hashCode()} rdf:type smol:ReturnStatement.
            prog:stmt${this.hashCode()} smol:hasValue prog:expr${value.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent() + value.getRDF()
    }

    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        val over = interpreter.stack.pop()
        if (over.active is StoreReturnStmt) {
            val res = interpreter.eval(value, stackFrame)
            return EvalResult(
                StackEntry(
                    AssignStmt(over.active.target, res, declares = null),
                    over.store,
                    over.obj,
                    over.id
                ), listOf()
            )
        }
        if (over.active is SequenceStmt && over.active.first is StoreReturnStmt) {
            val active = over.active.first
            val next = over.active.second
            val res = interpreter.eval(value, stackFrame)
            return replaceStmt(appendStmt(AssignStmt(active.target, res, declares = null), next), over)
        }
        throw Exception("Malformed heap")
    }
}