package no.uio.microobject.ast.stmt

import no.uio.microobject.ast.Expression
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.ast.Statement
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry

data class DestroyStmt(val expr: Expression, val pos : Int = -1): Statement {
    override fun toString(): String = "destroy($expr)"
    override fun getRDF(): String {
        return """
            prog:stmt${this.hashCode()} rdf:type smol:DestroyStatement.
            prog:stmt${this.hashCode()} smol:hasStmtExpr prog:expr${expr.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent() + expr.getRDF()
    }

    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        val res = interpreter.eval(expr, stackFrame)
        if(!interpreter.heap.containsKey(res) || res.literal == "null")
            throw Exception("Trying to destroy null or an unknown object: $res")
        interpreter.heap.remove(res)
        //Replace name with ERROR. This is needed so the RDF dump does have dangling pointers and we cna derive weird things in the open world
        for( mem in interpreter.heap.values ){
            var rem :String? = null
            for( kv in mem ){
                if (kv.value == res) {
                    rem = kv.key
                    break
                }
            }
            if(rem != null) mem[rem] = LiteralExpr("null")
        }
        for( entry in interpreter.stack ){
            var rem :String? = null
            for( kv in entry.store ){
                if (kv.value == res) {
                    rem = kv.key
                    break
                }
            }
            if(rem != null) entry.store[rem] = LiteralExpr("null")
        }
        return EvalResult(null, emptyList())
    }
}