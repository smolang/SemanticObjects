package no.uio.microobject.ast.expr

import no.uio.microobject.ast.*
import no.uio.microobject.runtime.GlobalMemory
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.SimulationMemory
import no.uio.microobject.type.*

data class ArithExpr(val Op : Operator, val params: List<Expression>, val tag : Type = ERRORTYPE) : Expression {
    override fun toString(): String = "($Op ${params.joinToString(" ")})"
    override fun eval(stack: Memory, heap: GlobalMemory, simMemory: SimulationMemory, obj: LiteralExpr): LiteralExpr {
        val first = params.getOrNull(0)
        val second = params.getOrNull(1)
        when(Op){
            Operator.EQ -> {
                if (params.size != 2 || first == null || second == null) throw Exception("Operator.EQ requires two parameters")
                return evalBool(first.eval(stack, heap, simMemory, obj) == second.eval(stack, heap, simMemory, obj))
            }
            Operator.NEQ -> {
                if (params.size != 2 || first == null || second == null) throw Exception("Operator.NEQ requires two parameters")
                return evalBool(first.eval(stack, heap, simMemory, obj) != second.eval(stack, heap, simMemory, obj))
            }
            Operator.GEQ -> {
                if (params.size != 2 || first == null || second == null) throw Exception("Operator.GEQ requires two parameters")
                return evalBool(
                    first.eval(stack, heap, simMemory, obj).literal.toDouble() >=
                            second.eval(stack, heap, simMemory, obj).literal.toDouble()
                )
            }
            Operator.LEQ -> {
                if (params.size != 2 || first == null || second == null) throw Exception("Operator.LEQ requires two parameters")
                return evalBool(
                    first.eval(stack, heap, simMemory, obj).literal.toDouble() <=
                            second.eval(stack, heap, simMemory, obj).literal.toDouble()
                )
            }
            Operator.GT -> {
                if (params.size != 2 || first == null || second == null) throw Exception("Operator.GT requires two parameters")
                return evalBool(
                    first.eval(stack, heap, simMemory, obj).literal.toDouble() >
                            second.eval(stack, heap, simMemory, obj).literal.toDouble()
                )
            }
            Operator.LT -> {
                if (params.size != 2 || first == null || second == null) throw Exception("Operator.LT requires two parameters")
                return evalBool(
                    first.eval(stack, heap, simMemory, obj).literal.toDouble() <
                            second.eval(stack, heap, simMemory, obj).literal.toDouble()
                )
            }
            Operator.AND -> {
                if (params.size != 2 || first == null || second == null) throw Exception("Operator.AND requires two parameters")
                return evalBool(
                    first.eval(stack, heap, simMemory, obj) == TRUEEXPR &&
                            second.eval(stack, heap, simMemory, obj) == TRUEEXPR
                )
            }
            Operator.OR -> {
                if (params.size != 2 || first == null || second == null) throw Exception("Operator.OR requires two parameters")
                return evalBool(
                    first.eval(stack, heap, simMemory, obj) == TRUEEXPR ||
                            second.eval(stack, heap, simMemory, obj) == TRUEEXPR
                )
            }
            Operator.MINUS ->  {
                if (params.size != 2 || first == null || second == null) throw Exception("Operator.MINUS requires two parameters")
                val enx1 = first.eval(stack, heap, simMemory, obj)
                val enx2 = second.eval(stack, heap, simMemory, obj)
                return if(enx1.tag == DOUBLETYPE)
                    LiteralExpr(
                        (enx1.literal.removePrefix("urn:").toDouble() - enx2.literal.removePrefix("urn:")
                            .toDouble()).toString(), DOUBLETYPE
                    )
                else
                    LiteralExpr(
                        (enx1.literal.removePrefix("urn:").toInt() - enx2.literal.removePrefix("urn:")
                            .toInt()).toString(), INTTYPE
                    )
            }
            Operator.PLUS ->  {
                if (params.size != 2 || first == null || second == null) throw Exception("Operator.PLUS requires two parameters")
                val enx1 = first.eval(stack, heap, simMemory, obj)
                val enx2 = second.eval(stack, heap, simMemory, obj)
                return if(enx1.tag == DOUBLETYPE)
                    LiteralExpr(
                        (enx1.literal.removePrefix("urn:").toDouble() + enx2.literal.removePrefix("urn:")
                            .toDouble()).toString(), DOUBLETYPE
                    )
                else
                    LiteralExpr(
                        (enx1.literal.removePrefix("urn:").toInt() + enx2.literal.removePrefix("urn:")
                            .toInt()).toString(), INTTYPE
                    )
            }
            Operator.MULT ->  {
                if (params.size != 2 || first == null || second == null) throw Exception("Operator.MULT requires two parameters")
                val enx1 = first.eval(stack, heap, simMemory, obj)
                val enx2 = second.eval(stack, heap, simMemory, obj)
                return if(enx1.tag == DOUBLETYPE)
                    LiteralExpr(
                        (enx1.literal.removePrefix("urn:").toDouble() * enx2.literal.removePrefix("urn:")
                            .toDouble()).toString(), DOUBLETYPE
                    )
                else
                    LiteralExpr(
                        (enx1.literal.removePrefix("urn:").toInt() * enx2.literal.removePrefix("urn:")
                            .toInt()).toString(), INTTYPE
                    )
            }
            Operator.MOD ->  {
                if (params.size != 2 || first == null || second == null) throw Exception("Operator.MOD requires two parameters")
                val enx1 = first.eval(stack, heap, simMemory, obj)
                val enx2 = second.eval(stack, heap, simMemory, obj)
                return if(enx1.tag == DOUBLETYPE)
                    LiteralExpr(
                        (enx1.literal.removePrefix("urn:").toDouble() % enx2.literal.removePrefix("urn:")
                            .toDouble()).toString(), DOUBLETYPE
                    )
                else
                    LiteralExpr(
                        (enx1.literal.removePrefix("urn:").toInt() % enx2.literal.removePrefix("urn:")
                            .toInt()).toString(), INTTYPE
                    )
            }
            Operator.DIV ->  {
                if (params.size != 2 || first == null || second == null) throw Exception("Operator.DIV requires two parameters")
                val enx1 = first.eval(stack, heap, simMemory, obj)
                val enx2 = second.eval(stack, heap, simMemory, obj)
                return if(enx1.tag == DOUBLETYPE)
                    LiteralExpr(
                        (enx1.literal.removePrefix("urn:").toDouble() / enx2.literal.removePrefix("urn:")
                            .toDouble()).toString(), DOUBLETYPE
                    )
                else
                    LiteralExpr(
                        (enx1.literal.removePrefix("urn:").toInt() / enx2.literal.removePrefix("urn:")
                            .toInt()).toString(), INTTYPE
                    )
            }
            Operator.NOT -> {
                if (params.size != 1 || first == null ) throw Exception("Operator.NOT requires one parameter")
                if (first.eval(stack, heap, simMemory, obj) == FALSEEXPR) return TRUEEXPR
                else return FALSEEXPR
            }
            Operator.CONCAT -> {
                if (params.size != 2 || first == null || second == null) throw Exception("Operator.CONCAT requires two parameters")
                val enx1 = first.eval(stack, heap, simMemory, obj)
                val enx2 = second.eval(stack, heap, simMemory, obj)
                return LiteralExpr(
                        enx1.literal.removeSurrounding("\"") + enx2.literal.removeSurrounding("\"") , STRINGTYPE
                    )
            }
        }
    }

    override fun getRDF(): String {
        var s = """
            prog:expr${this.hashCode()} rdf:type smol:ArithExpression.
            prog:expr${this.hashCode()} smol:hasOp "$Op".

        """.trimIndent()
        for (i in params.indices){
            s += "prog:expr${this.hashCode()} smol:hasOperand [smol:hasOperandIndex $i ; smol:hasOperandValue prog:expr${params[i].hashCode()}; ].\n"
            s += params[i].getRDF()
        }
        return s
    }

}

enum class Operator {
    PLUS, MINUS, MULT, DIV, NEQ, GEQ, EQ, LEQ, LT, GT, AND, OR, NOT, MOD, CONCAT;
    companion object{
        fun toJava(op : Operator) : String {
            return when(op){
                PLUS  -> "+"
                MINUS -> "-"
                MULT  -> "*"
                DIV   -> "/"
                NEQ   -> "!="
                GEQ   -> ">="
                EQ    -> "=="
                LEQ   -> "<="
                LT    -> "<"
                GT    -> ">"
                AND   -> "&&"
                OR    -> "||"
                NOT   -> "!"
                MOD   -> "%"
                CONCAT -> "+"
            }
        }
    }
}