package no.uio.microobject.data.stmt

import no.uio.microobject.data.*
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry
import no.uio.microobject.type.BaseType
import no.uio.microobject.type.Type

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

    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        if(stackFrame.obj.tag !is BaseType) throw Exception("This object is unknown: ${stackFrame.obj}")
        val m = interpreter.staticInfo.getSuperMethod(stackFrame.obj.tag.name, methodName) ?: throw Exception("super call impossible, no super method found.")
        val newMemory: Memory = mutableMapOf()
        newMemory["this"] = stackFrame.obj
        for (i in m.params.indices) {
            newMemory[m.params[i]] = interpreter.eval(params[i], stackFrame)
        }
        return EvalResult(
            StackEntry(StoreReturnStmt(target), stackFrame.store, stackFrame.obj, stackFrame.id),
            listOf(StackEntry(m.stmt, newMemory, stackFrame.obj, Names.getStackId()))
        )
    }
}