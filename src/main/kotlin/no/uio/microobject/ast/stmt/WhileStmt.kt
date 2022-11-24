package no.uio.microobject.ast.stmt

import no.uio.microobject.ast.Expression
import no.uio.microobject.ast.Statement
import no.uio.microobject.ast.appendStmt
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry

data class WhileStmt(val guard : Expression, val loopBody : Statement, val pos : Int = -1) : Statement {
    override fun toString(): String = "while $guard do $loopBody end"
    override fun getRDF(): String {
        return """
            prog:stmt${this.hashCode()} rdf:type smol:WhileStatement.
            prog:stmt${this.hashCode()} smol:hasGuard prog:expr${guard.hashCode()}.
            prog:stmt${this.hashCode()} smol:hasLoopBody prog:stmt${loopBody.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent() + guard.getRDF() + loopBody.getRDF()
    }

    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        return replaceStmt(IfStmt(guard, appendStmt(loopBody, this), SkipStmt()), stackFrame)
    }
}