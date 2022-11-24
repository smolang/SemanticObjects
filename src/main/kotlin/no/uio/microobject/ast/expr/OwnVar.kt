package no.uio.microobject.ast.expr

import no.uio.microobject.ast.Location
import no.uio.microobject.runtime.GlobalMemory
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.SimulationMemory
import no.uio.microobject.type.ERRORTYPE
import no.uio.microobject.type.Type

data class OwnVar(val name : String, var tag : Type = ERRORTYPE) : Location {   // field of own object
    override fun toString(): String = "this.$name"
    override fun getType(): Type = tag
    override fun setType(targetType: Type) { tag = targetType }
    override fun eval(stack: Memory, heap: GlobalMemory, simMemory: SimulationMemory, obj: LiteralExpr): LiteralExpr {
        if(heap[obj] == null) throw Exception("This object is unknown: $obj$")
        val heapObj: Memory = heap.getOrDefault(obj, mutableMapOf())
        return heapObj.getOrDefault(name, LiteralExpr("ERROR"))
    }

    override fun getRDF(): String {
        return """
            prog:loc${this.hashCode()} rdf:type smol:OwnVarLocation.
            prog:loc${this.hashCode()} smol:hasName '${name}'.

        """.trimIndent()
    }
}