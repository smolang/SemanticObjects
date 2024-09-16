package no.uio.microobject.ast.stmt

import no.uio.microobject.ast.*
import no.uio.microobject.ast.Expression
import no.uio.microobject.ast.Statement
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry
import no.uio.microobject.type.*
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.query.QuerySolution
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLNamedIndividual
import org.semanticweb.owlapi.reasoner.NodeSet

/**
 * Classify statement
 *
 * The classify statement is used to reclassify an object to a new class. The statement will check if the target and the
 * context object are the same. If they are not the same, the function will create a new memory object and check if the
 * target is a subclass of the className. If the target is a subclass of the className, the function will create a new
 * statement and free the old object from the heap. If the target is not a subclass of the className, the function will
 * call the ReclassifyStmt to reclassify the object.
 *
 * @property target The target location
 * @property contextObject The container object
 * @property className The name of the class
 * @property staticTable The static table
 * @property modelsTable The models table
 * @property declares The type of the object
 * @constructor Creates a classify statement
 * @see ReclassifyStmt
 */
data class ClassifyStmt(val target: Location, val contextObject: Expression, val className: String, val staticTable: MutableMap<String, Pair<String, String>>, val modelsTable: MutableMap<String, String>, val declares: Type?) : Statement {
    override fun toString(): String = "Reclassify to a $className"

    override fun getRDF(): String {
        return "prog:stmt${this.hashCode()} rdf:type smol:ReclassifyStatement.\n"
    }

    /**
     * Evaluates the classify statement
     *
     * The function will check if the target and the contextObj are the same. If they are not the same, the function will
     * create a new memory object and check if the target is a subclass of the className. If the target is a subclass of
     * the className, the function will create a new statement and free the old object from the heap. If the target is
     * not a subclass of the className, the function will call the ReclassifyStmt to reclassify the object.
     *
     * @param heapObj The heap object
     * @param stackFrame The current stack frame
     * @param interpreter The interpreter
     * @return The result of the evaluation
     */
    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        // check if the targt and the oldState are the same
        val targetName = target.toString()
        val contextName = contextObject.toString()
        if (targetName != contextName) {
            val newMemory: Memory = mutableMapOf()

            val targetObj: LiteralExpr = interpreter.eval(target, stackFrame)
            val contextObj: LiteralExpr = interpreter.eval(contextObject, stackFrame)

            for ((key, pair) in staticTable) {
                // Check if key is a subclass of className
                if (isSubclassOf(key, className.toString(), interpreter)) {

                    val value: String = pair.first

                    /*
                     * Change the %this to the actual literal mapped into the memory
                     * Also, allow for the usage of rdfs:subClassOf* with %parent mapping it to prog
                     */
                    val query = modifyQuery(value, targetObj, contextObj, className)

                    if (query.startsWith("ASK") || query.startsWith("ask") || query.startsWith("Ask")) {
                        val queryResult = interpreter.ask(query)

                        if (queryResult) {
                            val models = if (modelsTable.containsKey(key)) modelsTable[key]
                                ?.let { LiteralExpr(it, STRINGTYPE) } else null
                            val modeling = if (models != null) listOf(models) else listOf()

                            // check if pair.second is not an empty string
                            if (pair.second == "") {
                                return createStmtAndFreeMemory(target, key, mutableListOf(), declares, modeling, targetObj, interpreter, stackFrame)
                            } else {
                                return processQueryAndCreateStmt(
                                    pair.second,
                                    targetObj,
                                    contextObj,
                                    key,
                                    target,
                                    key,
                                    declares,
                                    modeling,
                                    interpreter,
                                    newMemory,
                                    stackFrame)!!
                            }
                        }
                    } else if (query.startsWith("SELECT") || query.startsWith("select") || query.startsWith("Select")) {
                        val queryResult = interpreter.query(query)

                        if (queryResult != null && queryResult.hasNext()) {
                            val result = queryResult.next()

                            val models = if (modelsTable.containsKey(key)) modelsTable[key]
                                ?.let { LiteralExpr(it, STRINGTYPE) } else null
                            val modeling = if (models != null) listOf(models) else listOf()

                            if (pair.second == "") {
                                val params = mutableListOf<Expression>()
                                processQueryResult(result, interpreter, newMemory, params)

                                return createStmtAndFreeMemory(target, key, params, declares, modeling, targetObj, interpreter, stackFrame)
                            } else {
                                return processQueryAndCreateStmt(
                                    pair.second,
                                    targetObj,
                                    contextObj,
                                    key,
                                    target,
                                    key,
                                    declares,
                                    modeling,
                                    interpreter,
                                    newMemory,
                                    stackFrame)!!
                            }
                        }
                    } else {
                        if (interpreter.settings.verbose) println("execute ISSA:\n $query")

                        var queryToExecute = query

                        if (!query.startsWith("<domain:models>")) {
                            queryToExecute = "<domain:models> some $query"
                        }

                        val res: NodeSet<OWLNamedIndividual> = interpreter.owlQuery(queryToExecute)
                        if (!res.isEmpty) {
                            val prefix = interpreter.settings.prefixMap()
                            val factory = OWLManager.createOWLOntologyManager().owlDataFactory
                            val namedIndividual: OWLNamedIndividual =
                                factory.getOWLNamedIndividual(IRI.create(prefix["run"] + contextObj.literal))
                            if (res.containsEntity(namedIndividual)) {
                                val models = if (modelsTable.containsKey(key)) modelsTable[key]
                                    ?.let { LiteralExpr(it, STRINGTYPE) } else null
                                val modeling = if (models != null) listOf(models) else listOf()

                                if (pair.second == "") {
                                    return createStmtAndFreeMemory(target, key, mutableListOf(), declares, modeling, targetObj, interpreter, stackFrame)
                                } else {
                                    return processQueryAndCreateStmt(
                                        pair.second,
                                        targetObj,
                                        contextObj,
                                        key,
                                        target,
                                        key,
                                        declares,
                                        modeling,
                                        interpreter,
                                        newMemory,
                                        stackFrame)!!
                                }
                            }
                        }
                    }
                }
            }
        }
        return ReclassifyStmt(target, contextObject, className, staticTable, modelsTable, declares).eval(heapObj, stackFrame, interpreter)
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
     * @param targetId The id of the object
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
    private fun processQueryAndCreateStmt(query: String, targetId: LiteralExpr, contextId: LiteralExpr, className: String, target: Location, key: String, declares: Type?, modeling: List<Expression>, interpreter: Interpreter, newMemory: Memory, stackFrame: StackEntry): EvalResult? {
        val newQuery = modifyQuery(query, targetId, contextId, className)
        val queryRes = interpreter.query(newQuery)
        if (queryRes != null && queryRes.hasNext()) {
            val result = queryRes.next()

            // Transform the result to a List<Expression>
            val params = mutableListOf<Expression>()
            processQueryResult(result, interpreter, newMemory, params)

            return createStmtAndFreeMemory(target, key, params, declares, modeling, targetId, interpreter, stackFrame)
        }
        return null
    }
}