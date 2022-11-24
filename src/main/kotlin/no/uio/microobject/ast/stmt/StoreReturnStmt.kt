package no.uio.microobject.ast.stmt

import no.uio.microobject.ast.Location
import no.uio.microobject.ast.Statement
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry

// This is a runtime-syntax only statement which models that we will write the return value of the next method in the stack into target
data class StoreReturnStmt(val target : Location, val pos : Int = -1) : Statement {
    override fun toString(): String = "$target <- stack"
    override fun getRDF(): String {
        return """
            prog:stmt${this.hashCode()} rdf:type smol:StoreReturnStatement.
            prog:stmt${this.hashCode()} smol:hasTarget prog:loc${target.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent() + target.getRDF()
    }

    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        throw Exception("StoreReturnStmt found on top of stack")
    }
}