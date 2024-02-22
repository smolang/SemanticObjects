package no.uio.microobject.ast.stmt

import no.uio.microobject.ast.*
import no.uio.microobject.ast.Expression
import no.uio.microobject.ast.Names
import no.uio.microobject.ast.Statement
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.data.TripleManager
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry
import no.uio.microobject.type.BaseType
import no.uio.microobject.type.STRINGTYPE
import no.uio.microobject.type.Type

data class ReclassifyStmt(val target: Location, val oldState: Expression, val className: String, val staticTable: MutableMap<String, String>, val declares: Type?) : Statement {
    override fun toString(): String = "Reclassify to a $className"

    override fun getRDF(): String {
        return "prog:stmt${this.hashCode()} rdf:type smol:ReclassifyStatement.\n"
    }

    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        val name = Names.getObjName(className)
        val n =
            interpreter.staticInfo.fieldTable[className] ?: throw Exception("This class is unknown: $className")

        val newMemory: Memory = mutableMapOf()

        val e = interpreter.eval(oldState, stackFrame)

        for ((key, value) in staticTable) {
            // Check if key is a subclass of className
            if (isSubclassOf(key, className.toString())) {
                val id: LiteralExpr = LiteralExpr(e.literal, BaseType(e.literal))
//                val runPrefix = interpreter.tripleManager.getPrefixMap()["run"]

                /*
                 * Change the %this to the actual literal mapped into the memory
                 * Also, allow for the usage of rdfs:subClassOf* with %parent mapping it to prog
                 */
                val query = value
                    .removePrefix("\"")
                    .removeSuffix("\"")
                    .replace("%this", "run:${id.literal}")
                    .replace("%parent", "prog:${className.toString()}")

                if (query.startsWith("ASK") || query.startsWith("ask")){
                    val queryResult = interpreter.ask(query)

                    if (queryResult) {
                        return replaceStmt(CreateStmt(target, key, listOf(), declares = declares, modeling = listOf()), stackFrame)
                    }
                } else if (query.startsWith("SELECT") || query.startsWith("select")) {
                    val queryResult = interpreter.query(query)

                    if (queryResult != null) {
                        return replaceStmt(CreateStmt(target, key, listOf(), declares = declares, modeling = listOf()), stackFrame)
                    }
                } else {
                    throw Exception("Invalid query type")
                }
            }
        }

        return replaceStmt(CreateStmt(target, className, listOf(), declares = declares, modeling = listOf()), stackFrame)
    }

    private fun isSubclassOf(subclass: String, superclass: String): Boolean {
        return true
    }
}