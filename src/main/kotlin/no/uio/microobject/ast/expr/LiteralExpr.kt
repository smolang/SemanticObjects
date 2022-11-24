package no.uio.microobject.ast.expr

import no.uio.microobject.ast.Expression
import no.uio.microobject.runtime.GlobalMemory
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.SimulationMemory
import no.uio.microobject.type.BOOLEANTYPE
import no.uio.microobject.type.ERRORTYPE
import no.uio.microobject.type.Type
import no.uio.microobject.type.UNITTYPE

data class LiteralExpr(val literal : String, val tag : Type = ERRORTYPE) : Expression {
    override fun toString(): String = literal
    override fun eval(stack: Memory, heap: GlobalMemory, simMemory: SimulationMemory, obj: LiteralExpr): LiteralExpr = this

    override fun getRDF(): String {
        return """
            prog:expr${this.hashCode()} rdf:type smol:LiteralExpression.
            prog:expr${this.hashCode()} smol:hasLiteral '${literal.removePrefix("\"").removeSuffix("\"")}'.
            prog:expr${this.hashCode()} smol:hasTag '${tag}'.
        """.trimIndent()
    }

    override fun equals(other: Any?): Boolean {
        if(other == null) return false
        if(other !is LiteralExpr) return false
        return literal == other.literal
    }

    override fun hashCode(): Int {
        var result = literal.hashCode()
        result = 31 * result + tag.hashCode()
        return result
    }
}

val FALSEEXPR = LiteralExpr("False", BOOLEANTYPE)
val TRUEEXPR = LiteralExpr("True", BOOLEANTYPE)
val UNITEXPR = LiteralExpr("unit", UNITTYPE)