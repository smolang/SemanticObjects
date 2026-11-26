package no.uio.microobject.ast.stmt

import no.uio.microobject.ast.*
import no.uio.microobject.ast.Expression
import no.uio.microobject.ast.Statement
import no.uio.microobject.ast.expr.FALSEEXPR
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.ast.expr.LocalVar
import no.uio.microobject.ast.expr.TRUEEXPR
import no.uio.microobject.main.ReasonerMode
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
 * The classify statement is used to classify an object to a new class. The statement will check if the target and the
 * context object are the same. If they are not the same, the function will create a new memory object and check if the
 * target is a subclass of the className. If the target is a subclass of the className, the function will create a new
 * statement and free the old object from the heap. If the target is not a subclass of the className, the function will
 * call the AdaptStmt to adapt the object.
 *
 * @property target The target location
 * @property contextObject The container object
 * @property staticTable The static table
 * @property modelsTable The models table
 * @property declares The type of the object
 * @constructor Creates a classify statement
 * @see AdaptStmt
 */
data class ClassifyStmt(val target: Location, val contextObject: Expression, val staticTable: MutableMap<String, Pair<String, String>>, val modelsTable: MutableMap<String, String>, var declares: Type?) : Statement {
    override fun toString(): String = "Classify to a new class"

    override fun getRDF(): String {
        return "prog:stmt${this.hashCode()} rdf:type smol:ClassifyStatement.\n"
    }

    /**
     * Evaluates the classify statement
     *
     * The reasoner settings are forced to owl for the duration of the evaluation and are always restored, also when an
     * exception escapes the evaluation.
     *
     * @param heapObj The heap object
     * @param stackFrame The current stack frame
     * @param interpreter The interpreter
     * @return The result of the evaluation
     */
    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        val reasoner = interpreter.settings.reasoner
        val jenaReasoner = interpreter.tripleManager.currentTripleSettings.jenaReasoner
        interpreter.settings.reasoner = ReasonerMode.owl
        interpreter.tripleManager.currentTripleSettings.jenaReasoner = ReasonerMode.owl
        try {
            return evalInternal(heapObj, stackFrame, interpreter)
        } finally {
            // Bring back the reasoners
            interpreter.settings.reasoner = reasoner
            interpreter.tripleManager.currentTripleSettings.jenaReasoner = jenaReasoner
        }
    }

    private fun evalInternal(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        val hierarchy = interpreter.staticInfo.hierarchy
        val targetName = target.toString()
        val contextName = contextObject.toString()

        val targetObj: LiteralExpr = interpreter.eval(target, stackFrame)
        val contextObj: LiteralExpr = interpreter.eval(contextObject, stackFrame)

        // The target object may not exist yet: `A x = classify(ctx);` declares x before the classification runs.
        val targetMissing = targetIsUninitialized(stackFrame) ||
                targetObj.literal == "ERROR" ||
                targetObj.literal == "null" ||
                targetObj.tag == NULLTYPE

        // The object the classification is performed on; while the target does not exist yet, the context object
        // plays its role (the new element does not exist yet).
        val sourceObj = if (targetMissing) contextObj else targetObj

        if (targetName != contextName) {
            val newMemory: Memory = mutableMapOf()

            val fields = interpreter.staticInfo.fieldTable

            val className: String = when {
                // Declared target form: the class is determined from the context object (the root superclass)
                targetMissing && target is LocalVar -> classOf(contextObj, hierarchy)
                // Null field target: look up the class declaring that field
                targetMissing -> declaringClassOf(targetName, fields)
                else -> classOf(targetObj, hierarchy)
            }

            declares = BaseType(className)

            for ((key, pair) in staticTable) {
                // Check if key is a subclass of className
                if (isSubclassOf(key, className, interpreter)) {

                    val value: String = pair.first

                    /*
                     * Change the %this to the actual literal mapped into the memory
                     * Also, allow for the usage of rdfs:subClassOf* with %parent mapping it to prog
                     */
                    val query = modifyQuery(value, sourceObj, contextObj, className)

                    if (query.startsWith("ASK") || query.startsWith("ask") || query.startsWith("Ask")) {
                        val queryResult = interpreter.ask(query)

                        if (queryResult) {
                            val models = if (modelsTable.containsKey(key)) modelsTable[key]
                                ?.let { LiteralExpr(it, STRINGTYPE) } else null
                            val modeling = if (models != null) listOf(models) else listOf()

                            if (pair.second == "") {
                                return createStmtAndFreeMemory(target, key, mutableListOf(), declares, modeling, targetObj, interpreter, stackFrame)
                            } else {
                                // An empty retrieves result is a normal outcome: fall through to the next entry
                                val created = processQueryAndCreateStmt(
                                    pair.second,
                                    sourceObj,
                                    contextObj,
                                    className,
                                    target,
                                    key,
                                    declares,
                                    modeling,
                                    targetObj,
                                    interpreter,
                                    newMemory,
                                    stackFrame)
                                if (created != null) return created
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
                                // An empty retrieves result is a normal outcome: fall through to the next entry
                                val created = processQueryAndCreateStmt(
                                    pair.second,
                                    sourceObj,
                                    contextObj,
                                    className,
                                    target,
                                    key,
                                    declares,
                                    modeling,
                                    targetObj,
                                    interpreter,
                                    newMemory,
                                    stackFrame)
                                if (created != null) return created
                            }
                        }
                    } else {
                        if (interpreter.settings.verbose) println("execute ISSA:\n $query")

                        var queryToExecute = query

                        if (!query.startsWith("<smol:links>")) {
                            queryToExecute = "<smol:links> some $query"
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
                                    // An empty retrieves result is a normal outcome: fall through to the next entry
                                    val created = processQueryAndCreateStmt(
                                        pair.second,
                                        sourceObj,
                                        contextObj,
                                        className,
                                        target,
                                        key,
                                        declares,
                                        modeling,
                                        targetObj,
                                        interpreter,
                                        newMemory,
                                        stackFrame)
                                    if (created != null) return created
                                }
                            }
                        }
                    }
                }
            }
        }

        if (targetMissing) {
            // Nothing matched and the new element does not exist yet: alias the target to the context object
            // instead of falling back to AdaptStmt, which needs an existing target object.
            return replaceStmt(AssignStmt(target, contextObj, declares = declares), stackFrame)
        }

        return AdaptStmt(target, staticTable, modelsTable, declares).eval(heapObj, stackFrame, interpreter)
    }

    /**
     * Checks whether the local target has not been assigned yet (declared target form: `A x = classify(ctx);`).
     */
    private fun targetIsUninitialized(stackFrame: StackEntry): Boolean {
        val t = target
        return t is LocalVar && !stackFrame.store.containsKey(t.name)
    }

    /**
     * Determines the class of an object literal: either the class itself or, for subclasses, its root superclass.
     */
    private fun classOf(obj: LiteralExpr, hierarchy: MutableMap<String, MutableSet<String>>): String {
        val tag = obj.tag.toString()
        if (hierarchy.containsKey(tag)) return tag
        return hierarchy.entries.find { it.value.contains(tag) }?.key
            ?: throw Exception("Class is unknown: $tag")
    }

    /**
     * Determines the class declaring the field addressed by [targetName] (null field target form).
     */
    private fun declaringClassOf(targetName: String, fields: Map<String, no.uio.microobject.runtime.FieldEntry>): String {
        val fieldName = targetName.split(".").lastOrNull()
            ?: throw Exception("Cannot determine the class of '$targetName': malformed target name.")
        return fields.entries.firstOrNull { entry -> entry.value.any { it.name == fieldName } }?.key
            ?: throw Exception("Cannot determine the class of '$targetName': no field named '$fieldName' is declared.")
    }

    /**
     * Checks if a class is a subclass of another class (transitively).
     *
     * The information is present in the static info hierarchy: the hierarchy maps a class to its direct subclasses,
     * so the direct edges are walked recursively.
     *
     * @param subclass The subclass
     * @param superclass The superclass
     * @param interpreter The interpreter
     * @return True if subclass is a subclass of superclass, false otherwise
     */
    private fun isSubclassOf(subclass: String, superclass: String, interpreter: Interpreter): Boolean {
        val hierarchy = interpreter.staticInfo.hierarchy
        val visited = mutableSetOf(superclass)
        fun recurse(current: String): Boolean {
            val children = hierarchy[current] ?: return false
            for (child in children) {
                if (child == subclass) return true
                if (visited.add(child) && recurse(child)) return true
            }
            return false
        }
        return recurse(superclass)
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
                    else if (objNameCand == "true" || objNameCand == "false")
                        if (objNameCand == "true") TRUEEXPR else FALSEEXPR
                    else if (objNameCand.matches("(true|false)".toRegex()) || objNameCand.matches("(true|false)\\^\\^http://www.w3.org/2001/XMLSchema#boolean".toRegex()))
                        if (objNameCand.split("^^")[0].equals("true", ignoreCase = true)) TRUEEXPR else FALSEEXPR
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
     * Checks whether an object literal is still referenced from any heap memory or stack store.
     */
    private fun isReferenced(id: LiteralExpr, interpreter: Interpreter): Boolean {
        for (mem in interpreter.heap.values) if (mem.values.any { it == id }) return true
        for (entry in interpreter.stack) if (entry.store.values.any { it == id }) return true
        return false
    }

    /**
     * Creates a new statement and frees the old object from the heap
     *
     * The old object is only removed when nothing else references it anymore, so that aliases held in other
     * objects or stack frames do not dangle.
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

        // Remove the old object from the heap only if it is not referenced anywhere anymore
        if (interpreter.heap.containsKey(id) && !isReferenced(id, interpreter)) {
            interpreter.heap.remove(id)
        }

        return stmt
    }

    /**
     * Processes the query and creates a statement
     *
     * @param query The query to process
     * @param targetId The id of the object (%this)
     * @param contextId The id of the context (%context)
     * @param className The name of the root class (%parent)
     * @param target The target location
     * @param key The key of the new object
     * @param declares The type of the object
     * @param modeling The modeling of the object
     * @param id The id of the object that may be replaced
     * @param interpreter The interpreter
     * @param newMemory The new memory
     * @return The result of the evaluation, or null if the retrieves query returned no rows
     */
    private fun processQueryAndCreateStmt(query: String, targetId: LiteralExpr, contextId: LiteralExpr, className: String, target: Location, key: String, declares: Type?, modeling: List<Expression>, id: LiteralExpr, interpreter: Interpreter, newMemory: Memory, stackFrame: StackEntry): EvalResult? {
        // %parent denotes the root class of the adaptation hierarchy, not the new state class
        val newQuery = modifyQuery(query, targetId, contextId, className)
        val queryRes = interpreter.query(newQuery)
        if (queryRes != null && queryRes.hasNext()) {
            val result = queryRes.next()

            // Transform the result to a List<Expression>
            val params = mutableListOf<Expression>()
            processQueryResult(result, interpreter, newMemory, params)

            // If I have a context object in the interpreter.staticInfo.contextTable, I mast add it to the params
            if (interpreter.staticInfo.contextTable.containsKey(className)) {
                val context = interpreter.staticInfo.contextTable[className]
                if (context != null) {
                    params.add(0, contextId)
                }
            }

            return createStmtAndFreeMemory(target, key, params, declares, modeling, id, interpreter, stackFrame)
        }
        return null
    }
}
