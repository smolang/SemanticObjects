package no.uio.microobject.ast.stmt

import no.uio.microobject.ast.*
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.ast.expr.LocalVar
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry
import no.uio.microobject.type.STRINGTYPE
import no.uio.microobject.type.Type

data class CreateStmt(val target : Location, val className: String, val params : List<Expression>, val pos : Int = -1, val declares: Type?, val modeling : List<Expression>) :
    Statement {
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

    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        val name = Names.getObjName(className)
        val n =
            interpreter.staticInfo.fieldTable[className] ?: throw Exception("This class is unknown: $className")
        val m = n.filter { it.internalInit == null }
        val newMemory: Memory = mutableMapOf()
        if (m.size != params.size) throw Exception(
            "Creation of an instance of class $className failed, mismatched number of parameters: $this. Requires: ${m.size}"
        )
        for (i in m.indices) {
            if(!m[i].name.startsWith("__"))
                newMemory[m[i].name] = interpreter.eval(params[i], stackFrame)
        }
        if(modeling.isNotEmpty()) {
            val rdfName = Names.getNodeName()
            newMemory["__models"] = LiteralExpr(rdfName, STRINGTYPE)
            val evals = modeling.map { rdfName + " " + interpreter.eval(it, stackFrame).literal.removeSurrounding("\"") }
            newMemory["__describe"] = LiteralExpr(evals.joinToString(" "), STRINGTYPE)
        }
        interpreter.heap[name] = newMemory
        val localFrame = StackEntry(SkipStmt(), mutableMapOf(Pair("this", name)),name,0)
        n.filter { it.internalInit != null }.forEach { newMemory[it.name] = interpreter.eval(it.internalInit!!, localFrame) }
        return replaceStmt(AssignStmt(target, name, declares = declares), stackFrame)
    }
}