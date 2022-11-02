package no.uio.microobject.data.stmt

import no.uio.microobject.data.*
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry
import no.uio.microobject.type.BaseType
import no.uio.microobject.type.Type

// Assignment, where value cannot refer to calls or object creations.
data class AssignStmt(val target : Location, val value : Expression, val pos : Int = -1, val declares: Type?) :
    Statement {
    override fun toString(): String = "$target := $value"
    override fun getRDF(): String {
        return """
            prog:stmt${this.hashCode()} rdf:type smol:AssignStatement.
            prog:stmt${this.hashCode()} smol:hasTarget prog:loc${target.hashCode()}.
            prog:stmt${this.hashCode()} smol:hasValue prog:expr${value.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent()
    }

    override fun eval(heapObj: Memory, stackFrame : StackEntry, interpreter: Interpreter) : EvalResult {
        val res = interpreter.eval(value, stackFrame)
        when (target) {
            is LocalVar -> stackFrame.store[target.name] = res
            is OwnVar -> {
                val got = interpreter.staticInfo.fieldTable[(stackFrame.obj.tag as BaseType).name] ?: throw Exception("Cannot find class ${stackFrame.obj.tag.name}")
                if (!got.map {it.name} .contains(target.name))
                    throw Exception("This field is unknown: ${target.name}")
                heapObj[target.name] = res
            }
            is OthersVar -> {
                val key = interpreter.eval(target.expr, stackFrame)

                when {
                    interpreter.heap.containsKey(key) -> {
                        val otherHeap = interpreter.heap[key]
                            ?: throw Exception("This object is unknown: $key")
                        if (!(interpreter.staticInfo.fieldTable[(key.tag as BaseType).name]
                                ?: error("")).any{ it.name == target.name}
                        ) throw Exception("This field is unknown: $key")
                        otherHeap[target.name] = res
                    }
                    interpreter.simMemory.containsKey(key) -> {
                        interpreter.simMemory[key]!!.write(target.name, res)
                    }
                    else -> throw Exception("This object is unknown: $key")
                }
            }
        }
        return EvalResult(null, emptyList())
    }
}