package no.uio.microobject.ast.stmt

import no.uio.microobject.ast.*
import no.uio.microobject.ast.Expression
import no.uio.microobject.ast.Statement
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.runtime.*
import no.uio.microobject.type.*
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.query.QuerySolution
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.IRI
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
 * @property contextObject The class that contains the object. It can be the same as the target or a superclass
 * @property className The superclass name. This is needed to check that the reclassification is valid for subclasses
 * @property staticTable The static table containing the class name and the query to check which state is the new one
 * @property modelsTable The models table containing the class name and the models for that class
 * @property declares The type of the object
 */
data class ReclassifyStmt(val target: Location, val contextObject: Expression, val className: String, val staticTable: MutableMap<String, Pair<String, String>>, val modelsTable: MutableMap<String, String>, val declares: Type?) : Statement {

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
     */
    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        val newMemory: Memory = mutableMapOf()

        val targetObj: LiteralExpr = interpreter.eval(target, stackFrame)
        val contextObj: LiteralExpr = interpreter.eval(contextObject, stackFrame)

//        interpreter.tripleManager.checkClassifyQueries()

        for ((key, pair) in staticTable) {
            // Check if key is a subclass of className
            if (isSubclassOf(key, className.toString(), interpreter)) {

                val value: String = pair.first

                /*
                 * Change the %this to the actual literal mapped into the memory
                 * Also, allow for the usage of rdfs:subClassOf* with %parent mapping it to prog
                 */
                val query = modifyQuery(value, targetObj, contextObj, className)

                if (query.startsWith("ASK") || query.startsWith("ask") || query.startsWith("Ask")){
                    val queryResult = interpreter.ask(query)

                    if (queryResult) {
                        val models = if(modelsTable.containsKey(key)) modelsTable[key]
                            ?.let { LiteralExpr(it, STRINGTYPE) } else  null
                        val modeling = if(models != null) listOf(models) else listOf()

                        // check if pair.second is not an empty string
                        if (pair.second == "") {
                            val newElement = reclassify(targetObj, key, className, mutableListOf(), modeling, interpreter, stackFrame)

                            return replaceStmt(AssignStmt(target, newElement, declares = declares), stackFrame)
                        } else {
                            val newElement = processStmt(pair.second, contextObj, targetObj, key, className, mutableListOf(), modeling, targetObj, interpreter, newMemory, stackFrame)

                            return newElement?.let { AssignStmt(target, it, declares = declares) }
                                ?.let { replaceStmt(it, stackFrame) }!!
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
                            // Transform the result to a List<Expression>
                            val params = mutableListOf<Expression>()
                            processQueryResult(result, interpreter, newMemory, params)

                            val newElement = reclassify(targetObj, key, className, params, modeling, interpreter, stackFrame)

                            return replaceStmt(AssignStmt(target, newElement, declares = declares), stackFrame)
                        } else {
                            val newElement = processStmt(pair.second, contextObj, targetObj, key, className, mutableListOf(), modeling, targetObj, interpreter, newMemory, stackFrame)

                            return newElement?.let { AssignStmt(target, it, declares = declares) }
                                ?.let { replaceStmt(it, stackFrame) }!!
                        }
                    }
                } else {
                    if(interpreter.settings.verbose) println("execute ISSA:\n $query")

                    var queryToExecute = query

                    if (!query.startsWith("<domain:models>")) {
                        queryToExecute = "<domain:models> some $query"
                    }

                    val res : NodeSet<OWLNamedIndividual> = interpreter.owlQuery(queryToExecute)
                    if (!res.isEmpty) {
                        val prefix = interpreter.settings.prefixMap()
                        val factory = OWLManager.createOWLOntologyManager().owlDataFactory
                        val namedIndividual: OWLNamedIndividual = factory.getOWLNamedIndividual(IRI.create(prefix["run"] + contextObj.literal))
                        if (res.containsEntity(namedIndividual)) {
                            val models = if(modelsTable.containsKey(key)) modelsTable[key]
                                ?.let { LiteralExpr(it, STRINGTYPE) } else  null
                            val modeling = if(models != null) listOf(models) else listOf()

                            if (pair.second == "") {
                                val newElement = reclassify(targetObj, key, className, mutableListOf(), modeling, interpreter, stackFrame)

                                return replaceStmt(AssignStmt(target, newElement, declares = declares), stackFrame)
                            } else {
                                val newElement = processStmt(pair.second, contextObj, targetObj, key, className, mutableListOf(), modeling, targetObj, interpreter, newMemory, stackFrame)

                                return newElement?.let { AssignStmt(target, it, declares = declares) }
                                    ?.let { replaceStmt(it, stackFrame) }!!
                            }
                        }
                    }
                }
            }
        }

        return replaceStmt(AssignStmt(target, target, declares = declares), stackFrame)
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
     * @param targetId The id of the object
     * @param contextId The id of the superclass
     * @param className The name of the class
     * @return The modified query
     */
    private fun modifyQuery(query: String, targetId: LiteralExpr, contextId: LiteralExpr, className: String): String {
        return query
            .removePrefix("\"")
            .removeSuffix("\"")
            .replace("%this", "run:${targetId.literal}")
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
                    else if (objNameCand == "true" || objNameCand == "false") LiteralExpr(found, BOOLEANTYPE)
                    else if (objNameCand.matches("(true|false)".toRegex()) || objNameCand.matches("(true|false)\\^\\^http://www.w3.org/2001/XMLSchema#boolean".toRegex()))
                        LiteralExpr(found.split("^^")[0], BOOLEANTYPE)
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
     * Reclassifies the object to a new class
     *
     * The function reclassifies the object to a new class. The function will remove all the fields that are not in the
     * parent class and add the new fields to the current state. The function will also change the object in the heap
     * and remove the old object from the heap.
     *
     * @param target The target object to reclassify
     * @param newClass The new class name
     * @param parentClass The parent class name
     * @param params The list of parameters that will be used to create the new object
     * @param modeling The list of models for the new object
     * @param interpreter The interpreter
     * @return The new object
     * @throws Exception If the target object is not in the heap
     */
    private fun reclassify(target: LiteralExpr, newClass: String, parentClass: String, params: MutableList<Expression>, modeling: List<Expression>, interpreter: Interpreter, stackFrame: StackEntry): LiteralExpr {
        val currentState = interpreter.heap[target] ?: throw Exception("The target object is not in the heap: $target")
        val parentState = interpreter.staticInfo.fieldTable[parentClass] ?: throw Exception("Parent class $parentClass not found in field table")

        // Remove fields not in the parent class
        currentState.keys.retainAll((parentState.map { it.name } + listOf("__models", "__describe")).toSet())

        // Add new fields from the new class
        interpreter.staticInfo.fieldTable[newClass]?.forEach { field ->
            if (!currentState.containsKey(field.name) && params.isNotEmpty()) {
                currentState[field.name] = params.removeAt(0) as LiteralExpr
            }
        }

        // Process __describe and __models if present
        currentState["__describe"]?.let {
            if (modeling.isNotEmpty()) {
                val rdfName = Names.getNodeName()
                currentState["__models"] = LiteralExpr(rdfName, STRINGTYPE)
                val evals = modeling.map { rdfName + " " + interpreter.eval(it, stackFrame).literal.removeSurrounding("\"") }
                currentState["__describe"] = LiteralExpr(evals.joinToString(" "), STRINGTYPE)
            }
        }

        val newTarget = LiteralExpr(target.literal, BaseType(newClass))
        interpreter.heap.remove(target)
        interpreter.heap[newTarget] = currentState


        // Update references in heap and stack
        interpreter.heap.values.forEach { mem ->
            mem.entries.find { it.value == target }?.setValue(newTarget)
        }
        interpreter.stack.forEach { entry ->
            entry.store.entries.find { it.value == target }?.setValue(newTarget)
        }

        return newTarget
    }

    /**
     * Processes the statement
     *
     * The function processes the statement by modifying the query, executing the query, and reclassifying the object to
     * the new class. The function will return the new object if the query returns some useful data (either true or a
     * result), otherwise, it will return null.
     *
     * @param query The query to execute
     * @param contextId The id of the superclass
     * @param targetId The target object to reclassify
     * @param newClass The new class name
     * @param parentClass The parent class name
     * @param params The list of parameters that will be used to create the new object
     * @param modeling The list of models for the new object
     * @param id The id of the object
     * @param interpreter The interpreter
     * @param newMemory The new memory
     * @return The new object if the query returns some useful data, otherwise null
     */
    private fun processStmt(query: String, contextId: LiteralExpr, targetId: LiteralExpr, newClass: String, parentClass: String, params: MutableList<Expression>, modeling: List<Expression>, id: LiteralExpr, interpreter: Interpreter, newMemory: Memory, stackFrame: StackEntry): LiteralExpr? {
        val newQuery = modifyQuery(query, id, contextId, className)
        val queryRes = interpreter.query(newQuery)
        if (queryRes != null && queryRes.hasNext()) {
            val result = queryRes.next()

            // Transform the result to a List<Expression>
            processQueryResult(result, interpreter, newMemory, params)

            return reclassify(targetId, newClass, parentClass, params, modeling, interpreter, stackFrame)
        }
        return null
    }
}
