package no.uio.microobject.data.stmt

import no.uio.microobject.data.Expression
import no.uio.microobject.data.Statement
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry

//for output
data class PrintStmt(val expr: Expression, val pos : Int = -1): Statement {
    override fun toString(): String = "println($expr)"
    override fun getRDF(): String {
        return """
            prog:stmt${this.hashCode()} rdf:type smol:PrintStatement.
            prog:stmt${this.hashCode()} smol:hasStmtExpr prog:expr${expr.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent() + expr.getRDF()
    }

    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        println(interpreter.eval(expr, stackFrame))
        return EvalResult(null, emptyList())
    }

}