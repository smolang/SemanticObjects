@file:Suppress("unused")

package no.uio.microobject.ast

import no.uio.microobject.ast.expr.FALSEEXPR
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.ast.expr.LocalVar
import no.uio.microobject.ast.expr.TRUEEXPR
import no.uio.microobject.ast.stmt.SequenceStmt
import no.uio.microobject.ast.stmt.SkipStmt
import no.uio.microobject.runtime.*
import no.uio.microobject.type.*


/* Top-level data structures for the AST, the implementations are in their own files in data.expr.* and data.stmt.* */

interface ProgramElement{
    fun getRDF() : String
}


interface Statement : ProgramElement {
    fun getLast(): Statement = this
    fun eval(heapObj: Memory, stackFrame : StackEntry, interpreter: Interpreter) : EvalResult

    fun replaceStmt(stmt: Statement, stackFrame : StackEntry) : EvalResult
      = EvalResult(StackEntry(stmt, stackFrame.store, stackFrame.obj, stackFrame.id), listOf())
}

interface Expression : ProgramElement {
    fun eval(stack: Memory, heap: GlobalMemory, simMemory: SimulationMemory, obj: LiteralExpr): LiteralExpr
}

interface Location : Expression {
    fun getType() : Type
    fun setType(targetType: Type)
}

/* This is a special construct for the simulate statement that assigns to the input variables/parameters */
data class VarInit(val name : String, val expr: Expression) : ProgramElement {
    override fun toString(): String = "$name : $expr"
    override fun getRDF(): String {
        //TODO: extend ontology
        return ""
    }

}

/* Access modes for different backends for `access`. May be refactored into their own statements. */
abstract class AccessMode
data class InfluxDBMode(val config : String) : AccessMode()
object SparqlMode : AccessMode()


/* helpers */
fun evalBool(bool : Boolean) : LiteralExpr=  if (bool) TRUEEXPR else FALSEEXPR

fun appendStmt (a : Statement, b: Statement) : Statement {
    if(b is SkipStmt) return a
    if(a is SkipStmt) return b
    return if(a !is SequenceStmt) SequenceStmt(a, b) else SequenceStmt(a.first, appendStmt(a.second,b))
}

/* Handles generation of fresh names*/
object Names{
    private var i = 0
    private var j = 0
    private var k = 0
    fun getObjName(className : String) : LiteralExpr = LiteralExpr("obj${i++}", BaseType(className))
    fun getVarName(tag : Type = ERRORTYPE) : LocalVar = LocalVar("_v${i++}", tag)
    fun getStackId() : Int = j++
    fun getNodeName() : String = "domain:model${k++}"
}
