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
import no.uio.microobject.type.*
import org.apache.jena.datatypes.xsd.XSDDatatype

/**
 * ReclassifyStmt is a statement that reclassifies an object to a new class
 *
 * The statement is used to reclassify an object to a new class. To do this, the old state of the object is used to
 * determine the new class. The new class is determined by a static table that contains the class name and a query to
 * check which state is the new one. The queries are executed only if the old class is a subclass of className.
 * If the query returns some useful data (either true or a result), the object is reclassified to the new class.
 *
 * @property target The target location to reclassify
 * @property containerObject The class that contains the object. It can be the same as the target or a superclass
 * @property className The superclass name. This is needed to check that the reclassification is valid for subclasses
 * @property staticTable The static table containing the class name and the query to check which state is the new one
 * @property modelsTable The models table containing the class name and the models for that class
 * @property declares The type of the object
 */
data class ReclassifyStmt(val target: Location, val containerObject: Expression, val className: String, val staticTable: MutableMap<String, String>, val modelsTable: MutableMap<String, String>, val declares: Type?) : Statement {
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
            if (isSubclassOf(key, className.toString(), interpreter)) {
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
                        val models = if(modelsTable.containsKey(key)) modelsTable!![key]
                            ?.let { LiteralExpr(it, STRINGTYPE) } else  null
                        val modeling = if(models != null) listOf(models) else listOf()

                        return replaceStmt(CreateStmt(target, key, listOf(), declares = declares, modeling = modeling), stackFrame)
                    }
                } else if (query.startsWith("SELECT") || query.startsWith("select")) {
                    val queryResult = interpreter.query(query)

                    if (queryResult != null && queryResult.hasNext()) {
                        val result = queryResult.next()

                        // Transform the result to a List<Expression>
                        val params = mutableListOf<Expression>()

                        // Add the parameters to the list
                        result.varNames().forEachRemaining { name ->
                            val variable = if (result.get(name).isLiteral) {
                                val found = result.get(name).toString().removePrefix(interpreter.settings.runPrefix)
                                val objNameCand = if (found.startsWith("\\\"")) found.replace("\\\"", "\"") else found
                                for (ob in interpreter.heap.keys) {
                                    if (ob.literal == objNameCand) {
                                        LiteralExpr(objNameCand, ob.tag)
                                        break
                                    }
                                }
                                if (!newMemory.containsKey("content")) {
                                    if (result.get(name).isLiteral && result.get(name).asNode().literalDatatype == XSDDatatype.XSDstring)
                                        LiteralExpr("\"" + found + "\"", STRINGTYPE)
                                    else if (result.get(name).isLiteral && result.get(name).asNode().literalDatatype == XSDDatatype.XSDinteger)
                                        LiteralExpr(found.split("^^")[0], INTTYPE)
                                    else if (result.get(name).isLiteral && result.get(name).asNode().literalDatatype == XSDDatatype.XSDdouble)
                                        LiteralExpr(found.split("^^")[0], DOUBLETYPE)
                                    else if (result.get(name).isLiteral && result.get(name).asNode().literalDatatype == XSDDatatype.XSDfloat)
                                        LiteralExpr(found.split("^^")[0], DOUBLETYPE)
                                    else if (objNameCand.matches("\\d+".toRegex()) || objNameCand.matches("\\d+\\^\\^http://www.w3.org/2001/XMLSchema#integer".toRegex()))
                                        LiteralExpr(found.split("^^")[0], INTTYPE)
                                    else if (objNameCand.matches("\\d+".toRegex()) || objNameCand.matches("\\d+\\^\\^http://www.w3.org/2001/XMLSchema#int".toRegex()))
                                        LiteralExpr(found.split("^^")[0], INTTYPE)
                                    else if (objNameCand.matches("\\d+.\\d+".toRegex())) LiteralExpr(found, DOUBLETYPE)
                                    else throw Exception("Query returned unknown object/literal: $found")
                                } else {
                                    LiteralExpr(result.get(name).toString(), BaseType(result.get(name).toString()))
                                }
                            } else {
                                LiteralExpr(result.get(name).toString(), BaseType(result.get(name).toString()))
                            }
                            params.add(variable)
                        }

                        val models = if(modelsTable.containsKey(key)) modelsTable!![key]
                            ?.let { LiteralExpr(it, STRINGTYPE) } else  null
                        val modeling = if(models != null) listOf(models) else listOf()

                        return replaceStmt(CreateStmt(target, key, params, declares = declares, modeling = modeling), stackFrame)
                    }
                } else {
                    throw Exception("Invalid query type: use ASK or SELECT")
                }
            }
        }

        throw Exception("No valid state found for $className")
    }

    /**
     * Checks if a class is a subclass of another class
     *
     * The information is present in the static info hierarchy. If the superclass is present in the hierarchy, the
     * function checks if the subclass is a subclass of the superclass.
     *
     * @param subclass The subclass
     * @param superclass The superclass
     * @param interpreter The interpreter
     * @return True if subclass is a subclass of superclass, false otherwise
     */
    private fun isSubclassOf(subclass: String, superclass: String, interpreter: Interpreter): Boolean {
        if (interpreter.staticInfo.hierarchy.containsKey(superclass))
            return interpreter.staticInfo.hierarchy[superclass]!!.contains(subclass)

        return false
    }
}