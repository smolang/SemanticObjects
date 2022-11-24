package no.uio.microobject.ast.stmt

import no.uio.microobject.ast.*
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry
import no.uio.microobject.type.STRINGTYPE
import no.uio.microobject.type.Type

// Object creation. There is no constructor, but we
data class CreateStmt(val target : Location, val className: String, val params : List<Expression>, val pos : Int = -1, val declares: Type?, val modeling : Expression? = null) :
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
        val m =
            interpreter.staticInfo.fieldTable[className] ?: throw Exception("This class is unknown: $className")
        val newMemory: Memory = mutableMapOf()
        if (m.size != params.size) throw Exception(
            "Creation of an instance of class $className failed, mismatched number of parameters: $this. Requires: ${m.size}"
        )
        for (i in m.indices) {
            if(!m[i].name.startsWith("__"))
                newMemory[m[i].name] = interpreter.eval(params[i], stackFrame)
        }
        if(modeling != null) {
            val str = interpreter.eval(modeling, stackFrame).literal
            val rdfName = Names.getNodeName()
            newMemory["__describe"] = LiteralExpr(rdfName + " " + str.removeSurrounding("\""), STRINGTYPE)
            newMemory["__models"] = LiteralExpr(rdfName, STRINGTYPE)
        }
        interpreter.heap[name] = newMemory
        return replaceStmt(AssignStmt(target, name, declares = declares), stackFrame)
    }
}