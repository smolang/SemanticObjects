package no.uio.microobject.ast.expr

import no.uio.microobject.ast.Expression
import no.uio.microobject.ast.Location
import no.uio.microobject.runtime.GlobalMemory
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.SimulationMemory
import no.uio.microobject.type.ERRORTYPE
import no.uio.microobject.type.Type

data class OthersVar(val expr: Expression, val name : String, var tag : Type = ERRORTYPE) : Location { // field of (possibly) other object
    override fun toString(): String = "$expr.$name"
    override fun getType(): Type = tag
    override fun setType(targetType: Type) { tag = targetType }
    override fun eval(stack: Memory, heap: GlobalMemory, simMemory: SimulationMemory, obj: LiteralExpr): LiteralExpr {
        val oObj = expr.eval(stack, heap, simMemory, obj)
        if(heap.containsKey(oObj)) return heap[oObj]!!.getOrDefault(name, LiteralExpr("ERROR"))
        if(simMemory.containsKey(oObj)) return simMemory[oObj]!!.read(name)
        throw Exception("Unknown object $oObj stored in $expr")
    }

    override fun getRDF(): String {
        return """
            prog:loc${this.hashCode()} rdf:type smol:OthersVarLocation.
            prog:loc${this.hashCode()} smol:hasExpr prog:expr${expr.hashCode()}.
            prog:loc${this.hashCode()} smol:hasName '${name}'.

        """.trimIndent()
    }
}