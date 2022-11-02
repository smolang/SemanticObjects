@file:Suppress("unused")

package no.uio.microobject.data

import no.uio.microobject.data.stmt.SequenceStmt
import no.uio.microobject.data.stmt.SkipStmt
import no.uio.microobject.runtime.*
import no.uio.microobject.type.*

/*
 * Simple extensions:
 *  - Allows calls and creations within expressions
 *  - Constructors and internal fields
 *  - Encapsulation (private/public)
 *  - More operators
 */

enum class Operator {
    PLUS, MINUS, MULT, DIV, NEQ, GEQ, EQ, LEQ, LT, GT, AND, OR, NOT, MOD;
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
            }
        }
    }
}

abstract class AccessMode
data class InfluxDBMode(val config : String) : AccessMode()
object SparqlMode : AccessMode()



interface ProgramElement{
    fun getRDF() : String
}


interface Statement : ProgramElement {
    fun getLast(): Statement = this
    fun eval(heapObj: Memory, stackFrame : StackEntry, interpreter: Interpreter) : EvalResult

    fun replaceStmt(stmt: Statement, stackFrame : StackEntry) : EvalResult
      = EvalResult(StackEntry(stmt, stackFrame.store, stackFrame.obj, stackFrame.id), listOf())
}

interface Expression : ProgramElement
interface Location : Expression{
    fun getType() : Type
    fun setType(targetType: Type)
}


data class VarInit(val name : String, val expr: Expression) : ProgramElement {
    override fun toString(): String = "$name : $expr"
    override fun getRDF(): String {
        //TODO: extend ontology
        return ""
    }

}

/** Expressions **/


data class LocalVar(val name : String, var tag : Type = ERRORTYPE) : Location { // local variable
    override fun toString(): String = name
    override fun getType(): Type = tag
    override fun setType(targetType: Type) { tag = targetType }
    override fun getRDF(): String {
        return """
            prog:loc${this.hashCode()} rdf:type smol:LocalVarLocation.
            prog:loc${this.hashCode()} smol:hasName '${name}'.

        """.trimIndent()
    }

}
data class OwnVar(val name : String, var tag : Type = ERRORTYPE) : Location {   // field of own object
    override fun toString(): String = "this.$name"
    override fun getType(): Type = tag
    override fun setType(targetType: Type) { tag = targetType }
    override fun getRDF(): String {
        return """
            prog:loc${this.hashCode()} rdf:type smol:OwnVarLocation.
            prog:loc${this.hashCode()} smol:hasName '${name}'.

        """.trimIndent()
    }
}
data class OthersVar(val expr: Expression, val name : String, var tag : Type = ERRORTYPE) : Location { // field of (possibly) other object
    override fun toString(): String = "$expr.$name"
    override fun getType(): Type = tag
    override fun setType(targetType: Type) { tag = targetType }
    override fun getRDF(): String {
        return """
            prog:loc${this.hashCode()} rdf:type smol:OthersVarLocation.
            prog:loc${this.hashCode()} smol:hasExpr prog:expr${expr.hashCode()}.
            prog:loc${this.hashCode()} smol:hasName '${name}'.

        """.trimIndent()
    }
}


data class ArithExpr(val Op : Operator, val params: List<Expression>, val tag : Type = ERRORTYPE) : Expression {
    override fun toString(): String = "($Op ${params.joinToString(" ")})"
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
data class LiteralExpr(val literal : String, val tag : Type = ERRORTYPE) : Expression {
    override fun toString(): String = literal
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


fun appendStmt (a : Statement, b: Statement) : Statement {
    if(b is SkipStmt) return a
    if(a is SkipStmt) return b
    return if(a !is SequenceStmt) SequenceStmt(a, b) else SequenceStmt(a.first, appendStmt(a.second,b))
}

// Use this whenever we need a new unique name
object Names{
    private var i = 0
    private var j = 0
    private var k = 0
    fun getObjName(className : String) : LiteralExpr = LiteralExpr("obj${i++}", BaseType(className))
    fun getVarName(tag : Type = ERRORTYPE) : LocalVar = LocalVar("_v${i++}", tag)
    fun getStackId() : Int = j++
    fun getNodeName() : String = "domain:model${k++}"
}

val FALSEEXPR = LiteralExpr("False", BOOLEANTYPE)
val TRUEEXPR = LiteralExpr("True", BOOLEANTYPE)
val UNITEXPR = LiteralExpr("unit", UNITTYPE)
