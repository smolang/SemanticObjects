package no.uio.microobject.data

import no.uio.microobject.ast.Names
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.ast.expr.TRUEEXPR
import no.uio.microobject.runtime.FieldEntry
import no.uio.microobject.runtime.GlobalMemory
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.MethodInfo
import no.uio.microobject.runtime.Visibility
import no.uio.microobject.type.*
import org.apache.commons.io.IOUtils
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.NodeFactory
import org.apache.jena.graph.Node_URI
import org.apache.jena.graph.Triple
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RiotException
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.containsKey
import kotlin.collections.first
import kotlin.collections.isNotEmpty
import kotlin.collections.iterator

val typeMap = hashMapOf(
    INTTYPE to XSDDatatype.XSDinteger.uri,
    STRINGTYPE to XSDDatatype.XSDstring.uri,
    BOOLEANTYPE to XSDDatatype.XSDboolean.uri,
    DOUBLETYPE to XSDDatatype.XSDdouble.uri
)

fun liftClass(classObj : Map.Entry<String, FieldEntry>,  //class to lift
              manager : TripleManager,
              searchTriple: Triple,
              tripleSettings: TripleSettings,
              matchingTriples: MutableList<Triple>,
              pseudo: Boolean
) {
    val useGuardClauses = tripleSettings.guards.getOrDefault("staticTable", true)
    val className: String = classObj.key
    val prefixMap = manager.settings.prefixMap()
    val rdf = prefixMap["rdf"]
    val rdfs = prefixMap["rdfs"]
    val owl = prefixMap["owl"]
    val prog = prefixMap["prog"]
    val smol = prefixMap["smol"]

    manager.addIfMatch(uriTriple("${prog}${className}", "${rdf}type", "${smol}Class"), searchTriple, matchingTriples, pseudo)
    manager.addIfMatch(uriTriple("${prog}${className}", "${rdf}type", "${owl}Class"), searchTriple, matchingTriples, pseudo)
    manager.addIfMatch(literalTriple("${prog}${className}", "${smol}hasName", className, STRINGTYPE, manager.settings), searchTriple, matchingTriples, pseudo)
    manager.addIfMatch(
        uriTriple("${prog}${className}", "${rdfs}subClassOf", "${prog}Object"),
        searchTriple,
        matchingTriples,
        pseudo
    )

    for (fieldEntry in classObj.value) {
        if (fieldEntry.computationVisibility == Visibility.HIDE) continue
        val fieldName: String = classObj.key + "_" + fieldEntry.name
        // Guard clause: Skip this fieldName when the subject of the search triple is different from both "${prog}${className}" and "${prog}$fieldName"
        if (useGuardClauses) {
            if (searchTriple.subject is Node_URI) {
                if (searchTriple.subject.uri != "${prog}${className}" && searchTriple.subject.uri != "${prog}$fieldName") continue
            }
        }

        manager.addIfMatch(
            uriTriple("${prog}${className}", "${smol}hasField", "${prog}${fieldName}"),
            searchTriple,
            matchingTriples,
            pseudo
        )
        manager.addIfMatch(
            uriTriple("${prog}${fieldName}", "${rdf}type", "${smol}Field"),
            searchTriple,
            matchingTriples,
            pseudo
        )
        if(manager.settings.punning){
            manager.addIfMatch(uriTriple("${prog}${fieldName}", "${rdfs}domain", "${prog}${className}"), searchTriple, matchingTriples, pseudo)


            if(typeMap.containsKey(fieldEntry.type)){
                manager.addIfMatch(uriTriple("${prog}${fieldName}", "${rdf}type", "${owl}DatatypeProperty"), searchTriple, matchingTriples, pseudo)
                manager.addIfMatch(uriTriple("${prog}${fieldName}", "${rdfs}range", typeMap[fieldEntry.type]!!), searchTriple, matchingTriples, pseudo)
            } else {
                if(fieldEntry.type !is SimulatorType) {
                    manager.addIfMatch(
                        uriTriple("${prog}${fieldName}", "${rdf}type", "${owl}FunctionalProperty"),
                        searchTriple,
                        matchingTriples,
                        pseudo
                    )
                    manager.addIfMatch(
                        uriTriple("${prog}${fieldName}", "${rdf}type", "${owl}ObjectProperty"),
                        searchTriple,
                        matchingTriples,
                        pseudo
                    )
                    manager.addIfMatch(
                        uriTriple(
                            "${prog}${fieldName}",
                            "${rdfs}range",
                            "${prog}${fieldEntry.type}"
                        ), searchTriple, matchingTriples, pseudo
                    )

                }
            }
        }
    }
}

fun liftMethod(method : Map.Entry<String, MethodInfo>,  //class to lift
               className : String,
               manager : TripleManager,
               searchTriple: Triple,
               matchingTriples: MutableList<Triple>,
               pseudo: Boolean
) {

    val prefixMap = manager.settings.prefixMap()
    val rdf = prefixMap["rdf"]
    val rdfs = prefixMap["rdfs"]
    val owl = prefixMap["owl"]
    val prog = prefixMap["prog"]
    val smol = prefixMap["smol"]
    val domain = prefixMap["domain"]
    val methodName: String = className+"_"+method.key

    // Suggestion: should this also be called for rules and domains? Is rules/domains considered to be methods?
    // example of generated triples from rules:
    // (prog:Course smol:hasMethod prog:Course_ruleGetLecturer)
    // (prog:Course_ruleGetLecturer a smol:Method)
    manager.addIfMatch(uriTriple("${prog}${className}", "${smol}hasMethod", "${prog}${methodName}"), searchTriple, matchingTriples, pseudo)
    manager.addIfMatch(uriTriple("${prog}${methodName}", "${rdf}type", "${owl}NamedIndividual"), searchTriple, matchingTriples, pseudo)
    manager.addIfMatch(uriTriple("${prog}${methodName}", "${rdf}type", "${smol}Method"), searchTriple, matchingTriples, pseudo)
    manager.addIfMatch(literalTriple("${prog}${className}", "${smol}hasName", className, STRINGTYPE, manager.settings), searchTriple, matchingTriples, pseudo)


    if(method.value.isRule || method.value.isDomain) {

        val local = if(method.value.isDomain) domain else prog
        if(manager.settings.punning) {
            if (typeMap.containsKey(method.value.retType)) {
                manager.addIfMatch(
                    uriTriple(
                        "${local}${methodName}_builtin_res",
                        "${rdf}type",
                        "${owl}DatatypeProperty"
                    ), searchTriple, matchingTriples, pseudo
                )
                manager.addIfMatch(
                    uriTriple(
                        "${local}${methodName}_builtin_res",
                        "${rdfs}range",
                        typeMap[method.value.retType]!!
                    ), searchTriple, matchingTriples, pseudo
                )
            } else {
                manager.addIfMatch(
                    uriTriple(
                        "${local}${methodName}_builtin_res",
                        "${rdf}type",
                        "${owl}FunctionalProperty"
                    ), searchTriple, matchingTriples, pseudo
                )
                manager.addIfMatch(
                    uriTriple("${local}${methodName}_builtin_res", "${rdf}type", "${owl}ObjectProperty"),
                    searchTriple,
                    matchingTriples,
                    pseudo
                )
                manager.addIfMatch(
                    uriTriple(
                        "${local}${methodName}_builtin_res",
                        "${rdfs}range",
                        "${prog}${method.value.retType}"
                    ), searchTriple, matchingTriples, pseudo
                )
            }
        } else {
            manager.addIfMatch(uriTriple("${prog}${methodName}", "${rdf}type", "${smol}Field"), searchTriple, matchingTriples, pseudo)
        }
    }
}


fun liftRuleMethod(m : Map.Entry<String, MethodInfo>,  //class to lift
                   manager : TripleManager,
                   searchTriple: Triple,
                   interpreter: Interpreter,
                   obj : LiteralExpr,
                   matchingTriples: MutableList<Triple>,
                   pseudo: Boolean,
                   useGuardClauses : Boolean
) {
    val prefixMap = manager.settings.prefixMap()
    val prog = prefixMap["prog"]
    val smol = prefixMap["smol"]
    val domain = prefixMap["domain"]

    if (!m.value.isRule && !m.value.isDomain) return


    val subj =
        if(interpreter.heap[obj]!!.containsKey("__models"))
          interpreter.heap[obj]!!["__models"]!!.literal.removeSurrounding("\"")
        else manager.settings.replaceKnownPrefixesNoColon("run:${obj.literal}")

    val local = if(m.value.isDomain) domain else prog
    // Guard on the predicate. If the predicate is not what we search for, then we can skip evalCall below.
    val predicateString = manager.settings.replaceKnownPrefixesNoColon("${local}:${m.value.declaringClass}_${m.key}_builtin_res")
    if (useGuardClauses && searchTriple.predicate is Node_URI &&
        searchTriple.predicate.uri != "${smol}hasEntry" &&
        searchTriple.predicate.uri != "${smol}hasValue" &&
        searchTriple.subject.uri != subj) return


    val retVal = interpreter.evalCall(obj.literal, (obj.tag as BaseType).name, m.key)
    val resNode = getLiteralNode(retVal.second, manager.settings)

    if(manager.settings.punning) {
        val resTriple =
            Triple(
                NodeFactory.createURI(subj),
                NodeFactory.createURI(predicateString),
                resNode
            )
        manager.addIfMatch(resTriple, searchTriple, matchingTriples, pseudo)
    } else {
        val entry = NodeFactory.createURI(Names.getEntryName())
        var resTriple =
            Triple(
                NodeFactory.createURI(subj),
                NodeFactory.createURI("${smol}hasEntry"),
                entry
            )
        manager.addIfMatch(resTriple, searchTriple, matchingTriples, pseudo)
        resTriple =
            Triple(
                entry,
                NodeFactory.createURI("${smol}hasValue"),
                resNode
            )
        manager.addIfMatch(resTriple, searchTriple, matchingTriples, pseudo)
    }
}

fun liftField(store:String,  //class to lift
              manager : TripleManager,
              searchTriple: Triple,
              interpreter: Interpreter,
              obj : LiteralExpr,
              matchingTriples: MutableList<Triple>,
              pseudo: Boolean,
              heap: GlobalMemory,
              useGuardClauses : Boolean){

    val prefixMap = manager.settings.prefixMap()
    val prog = prefixMap["prog"]
    val smol = prefixMap["smol"]
    val domain = prefixMap["domain"]
    //get the declaration
    val fDeclare = interpreter.staticInfo.fieldTable[(obj.tag as BaseType).name]!!.first { it.name == store }

    val local = if(fDeclare.isDomain) domain else prog

    val target : String =
        if(fDeclare.isDomain)
            manager.settings.replaceKnownPrefixesNoColon(heap[obj]!!.getOrDefault("__models", LiteralExpr("ERROR")).literal.removeSurrounding("\""))
        else heap[obj]!!.getOrDefault(store, LiteralExpr("ERROR")).literal
    val value: LiteralExpr = heap[obj]!!.getOrDefault(store, LiteralExpr("ERROR"))
    val resNode = getLiteralNode(value, manager.settings)

    if(manager.settings.punning) {
        val predicateString = "${local}${obj.tag}_${store}"

        if (useGuardClauses) {
            if (searchTriple.predicate is Node_URI) {
                if (searchTriple.predicate.uri != predicateString && searchTriple.subject.uri != target) return
            }
        }

        val candidateTriple = Triple(
            NodeFactory.createURI(target),
            NodeFactory.createURI(predicateString),
            resNode
        )
        manager.addIfMatch(candidateTriple, searchTriple, matchingTriples, pseudo)
    } else {

        val entry = NodeFactory.createURI(Names.getEntryName())
        var resTriple =
            Triple(
                NodeFactory.createURI(target),
                NodeFactory.createURI("${smol}hasEntry"),
                entry
            )
        manager.addIfMatch(resTriple, searchTriple, matchingTriples, pseudo)
        resTriple =
            Triple(
                entry,
                NodeFactory.createURI("${smol}hasValue"),
                resNode
            )
        manager.addIfMatch(resTriple, searchTriple, matchingTriples, pseudo)
    }
}

fun generateLinkage(manager : TripleManager,
                  searchTriple: Triple,
                  interpreter: Interpreter,
                  obj : LiteralExpr,
                  matchingTriples: MutableList<Triple>,
                  pseudo: Boolean,
                  heap: GlobalMemory,
                  useGuardClauses : Boolean){
    // Connect model to the description
    var description: String = heap[obj]!!.getOrDefault("__describe", LiteralExpr("ERROR")).literal

    // Guard on the subject of the description.
    // If the first string in the description (which equals the URI of the model) does not match the searchTriple subject, then continue to the next store
    val modelURI: String = manager.settings.replaceKnownPrefixesNoColon(description.split(" ")[0])
    if (useGuardClauses) {
        if (searchTriple.subject is Node_URI){
            if (searchTriple.subject.uri != modelURI) return
        }
    }

    // Parse and load the description into a jena model.
    var extendedDescription = ""
    //Here we must now check which models clause we take
    val staticInfo = interpreter.staticInfo
    if(staticInfo.modelsTable[(obj.tag as BaseType).name] != null && staticInfo.modelsTable[obj.tag.name]!!.isNotEmpty()){
        for(mEntry in staticInfo.modelsTable[obj.tag.name]!!){
            val ret = interpreter.evalClassLevel(mEntry.first, obj)
            if(ret == TRUEEXPR){
                val target = heap[obj]!!.getOrDefault("__models", LiteralExpr("ERROR")).literal.removeSurrounding("\"")
                val descr = mEntry.second.removeSurrounding("\"")
                description = "$target $descr\n"
                break
            }
        }
    }

    for ((key, value) in interpreter.settings.prefixMap()) extendedDescription += "@prefix $key: <$value> .\n"
    description = description.replace("\\\"","\"")
    for(fd in heap[obj]!!.keys.filter { !it.startsWith("__") }){
        val ll = getLiteralNode(heap[obj]!![fd]!!, manager.settings)
        description = if(ll.isLiteral)
            description.replace("%$fd",ll.literal.toString(true).replace(manager.settings.prefixMap()["xsd"]!!,"xsd:"))
        else
            description.replace("%$fd",ll.toString())
    }

    //this instantiates blank nodes so they are stable over subqueries, should probably be moved into the translation
    val matches = Regex("_:[a-zA-Z0-9]*").findAll(description)
    for(m in matches) {
        val suffix = m.value.split(":")[1]
        val newName = "domain:virt_${modelURI.split("#")[1]}_$suffix"
        description = description.replace(m.value, newName)
    }
    extendedDescription += description
    try {
        val m: Model = ModelFactory.createDefaultModel().read(IOUtils.toInputStream(extendedDescription, "UTF-8"), null, "TTL")
        // Consider each triple and add it if it matches the search triple.
        for (st in m.listStatements()) manager.addIfMatch(st.asTriple(), searchTriple, matchingTriples, pseudo)
    } catch (_: RiotException){
        println("Parsing error during lifting of the extended model description aka linkage.")
    }
}