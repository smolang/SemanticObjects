package no.uio.microobject.ast.stmt

import no.uio.microobject.ast.Expression
import no.uio.microobject.ast.Statement
import no.uio.microobject.ast.expr.TRUEEXPR
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry

// Standard control flow
data class IfStmt(val guard : Expression, val thenBranch : Statement, val elseBranch : Statement, val pos : Int = -1) :
    Statement {
    override fun toString(): String = "if($guard) then $thenBranch else $elseBranch fi"
    override fun getRDF(): String {
        return """
            prog:stmt${this.hashCode()} rdf:type smol:IfStatement.
            prog:stmt${this.hashCode()} smol:hasGuard prog:expr${guard.hashCode()}.
            prog:stmt${this.hashCode()} smol:hasThenBranch prog:stmt${thenBranch.hashCode()}.
            prog:stmt${this.hashCode()} smol:hasElseBranch prog:stmt${elseBranch.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent() + guard.getRDF() + thenBranch.getRDF() + elseBranch.getRDF()
    }

    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        val res = interpreter.eval(guard, stackFrame)
        return if (res == TRUEEXPR) replaceStmt(thenBranch, stackFrame)
               else                 replaceStmt(elseBranch, stackFrame)
    }
}