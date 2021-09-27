@file:Suppress("unused")

package no.uio.microobject.data

import no.uio.microobject.backend.JavaBackend
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
    fun toJava(builder: JavaBackend, semantic: Boolean = false) {
            throw Exception("translating ${this.javaClass.name} is not supported yet")
    }
}


interface Statement : ProgramElement {
    fun getLast(): Statement = this
}

interface Expression : ProgramElement
interface Location : Expression{
    fun getType() : Type
    fun setType(targetType: Type)
}

/** Statement **/

// Empty statement. Handy for unrolling of loops.
data class SkipStmt(val pos : Int = -1) : Statement{
    override fun toString(): String = "skip"
    override fun getRDF(): String = "prog:stmt${this.hashCode()} rdf:type smol:SkipStatement.\nprog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.\n"
    override fun toJava(builder: JavaBackend, semantic: Boolean) {
        builder.addText(";/*empty statement*/")
    }
}


// Stops automatic execution
data class DebugStmt(val pos : Int = -1) : Statement{
    override fun toString(): String = "breakpoint"
    override fun getRDF(): String = "prog:stmt${this.hashCode()} rdf:type smol:DebugStatement.\nprog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.\n"
    override fun toJava(builder: JavaBackend, semantic: Boolean) {
        builder.addText(";/*debug statement*/")
    }
}


// Assignment, where value cannot refer to calls or object creations.
data class AssignStmt(val target : Location, val value : Expression, val pos : Int = -1, val declares: Type?) : Statement {
    override fun toString(): String = "$target := $value"
    override fun getRDF(): String {
        return """
            prog:stmt${this.hashCode()} rdf:type smol:AssignStatement.
            prog:stmt${this.hashCode()} smol:hasTarget prog:loc${target.hashCode()}.
            prog:stmt${this.hashCode()} smol:hasValue prog:expr${value.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent()
    }

    override fun toJava(builder: JavaBackend, semantic: Boolean) {
        if(declares != null) {
            var type = declares.toString()
            if(type == "Int") type = "int"
            builder.addText("$type ")
        }
        builder.addIntend()
        target.toJava(builder, semantic)
        builder.addText(" = ")
        value.toJava(builder, semantic)
        builder.addText(";")

    }
}

// Super call. MethodName is merely saved for easier access for the interpreter
data class SuperStmt(val target : Location, val methodName : String, val params : List<Expression>, val pos : Int = -1, val declares: Type?) :
    Statement {
    override fun toString(): String = "$target := super(${params.joinToString(",")})"
    override fun getRDF(): String {
        var s = """
            prog:stmt${this.hashCode()} rdf:type smol:SuperStatement.
            prog:stmt${this.hashCode()} smol:hasTarget prog:loc${target.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent()
        for (i in params.indices){
            s += "prog:stmt${this.hashCode()} smol:hasParameter [smol:hasParameterIndex $i ; smol:hasParameterValue prog:expr${params[i].hashCode()}; ].\n"
            s += params[i].getRDF()
        }
        return s + target.getRDF()
    }

    override fun toJava(builder: JavaBackend, semantic: Boolean) {
        builder.addIntend()
        if(declares != null) {
            var type = declares.toString()
            if(type == "Int") type = "int"
            builder.addText("$type ")
        }
        target.toJava(builder, semantic)
        builder.addText(" = ")
        builder.addText("super(")
        for(i in params.indices){
            params[i].toJava(builder, semantic)
            if(i != params.size -1)
                builder.addText(", ")
        }
        params.forEach { it.toJava(builder, semantic) }
        builder.addText(");\n ")
    }
}

// Method call. We have the ABS-style split between calls and expressions to make the rules more simple
data class CallStmt(val target : Location, val callee : Location, val method : String, val params : List<Expression>, val pos : Int = -1, val declares : Type?) :
    Statement {
    override fun toString(): String = "$target := $callee.$method(${params.joinToString(",")})"
    override fun getRDF(): String {
        var s = """
            prog:stmt${this.hashCode()} rdf:type smol:CallStatement.
            prog:stmt${this.hashCode()} smol:hasTarget prog:loc${target.hashCode()}.
            prog:stmt${this.hashCode()} smol:hasCallee prog:loc${callee.hashCode()}.
            prog:stmt${this.hashCode()} smol:hasMethodName '${method}'.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent()
        for (i in params.indices){
            s += "prog:stmt${this.hashCode()} smol:hasParameter [smol:hasParameterIndex $i ; smol:hasParameterValue prog:expr${params[i].hashCode()}; ].\n"
            s += params[i].getRDF()
        }
        return s + target.getRDF() + callee.getRDF()
    }

    override fun toJava(builder: JavaBackend, semantic: Boolean) {
        builder.addIntend()
        if(declares != null) {
            var type = declares.toString()
            if(type == "Int") type = "int"
            builder.addText("$type ")
        }
        target.toJava(builder, semantic)
        builder.addText(" = ")
        callee.toJava(builder, semantic)
        builder.addText(".$method(")
        for(i in params.indices){
            params[i].toJava(builder, semantic)
            if(i != params.size -1)
                builder.addText(", ")
        }
        params.forEach { it.toJava(builder, semantic) }
        builder.addText(");\n ")
    }
}

// Object creation. There is no constructor, but we
data class CreateStmt(val target : Location, val className: String, val params : List<Expression>, val pos : Int = -1, val declares: Type?, val modeling : Expression? = null) : Statement {
    override fun toString(): String = "$target := new $className(${params.joinToString(",")})"
    override fun getRDF(): String {
        var s = """
            prog:stmt${this.hashCode()} rdf:type smol:CreateStatement.
            prog:stmt${this.hashCode()} smol:hasTarget prog:loc${target.hashCode()}.
            prog:stmt${this.hashCode()} smol:hasClassName '${className}'.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent()
        for (i in params.indices){
            s += "prog:stmt${this.hashCode()} smol:hasParameter [smol:hasParameterIndex $i ; smol:hasParameterValue prog:expr${params[i].hashCode()}; ].\n"
            s += params[i].getRDF()
        }
        return s + target.getRDF()
    }

    override fun toJava(builder: JavaBackend, semantic: Boolean) {
        builder.addIntend()
        if(declares != null) {
            var type = declares.toString()
            if(type == "Int") type = "int"
            builder.addText("$type ")
        }
        target.toJava(builder, semantic)
        builder.addText(" = new ")
        builder.addText("$className(")
        for(i in params.indices){
            params[i].toJava(builder, semantic)
            if(i != params.size -1)
                builder.addText(", ")
        }
        builder.addText(");\n ")
    }
}

// Return statement
data class ReturnStmt(var value : Expression, val pos : Int = -1) : Statement {
    override fun toString(): String = "return $value"
    override fun getRDF(): String {
        return """
            prog:stmt${this.hashCode()} rdf:type smol:ReturnStatement.
            prog:stmt${this.hashCode()} smol:hasValue prog:expr${value.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent() + value.getRDF()
    }

    override fun toJava(builder: JavaBackend, semantic: Boolean) {
        builder.addIntend()
        builder.addText("return ")
        value.toJava(builder, semantic)
        builder.addText(";\n")
    }
}

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
}

// Standard control flow
data class IfStmt(val guard : Expression, val thenBranch : Statement, val elseBranch : Statement, val pos : Int = -1) : Statement {
    override fun toString(): String = "if($guard) then $thenBranch else $elseBranch fi"
    override fun getRDF(): String {
        return """
            prog:stmt${this.hashCode()} rdf:type smol:IfStatement.
            prog:stmt${this.hashCode()} smol:hasGuard prog:expr${guard.hashCode()}.
            prog:stmt${this.hashCode()} smol:hasThenBranch prog:stmt${thenBranch.hashCode()}.
            prog:stmt${this.hashCode()} smol:hasElseBranch prog:stmt${elseBranch.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent() + guard.getRDF() + thenBranch.getRDF() + elseBranch.getRDF()
    }

    override fun toJava(builder: JavaBackend, semantic: Boolean) {
        builder.addIntend()
        builder.addText("if(")
        guard.toJava(builder, semantic)
        builder.addText("){")
        builder.offsetDepth(1)
        thenBranch.toJava(builder, semantic)
        builder.offsetDepth(-1)
        builder.addText("} else {")
        builder.offsetDepth(1)
        elseBranch.toJava(builder, semantic)
        builder.offsetDepth(-1)
        builder.addText("}")
    }
}

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

    override fun toJava(builder: JavaBackend, semantic: Boolean) {
        builder.addIntend()
        builder.addText("while(")
        guard.toJava(builder, semantic)
        builder.addText("){")
        builder.offsetDepth(1)
        loopBody.toJava(builder, semantic)
        builder.offsetDepth(-1)
        builder.addText("}")
    }
}

// We use a binary tree instead of a list to make the interpreter more simple.
// The value of first is NOT allowed to be another SequenceStmt. Use appendStmt below to build trees.
data class SequenceStmt(val first: Statement, val second : Statement) : Statement {
    override fun getLast(): Statement = second
    override fun toString(): String = "$first; $second"
    override fun getRDF(): String {
        return """
            prog:stmt${this.hashCode()} rdf:type smol:SequenceStatement.
            prog:stmt${this.hashCode()} smol:first prog:stmt${first.hashCode()}.
            prog:stmt${this.hashCode()} smol:second prog:stmt${second.hashCode()}.

        """.trimIndent() + first.getRDF() + second.getRDF()
    }

    override fun toJava(builder: JavaBackend, semantic: Boolean) {
        first.toJava(builder, semantic)
        second.toJava(builder, semantic)
    }
}

fun appendStmt (a : Statement, b: Statement) : Statement {
    if(b is SkipStmt) return a
    if(a is SkipStmt) return b
    return if(a !is SequenceStmt) SequenceStmt(a, b) else SequenceStmt(a.first, appendStmt(a.second,b))
}


data class DestroyStmt(val expr: Expression, val pos : Int = -1): Statement {
    override fun toString(): String = "destroy($expr)"
    override fun getRDF(): String {
        return """
            prog:stmt${this.hashCode()} rdf:type smol:DestroyStatement.
            prog:stmt${this.hashCode()} smol:hasStmtExpr prog:expr${expr.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent() + expr.getRDF()
    }
    override fun toJava(builder: JavaBackend, semantic: Boolean) {
        if(semantic) builder.addText(";/*destroy statement*/")
        else super.toJava(builder, semantic)
    }
}

//for output
data class PrintStmt(val expr: Expression, val pos : Int = -1): Statement {
    override fun toString(): String = "println($expr)"
    override fun getRDF(): String {
        return """
            prog:stmt${this.hashCode()} rdf:type smol:PrintStatement.
            prog:stmt${this.hashCode()} smol:hasStmtExpr prog:expr${expr.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent() + expr.getRDF()
    }

    override fun toJava(builder: JavaBackend, semantic: Boolean) {
        builder.addIntend()
        builder.addText("System.out.println(")
        expr.toJava(builder, semantic)
        builder.addText(");")
    }
}


// For ontology-based reflexion
data class AccessStmt(val target : Location, val query: Expression, val params : List<Expression>, val pos : Int = -1, val mode : AccessMode = SparqlMode, val declares: Type?) : Statement {
    override fun toString(): String = "$target := access($query, ${params.joinToString(",")})"
    override fun getRDF(): String {
        var s = """
            prog:stmt${this.hashCode()} rdf:type smol:AccessStatement.
            prog:stmt${this.hashCode()} smol:hasTarget prog:loc${target.hashCode()}.
            prog:stmt${this.hashCode()} smol:hasQuery prog:expr${query.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent()
        for (i in params.indices){
            s += "prog:stmt${this.hashCode()} smol:hasParameter [smol:hasParameterIndex $i ; smol:hasParameterValue prog:expr${params[i].hashCode()}; ].\n"
            s += params[i].getRDF()
        }
        // return s + target.getRDF()
        return s + target.getRDF() + query.getRDF()
        // '${literal.removePrefix("\"").removeSuffix("\"")}'
    }
}
// For ontology-based reflexion
data class ConstructStmt(val target : Location, val query: Expression, val params : List<Expression>, val pos : Int = -1, val declares: Type?) : Statement {
    override fun toString(): String = "$target := access($query, ${params.joinToString(",")})"
    override fun getRDF(): String {
        var s = """
            prog:stmt${this.hashCode()} rdf:type smol:ConstructStatement.
            prog:stmt${this.hashCode()} smol:hasTarget prog:loc${target.hashCode()}.
            prog:stmt${this.hashCode()} smol:hasQuery prog:expr${query.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent()
        for (i in params.indices){
            s += "prog:stmt${this.hashCode()} smol:hasParameter [smol:hasParameterIndex $i ; smol:hasParameterValue prog:expr${params[i].hashCode()}; ].\n"
            s += params[i].getRDF()
        }
        // return s + target.getRDF()
        return s + target.getRDF() + query.getRDF()
        // '${literal.removePrefix("\"").removeSuffix("\"")}'
    }
}
data class OwlStmt(val target : Location, val query: Expression, val pos : Int = -1, val declares: Type?) : Statement {
    override fun toString(): String = "$target := derive($query)"
    override fun getRDF(): String {
        return """
            prog:stmt${this.hashCode()} rdf:type smol:OwlStatement.
            prog:stmt${this.hashCode()} smol:hasTarget prog:loc${target.hashCode()}.
            prog:stmt${this.hashCode()} smol:hasQuery prog:expr${query.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent() + target.getRDF() + query.getRDF()
    }
}
data class ValidateStmt(val target : Location, val query: Expression, val pos : Int = -1, val declares: Type?) : Statement {
    override fun toString(): String = "$target := validate($query)"
    override fun getRDF(): String {
        return """
            prog:stmt${this.hashCode()} rdf:type smol:ShaclStatement.
            prog:stmt${this.hashCode()} smol:hasTarget prog:loc${target.hashCode()}.
            prog:stmt${this.hashCode()} smol:hasQuery prog:expr${query.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent() + target.getRDF() + query.getRDF()
    }
}

// For simulation interface
data class SimulationStmt(val target : Location, val path: String, val params : List<VarInit>, val pos : Int = -1, val declares: Type?) : Statement {
    override fun toString(): String = "$target := simulate($path, ${params.joinToString(",")})"
    override fun getRDF(): String {
        //TODO: extend ontology
        return ""
    }
}

data class TickStmt(val fmu: Expression, val tick : Expression, val pos : Int = -1) : Statement {
    override fun toString(): String = "tick($fmu, $tick)"
    override fun getRDF(): String {
        //TODO: extend ontology
        return ""
    }
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

    override fun toJava(builder: JavaBackend, semantic: Boolean) {
        builder.addText(name)
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
    override fun toJava(builder: JavaBackend, semantic: Boolean) {
        builder.addText("this.$name")
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
    override fun toJava(builder: JavaBackend, semantic: Boolean) {
        expr.toJava(builder, semantic)
        builder.addText(".$name")
    }
}


data class ArithExpr(val Op : Operator, val params: List<Expression>, val tag : Type = ERRORTYPE) : Expression {
    override fun toString(): String = "($Op ${params.joinToString(" ")})"
    override fun getRDF(): String {
        var s = """
            prog:expr${this.hashCode()} rdf:type smol:ArithExpression.
            prog:expr${this.hashCode()} smol:hasOp $Op.

        """.trimIndent()
        for (i in params.indices){
            s += "prog:expr${this.hashCode()} smol:hasOperand [smol:hasOperandIndex $i ; smol:hasOperandValue prog:expr${params[i].hashCode()}; ].\n"
            s += params[i].getRDF()
        }
        return s
    }

    override fun toJava(builder: JavaBackend, semantic: Boolean) {
        when(params.size){
            1 -> {
                builder.addText(Operator.toJava(Op)+"(")
                params.first().toJava(builder, semantic)
                builder.addText(")")
            }
            2 -> {
                builder.addText("(")
                params[0].toJava(builder, semantic)
                builder.addText(")"+Operator.toJava(Op)+"(")
                params[1].toJava(builder, semantic)
                builder.addText(")")
            }
            else -> throw Exception("only unary and binary operators are support in the java backend")
        }
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

    override fun toJava(builder: JavaBackend, semantic: Boolean) {
        builder.addText(literal)
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
