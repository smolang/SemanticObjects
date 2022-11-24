package no.uio.microobject.ast.expr

import no.uio.microobject.ast.Location
import no.uio.microobject.runtime.GlobalMemory
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.SimulationMemory
import no.uio.microobject.type.ERRORTYPE
import no.uio.microobject.type.Type

/** Expressions **/


data class LocalVar(val name : String, var tag : Type = ERRORTYPE) : Location { // local variable
    override fun toString(): String = name
    override fun getType(): Type = tag
    override fun setType(targetType: Type) { tag = targetType }
    override fun eval(stack: Memory, heap: GlobalMemory, simMemory: SimulationMemory, obj: LiteralExpr): LiteralExpr {
        return stack.getOrDefault(name, LiteralExpr("ERROR"))
    }

    override fun getRDF(): String {
        return """
            prog:loc${this.hashCode()} rdf:type smol:LocalVarLocation.
            prog:loc${this.hashCode()} smol:hasName '${name}'.

        """.trimIndent()
    }

}