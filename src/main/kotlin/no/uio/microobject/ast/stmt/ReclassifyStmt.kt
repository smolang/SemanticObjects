package no.uio.microobject.ast.stmt

import no.uio.microobject.ast.*
import no.uio.microobject.ast.Expression
import no.uio.microobject.ast.Names
import no.uio.microobject.ast.Statement
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry
import no.uio.microobject.type.BaseType
import no.uio.microobject.type.Type

/**
 * ReclassifyStmt is a statement that reclassifies an object to a new class
 *
 * The statement is used to reclassify an object to a new class. To do this, the old state of the object is used to
 * determine the new class. The new class is determined by a static table that contains the class name and a query to
 * check which state is the new one. The queries are executed only if the old class is a subclass of className.
 * If the query returns some useful data (either true or a result), the object is reclassified to the new class.
 *
 * @property target The target location to reclassify
 * @property oldState The old state of the object
 * @property className The superclass name. This is needed to check that the reclassification is valid for subclasses
 * @property staticTable The static table containing the class name and the query to check which state is the new one
 * @property declares The type of the object
 */
data class ReclassifyStmt(val target: Location, val containerObject: Expression, val className: String, val staticTable: MutableMap<String, String>, val declares: Type?) : Statement {
    override fun toString(): String = "Reclassify to a $className"

    override fun getRDF(): String {
        return "prog:stmt${this.hashCode()} rdf:type smol:ReclassifyStatement.\n"
    }

    /**
     * Evaluates the reclassification statement
     *
     * This is done by checking the static table for the new
     * class and checking if the old class is a subclass of className. If it is, the query for the subclasses are checked
     * and if the query returns some useful data (either true or a result), the object is reclassified to the new class.
     * If the query returns nothing, the object gets reclassified to the superclass defined by className.
     *
     * @param heapObj The memory of the heap
     * @param stackFrame The current stack frame
     * @param interpreter The interpreter
     * @return The result of the evaluation
     * @throws Exception If the class is unknown
     * @throws Exception If the query type is invalid
     * @throws Exception If no valid subclass is found for className
     */
    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        val name = Names.getObjName(target.toString())
        val n =
            interpreter.staticInfo.fieldTable[className] ?: throw Exception("This class is unknown: $className")

        val newMemory: Memory = mutableMapOf()

        val t = interpreter.eval(target, stackFrame)
        val e = interpreter.eval(containerObject, stackFrame)

        for ((key, value) in staticTable) {
            // Check if key is a subclass of className
            if (isSubclassOf(key, className.toString())) {
                val id: LiteralExpr = LiteralExpr(t.literal, BaseType(t.literal))
                val superId: LiteralExpr = LiteralExpr(e.literal, BaseType(e.literal))

                /*
                 * Change the %this to the actual literal mapped into the memory
                 * Also, allow for the usage of rdfs:subClassOf* with %parent mapping it to prog
                 */
                val query = value
                    .removePrefix("\"")
                    .removeSuffix("\"")
                    .replace("%this", "run:${id.literal}")
                    .replace("%super", "run:${superId.literal}")
                    .replace("%parent", "prog:${className.toString()}")

                if (query.startsWith("ASK") || query.startsWith("ask")){
                    val queryResult = interpreter.ask(query)

                    if (queryResult) {
                        return replaceStmt(CreateStmt(target, key, listOf(), declares = declares, modeling = listOf()), stackFrame)
                    }
                } else if (query.startsWith("SELECT") || query.startsWith("select")) {
                    val queryResult = interpreter.query(query)

                    if (queryResult != null && queryResult.hasNext()) {
                        return replaceStmt(CreateStmt(target, key, listOf(), declares = declares, modeling = listOf()), stackFrame)
                    }
                } else {
                    throw Exception("Invalid query type: use ASK or SELECT")
                }
            }
        }

        throw Exception("No valid subclass found for $className")

//        return replaceStmt(CreateStmt(target, className, listOf(), declares = declares, modeling = listOf()), stackFrame)
//        return replaceStmt(AssignStmt(target, name, declares = declares), stackFrame)
    }


    private fun isSubclassOf(subclass: String, superclass: String): Boolean {
        return true
    }
}