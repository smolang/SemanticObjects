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
    PLUS, MINUS, NEQ, GEQ, EQ, LEQ
}

interface ProgramElement{
    fun getRDF() : String
}


interface Statement : ProgramElement
interface Expression : ProgramElement
interface Location : Expression

/** Statement **/

// Empty statement. Handy for unrolling of loops.
data class SkipStmt(val pos : Int = -1) : Statement{
    override fun toString(): String = "skip"
    override fun getRDF(): String = ":stmt${this.hashCode()} rdf:type :MOXSkipStatement.\n:stmt${this.hashCode()} :MOLine '$pos'^^xsd:integer.\n"
}


// Stops automatic execution
data class DebugStmt(val pos : Int = -1) : Statement{
    override fun toString(): String = "breakpoint"
    override fun getRDF(): String = ":stmt${this.hashCode()} rdf:type :MOXDebugStatement.\n:stmt${this.hashCode()} :MOLine '$pos'^^xsd:integer.\n"
}

// Assignment, where value cannot refer to calls or object creations.
data class AssignStmt(val target : Location, val value : Expression, val pos : Int = -1) : Statement {
    override fun toString(): String = "$target := $value"
    override fun getRDF(): String {
        return """
            :stmt${this.hashCode()} rdf:type :MOXAssignStatement.
            :stmt${this.hashCode()} :MOhasTarget :loc${target.hashCode()}.
            :stmt${this.hashCode()} :MOhasValue :expr${value.hashCode()}.
            :stmt${this.hashCode()} :MOLine '$pos'^^xsd:integer.

        """.trimIndent()
    }
}

// Method call. We have the ABS-style split between calls and expressions to make the rules more simple
data class CallStmt(val target : Location, val callee : Location, val method : String, val params : List<Expression>, val pos : Int = -1) :
    Statement {
    override fun toString(): String = "$target := $callee.$method(${params.joinToString(",")})"
    override fun getRDF(): String {
        var s = """
            :stmt${this.hashCode()} rdf:type :MOXCallStatement.
            :stmt${this.hashCode()} :MOhasTarget :loc${target.hashCode()}.
            :stmt${this.hashCode()} :MOhasCallee :loc${callee.hashCode()}.
            :stmt${this.hashCode()} :MOhasMethodName '${method}'.
            :stmt${this.hashCode()} :MOLine '$pos'^^xsd:integer.

        """.trimIndent()
        for (i in params.indices){
            s += ":stmt${this.hashCode()} :MOhasParameter [:MOhasParameterIndex $i ; :MOhasParameterValue :expr${params[i].hashCode()}; ].\n"
            s += params[i].getRDF()
        }
        return s + target.getRDF() + callee.getRDF()
    }
}

// Object creation. There is no constructor, but we
data class CreateStmt(val target : Location, val className: String, val params : List<Expression>, val pos : Int = -1) : Statement {
    override fun toString(): String = "$target := new $className(${params.joinToString(",")})"
    override fun getRDF(): String {
        var s = """
            :stmt${this.hashCode()} rdf:type :MOXCreateStatement.
            :stmt${this.hashCode()} :MOhasTarget :loc${target.hashCode()}.
            :stmt${this.hashCode()} :MOhasClassName '${className}'.
            :stmt${this.hashCode()} :MOLine '$pos'^^xsd:integer.

        """.trimIndent()
        for (i in params.indices){
            s += ":stmt${this.hashCode()} :MOhasParameter [:MOhasParameterIndex $i ; :MOhasParameterValue :expr${params[i].hashCode()}; ].\n"
            s += params[i].getRDF()
        }
        return s + target.getRDF()
    }
}

// Return statement
data class ReturnStmt(var value : Expression, val pos : Int = -1) : Statement {
    override fun toString(): String = "return $value"
    override fun getRDF(): String {
        return """
            :stmt${this.hashCode()} rdf:type :MOXReturnStatement.
            :stmt${this.hashCode()} :MOhasValue :expr${value.hashCode()}.
            :stmt${this.hashCode()} :MOLine '$pos'^^xsd:integer.

        """.trimIndent() + value.getRDF()
    }
}

// This is a microobject.runtime-syntax only statement which models that we will write the return value of the next method in the stack into target
data class StoreReturnStmt(val target : Location, val pos : Int = -1) : Statement {
    override fun toString(): String = "$target <- stack"
    override fun getRDF(): String {
        return """
            :stmt${this.hashCode()} rdf:type :MOXStoreReturnStatement.
            :stmt${this.hashCode()} :MOhasTarget :loc${target.hashCode()}.
            :stmt${this.hashCode()} :MOLine '$pos'^^xsd:integer.

        """.trimIndent() + target.getRDF()
    }
}

// Standard control flow
data class IfStmt(val guard : Expression, val thenBranch : Statement, val elseBranch : Statement, val pos : Int = -1) : Statement {
    override fun toString(): String = "if($guard) then $thenBranch else $elseBranch fi"
    override fun getRDF(): String {
        return """
            :stmt${this.hashCode()} rdf:type :MOXIfStatement.
            :stmt${this.hashCode()} :MOhasGuard :expr${guard.hashCode()}.
            :stmt${this.hashCode()} :MOhasThenBranch :stmt${thenBranch.hashCode()}.
            :stmt${this.hashCode()} :MOhasElseBranch :stmt${elseBranch.hashCode()}.
            :stmt${this.hashCode()} :MOLine '$pos'^^xsd:integer.

        """.trimIndent() + guard.getRDF() + thenBranch.getRDF() + elseBranch.getRDF()
    }
}

data class WhileStmt(val guard : Expression, val loopBody : Statement, val pos : Int = -1) : Statement {
    override fun toString(): String = "while $guard do $loopBody end"
    override fun getRDF(): String {
        return """
            :stmt${this.hashCode()} rdf:type :MOXWhileStatement.
            :stmt${this.hashCode()} :MOhasGuard :expr${guard.hashCode()}.
            :stmt${this.hashCode()} :MOhasLoopBody :stmt${loopBody.hashCode()}.
            :stmt${this.hashCode()} :MOLine '$pos'^^xsd:integer.

        """.trimIndent() + guard.getRDF() + loopBody.getRDF()
    }
}

// We use a binary tree instead of a list to make the interpreter more simple.
// The value of first is NOT allowed to be another SequenceStmt. Use appendStmt below to build trees.
data class SequenceStmt(val first: Statement, val second : Statement) : Statement {
    override fun toString(): String = "$first; $second"
    override fun getRDF(): String {
        return """
            :stmt${this.hashCode()} rdf:type :MOXSequenceStatement.
            :stmt${this.hashCode()} :MOfirst :stmt${first.hashCode()}.
            :stmt${this.hashCode()} :MOsecond :stmt${second.hashCode()}.

        """.trimIndent() + first.getRDF() + second.getRDF()
    }
}

fun appendStmt (a : Statement, b: Statement) : Statement {
    if(b is SkipStmt) return a
    if(a is SkipStmt) return b
    return if(a !is SequenceStmt) SequenceStmt(a, b) else SequenceStmt(a.first, appendStmt(a.second,b))
}

//for output
data class PrintStmt(val expr: Expression, val pos : Int = -1): Statement {
    override fun toString(): String = "println($expr)"
    override fun getRDF(): String {
        return """
            :stmt${this.hashCode()} rdf:type :MOXPrintStatement.
            :stmt${this.hashCode()} :MOhasStmtExpr :expr${expr.hashCode()}.
            :stmt${this.hashCode()} :MOLine '$pos'^^xsd:integer.

        """.trimIndent() + expr.getRDF()
    }
}


// For ontology-based reflexion
data class SparqlStmt(val target : Location, val query: Expression, val params : List<Expression>, val pos : Int = -1) : Statement {
    override fun toString(): String = "$target := access($query, ${params.joinToString(",")})"
    override fun getRDF(): String {
        var s = """
            :stmt${this.hashCode()} rdf:type :MOXSparqlStatement.
            :stmt${this.hashCode()} :MOhasTarget :loc${target.hashCode()}.
            :stmt${this.hashCode()} :MOhasQuery :expr${query.hashCode()}.
            :stmt${this.hashCode()} :MOLine '$pos'^^xsd:integer.

        """.trimIndent()
        for (i in params.indices){
            s += ":stmt${this.hashCode()} :MOhasParameter [:MOhasParameterIndex $i ; :MOhasParameterValue :expr${params[i].hashCode()}; ].\n"
            s += params[i].getRDF()
        }
        // return s + target.getRDF()
        return s + target.getRDF() + query.getRDF()
        // '${literal.removePrefix("\"").removeSuffix("\"")}'
    }
}
data class OwlStmt(val target : Location, val query: Expression, val pos : Int = -1) : Statement {
    override fun toString(): String = "$target := derive($query)"
    override fun getRDF(): String {
        return """
            :stmt${this.hashCode()} rdf:type :MOXOwlStatement.
            :stmt${this.hashCode()} :MOhasTarget :loc${target.hashCode()}.
            :stmt${this.hashCode()} :MOhasQuery :expr${query.hashCode()}.
            :stmt${this.hashCode()} :MOLine '$pos'^^xsd:integer.

        """.trimIndent() + target.getRDF() + query.getRDF()
    }
}



/** Expressions **/


data class LocalVar(val name : String) : Location { // local variable
    override fun toString(): String = name
    override fun getRDF(): String {
        return """
            :loc${this.hashCode()} rdf:type :MOXLocalVarLocation.
            :loc${this.hashCode()} :MOhasName '${name}'.

        """.trimIndent()
    }
}
data class OwnVar(val name : String) : Location {   // field of own object
    override fun toString(): String = "this.$name"
    override fun getRDF(): String {
        return """
            :loc${this.hashCode()} rdf:type :MOXOwnVarLocation.
            :loc${this.hashCode()} :MOhasName '${name}'.

        """.trimIndent()
    }
}
data class OthersVar(val expr: Expression, val name : String) : Location { // field of (possibly) other object
    override fun toString(): String = "$expr.$name"
    override fun getRDF(): String {
        return """
            :loc${this.hashCode()} rdf:type :MOXOthersVarLocation.
            :loc${this.hashCode()} :MOhasExpr :expr${expr.hashCode()}.
            :loc${this.hashCode()} :MOhasName '${name}'.

        """.trimIndent()
    }
}


data class ArithExpr(val Op : Operator, val params: List<Expression>) : Expression {
    override fun toString(): String = "($Op ${params.joinToString(" ")})"
    override fun getRDF(): String {
        var s = """
            :expr${this.hashCode()} rdf:type :MOXArithExpression.
            :expr${this.hashCode()} :MOhasOp $Op.

        """.trimIndent()
        for (i in params.indices){
            s += ":expr${this.hashCode()} :MOhasOperand [:MOhasOperandIndex $i ; :MOhasOperandValue :expr${params[i].hashCode()}; ].\n"
            s += params[i].getRDF()
        }
        return s
    }



}
data class LiteralExpr(val literal : String, val tag : String = "IGNORED") : Expression {
    override fun toString(): String = literal
    override fun getRDF(): String {
        return """
            :expr${this.hashCode()} rdf:type :MOXLiteralExpression.
            :expr${this.hashCode()} :MOhasLiteral '${literal.removePrefix("\"").removeSuffix("\"")}'.
            :expr${this.hashCode()} :MOhasTag '${tag}'.

        """.trimIndent()
    }
}

// Use this whenever we need a new unique name
object Names{
    private var i = 0
    private var j = 0
    fun getObjName(className : String) : LiteralExpr = LiteralExpr("obj${i++}", className)
    fun getVarName() : LocalVar = LocalVar("_v${i++}")
    fun getStackId() : Int = j++
}