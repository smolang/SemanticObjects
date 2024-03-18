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
import org.apache.jena.query.QuerySolution
import org.semanticweb.owlapi.model.OWLNamedIndividual
import org.semanticweb.owlapi.reasoner.NodeSet

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
data class ReclassifyStmt(val target: Location, val containerObject: Expression, val className: String, val staticTable: MutableMap<String, Pair<String, String>>, val modelsTable: MutableMap<String, String>, val declares: Type?) : Statement {

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
        val newMemory: Memory = mutableMapOf()

        val t = interpreter.eval(target, stackFrame)
        val e = interpreter.eval(containerObject, stackFrame)

        for ((key, pair) in staticTable) {
            // Check if key is a subclass of className
            if (isSubclassOf(key, className.toString(), interpreter)) {
                val id: LiteralExpr = LiteralExpr(t.literal, BaseType(t.literal))
                val contextId: LiteralExpr = LiteralExpr(e.literal, BaseType(e.literal))

                val value: String = pair.first

                /*
                 * Change the %this to the actual literal mapped into the memory
                 * Also, allow for the usage of rdfs:subClassOf* with %parent mapping it to prog
                 */
                val query = modifyQuery(value, id, contextId, className)

                if (query.startsWith("ASK") || query.startsWith("ask") || query.startsWith("Ask")){
                    val queryResult = interpreter.ask(query)

                    if (queryResult) {
                        val models = if(modelsTable.containsKey(key)) modelsTable[key]
                            ?.let { LiteralExpr(it, STRINGTYPE) } else  null
                        val modeling = if(models != null) listOf(models) else listOf()

                        // check if pair.second is not an empty string
                        if (pair.second == "") {
                            val stmt = replaceStmt(CreateStmt(target, key, listOf(), declares = declares, modeling = modeling), stackFrame)

                            // Remove the old object from the heap
                            if (interpreter.heap.containsKey(id)) {
                                interpreter.heap.remove(id)
                            }

                            return stmt
                        } else {
                            return processQueryAndCreateStmt(pair.second, id, contextId, className, target, key, declares, modeling, interpreter, newMemory, stackFrame)!!
                        }
                    }
                } else if (query.startsWith("SELECT") || query.startsWith("select") || query.startsWith("Select")) {
                    val queryResult = interpreter.query(query)

                    if (queryResult != null && queryResult.hasNext()) {
                        val result = queryResult.next()

                        val models = if(modelsTable.containsKey(key)) modelsTable[key]
                            ?.let { LiteralExpr(it, STRINGTYPE) } else  null
                        val modeling = if(models != null) listOf(models) else listOf()

                        if (pair.second == "") {
                            val stmt = replaceStmt(CreateStmt(target, key, listOf(), declares = declares, modeling = modeling), stackFrame)

                            // Remove the old object from the heap
                            if (interpreter.heap.containsKey(id)) {
                                interpreter.heap.remove(id)
                            }

                            return stmt
                        } else {
                            return processQueryAndCreateStmt(pair.second, id, contextId, className, target, key, declares, modeling, interpreter, newMemory, stackFrame)!!
                        }
                    }
                } else {
                    val res : NodeSet<OWLNamedIndividual> = interpreter.owlQuery(query)
                    if (res.isEmpty) {
                        throw Exception("No results returned from the query.")
                    }

                    val models = if(modelsTable.containsKey(key)) modelsTable[key]
                        ?.let { LiteralExpr(it, STRINGTYPE) } else  null
                    val modeling = if(models != null) listOf(models) else listOf()

                    if (pair.second == "") {
                        val stmt = replaceStmt(CreateStmt(target, key, listOf(), declares = declares, modeling = modeling), stackFrame)

                        // Remove the old object from the heap
                        if (interpreter.heap.containsKey(id)) {
                            interpreter.heap.remove(id)
                        }

                        return stmt
                    } else {
                        return processQueryAndCreateStmt(pair.second, id, contextId, className, target, key, declares, modeling, interpreter, newMemory, stackFrame)!!
                    }
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

    /**
     * Modifies the query to replace the %this and %context with the actual literals
     *
     * @param query The query to modify
     * @param id The id of the object
     * @param superId The id of the superclass
     * @param className The name of the class
     * @return The modified query
     */
    private fun modifyQuery(query: String, id: LiteralExpr, contextId: LiteralExpr, className: String): String {
        return query
            .removePrefix("\"")
            .removeSuffix("\"")
            .replace("%this", "run:${id.literal}")
            .replace("%context", "run:${contextId.literal}")
            .replace("%parent", "prog:${className}")
    }

    /**
     * Processes the query result and adds the variables to the params list
     *
     * For each variable in the result, the function adds the variable to the params list after modifying the variable
     * to the correct type. The function will remove the ^^XMLSchema#datatype from the variable and add the variable to
     * the params list.
     *
     * @param result The query result
     * @param interpreter The interpreter
     * @param newMemory The new memory
     * @param params The list of parameters that will be used to create the new object
     */
    private fun processQueryResult(result: QuerySolution, interpreter: Interpreter, newMemory: Memory, params: MutableList<Expression>) {
        result.varNames().forEachRemaining { variableName ->
            val varObj = result.get(variableName)
            val variable = if (varObj.isLiteral) {
                val found = varObj.toString().removePrefix(interpreter.settings.runPrefix)
                val objNameCand = if (found.startsWith("\\\"")) found.replace("\\\"", "\"") else found
                for (ob in interpreter.heap.keys) {
                    if (ob.literal == objNameCand) {
                        LiteralExpr(objNameCand, ob.tag)
                        break
                    }
                }
                if (!newMemory.containsKey("content")) {
                    if (varObj.isLiteral && varObj.asNode().literalDatatype == XSDDatatype.XSDstring)
                        LiteralExpr("\"" + found + "\"", STRINGTYPE)
                    else if (varObj.isLiteral && varObj.asNode().literalDatatype == XSDDatatype.XSDinteger)
                        LiteralExpr(found.split("^^")[0], INTTYPE)
                    else if (varObj.isLiteral && varObj.asNode().literalDatatype == XSDDatatype.XSDdouble)
                        LiteralExpr(found.split("^^")[0], DOUBLETYPE)
                    else if (varObj.isLiteral && varObj.asNode().literalDatatype == XSDDatatype.XSDfloat)
                        LiteralExpr(found.split("^^")[0], DOUBLETYPE)
                    else if (objNameCand.matches("\\d+".toRegex()) || objNameCand.matches("\\d+\\^\\^http://www.w3.org/2001/XMLSchema#integer".toRegex()))
                        LiteralExpr(found.split("^^")[0], INTTYPE)
                    else if (objNameCand.matches("\\d+".toRegex()) || objNameCand.matches("\\d+\\^\\^http://www.w3.org/2001/XMLSchema#int".toRegex()))
                        LiteralExpr(found.split("^^")[0], INTTYPE)
                    else if (objNameCand.matches("\\d+.\\d+".toRegex())) LiteralExpr(found, DOUBLETYPE)
                    else throw Exception("Query returned unknown object/literal: $found")
                } else {
                    LiteralExpr(varObj.toString(), BaseType(varObj.toString()))
                }
            } else {
                LiteralExpr(varObj.toString(), BaseType(varObj.toString()))
            }
            params.add(variable)
        }
    }

    /**
     * Creates a new statement and frees the old object from the heap
     *
     * @param target The target location
     * @param key The key of the new object
     * @param params The parameters to create the new object
     * @param declares The type of the object
     * @param modeling The modeling of the object
     * @param id The id of the object
     * @param interpreter The interpreter
     * @param stackFrame The current stack frame
     * @return The result of the evaluation
     */
    private fun createStmtAndFreeMemory(target: Location, key: String, params: MutableList<Expression>, declares: Type?, modeling: List<Expression>, id: LiteralExpr, interpreter: Interpreter, stackFrame: StackEntry): EvalResult {
        val stmt = replaceStmt(CreateStmt(target, key, params, declares = declares, modeling = modeling), stackFrame)

        // Remove the old object from the heap
        if (interpreter.heap.containsKey(id)) {
            interpreter.heap.remove(id)
        }

        return stmt
    }

    /**
     * Processes the query and creates a statement
     *
     * @param query The query to process
     * @param id The id of the object
     * @param contextId The id of the context
     * @param className The name of the class
     * @param target The target location
     * @param key The key of the new object
     * @param declares The type of the object
     * @param modeling The modeling of the object
     * @param interpreter The interpreter
     * @param newMemory The new memory
     * @return The result of the evaluation
     */
    private fun processQueryAndCreateStmt(query: String, id: LiteralExpr, contextId: LiteralExpr, className: String, target: Location, key: String, declares: Type?, modeling: List<Expression>, interpreter: Interpreter, newMemory: Memory, stackFrame: StackEntry): EvalResult? {
        val newQuery = modifyQuery(query, id, contextId, className)
        val queryRes = interpreter.query(newQuery)
        if (queryRes != null && queryRes.hasNext()) {
            val result = queryRes.next()

            // Transform the result to a List<Expression>
            val params = mutableListOf<Expression>()
            processQueryResult(result, interpreter, newMemory, params)

            return createStmtAndFreeMemory(target, key, params, declares, modeling, id, interpreter, stackFrame)
        }
        throw Exception("No valid state found for $className")
    }
}