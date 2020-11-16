@file:Suppress("unused")

package microobject.data

/*
 * Simple extensions:
 *  - Allows calls and creations within expressions
 *  - Constructors and internal fields
 *  - Encapsulation (private/public)
 *  - More operators
 */

enum class Operator {
    PLUS, MINUS, NEQ, GEQ, EQ
}

interface ProgramElement
interface Statement : ProgramElement
interface Expression : ProgramElement
interface Location : Expression

/** Statement **/

// Empty statement. Handy for unrolling of loops.
object SkipStmt : Statement{
    override fun toString(): String = "skip"
}


// Stops automatic execution
object DebugStmt : Statement{
    override fun toString(): String = "breakpoint"
}

// Assignment, where value cannot refer to calls or object creations.
data class AssignStmt(val target : Location, val value : Expression) : Statement {
    override fun toString(): String = "$target := $value"
}

// Method call. We have the ABS-style split between calls and expressions to make the rules more simple
data class CallStmt(val target : Location, val callee : Location, val method : String, val params : List<Expression>) :
    Statement {
    override fun toString(): String = "$target := $callee.$method(${params.joinToString(",")})"
}

// Object creation. There is no constructor, but we
data class CreateStmt(val target : Location, val className: String, val params : List<Expression>) : Statement {
    override fun toString(): String = "$target := new $className(${params.joinToString(",")})"
}

// Return statement
data class ReturnStmt(var value : Expression) : Statement {
    override fun toString(): String = "return $value"
}

// This is a microobject.runtime-syntax only statement which models that we will write the return value of the next method in the stack into target
data class StoreReturnStmt(val target : Location) : Statement {
    override fun toString(): String = "$target <- stack"
}

// Standard control flow
data class IfStmt(val guard : Expression, val thenBranch : Statement, val elseBranch : Statement) : Statement {
    override fun toString(): String = "if($guard) then $thenBranch else $elseBranch fi"
}

data class WhileStmt(val guard : Expression, val loopBody : Statement) : Statement {
    override fun toString(): String = "while($guard) do $loopBody od"
}

// We use a binary tree instead of a list to make the interpreter more simple.
// The value of first is NOT allowed to be another SequenceStmt. Use appendStmt below to build trees.
data class SequenceStmt(val first: Statement, val second : Statement) : Statement {
    override fun toString(): String = "$first; $second"
}

fun appendStmt (a : Statement, b: Statement) : Statement {
    if(b == SkipStmt) return a
    if(a == SkipStmt) return b
    return if(a !is SequenceStmt) SequenceStmt(a, b) else SequenceStmt(a.first, appendStmt(a.second,b))
}

//for output
data class PrintStmt(val expr: Expression): Statement {
    override fun toString(): String = "println($expr)"
}


// For ontology-based reflexion
data class SparqlStmt(val target : Location, val query: Expression, val params : List<Expression>) : Statement {
    override fun toString(): String = "$target := access($query, ${params.joinToString(",")})"
}
data class OwlStmt(val target : Location, val query: Expression) : Statement {
    override fun toString(): String = "$target := derive($query)"
}



/** Expressions **/


data class LocalVar(val name : String) : Location { // local variable
    override fun toString(): String = name
}
data class OwnVar(val name : String) : Location {   // field of own object
    override fun toString(): String = "this.$name"
}
data class OthersVar(val expr: Expression, val name : String) : Location { // field of (possibly) other object
    override fun toString(): String = "$expr.$name"
}

data class ArithExpr(val Op : Operator, val params: List<Expression>) : Expression {
    override fun toString(): String = "($Op ${params.joinToString(" ")})"
}
data class LiteralExpr(val literal : String, val tag : String = "IGNORED") : Expression {
    override fun toString(): String = literal
}

// Use this whenever we need a new unique name
object Names{
    private var i = 0
    private var j = 0
    fun getObjName(className : String) : LiteralExpr = LiteralExpr("obj${i++}", className)
    fun getVarName() : LocalVar = LocalVar("_v${i++}")
    fun getStackId() : Int = j++
}