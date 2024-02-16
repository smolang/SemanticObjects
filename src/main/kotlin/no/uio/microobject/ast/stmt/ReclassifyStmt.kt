package no.uio.microobject.ast.stmt

import no.uio.microobject.ast.Statement
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry

data class ReclassifyStmt(val className: String, val staticTable: MutableMap<String, String>) : Statement {
    override fun toString(): String = "Reclassify to a $className"

    override fun getRDF(): String {
        return "prog:stmt${this.hashCode()} rdf:type smol:ReclassifyStatement.\n"
    }

    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        for ((key, value) in staticTable) {
            // Check if key is a subclass of className
            if (isSubclassOf(key, className)) {
                // Perform the query associated with key
                val results = interpreter.query(value.removePrefix("\"").removeSuffix("\""))
                val queryResult = interpreter.query(value)

                // If the query returns true, return the class associated with key
                if (queryResult != null) {
                    return EvalResult(key)
                }
            }
        }

        // If no subclass of className is found, return className
        return EvalResult(className)
    }

    private fun isSubclassOf(subclass: String, superclass: String): Boolean {
        return true
    }
}