package no.uio.microobject.data.stmt

import no.uio.microobject.data.Statement
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry

// Stops automatic execution
data class DebugStmt(val pos : Int = -1) : Statement {
    override fun toString(): String = "breakpoint"
    override fun getRDF(): String = "prog:stmt${this.hashCode()} rdf:type smol:DebugStatement.\nprog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.\n"

    override fun eval(heapObj: Memory, stackFrame : StackEntry, interpreter: Interpreter) : EvalResult {
        return EvalResult(null, emptyList(), true)
    }
}