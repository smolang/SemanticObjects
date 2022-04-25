package no.uio.microobject.data

import com.github.owlcs.ontapi.OntManagers
import com.github.owlcs.ontapi.Ontology
import com.github.owlcs.ontapi.OntologyManager
import com.github.owlcs.ontapi.config.OntLoaderConfiguration
import java.io.*
import no.uio.microobject.main.Settings
import no.uio.microobject.runtime.*
import no.uio.microobject.type.*
import org.apache.commons.io.IOUtils
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.Graph
import org.apache.jena.graph.impl.GraphBase
import org.apache.jena.graph.Node
import org.apache.jena.graph.Node_URI
import org.apache.jena.graph.NodeFactory
import org.apache.jena.graph.Triple
import org.apache.jena.graph.compose.MultiUnion
import org.apache.jena.rdf.model.*
import org.apache.jena.reasoner.Reasoner
import org.apache.jena.reasoner.ReasonerRegistry
import org.apache.jena.util.iterator.ExtendedIterator
import org.apache.jena.util.iterator.NiceIterator
import org.semanticweb.owlapi.model.OWLOntology
import java.net.URL


// Settings controlling the TripleManager.
data class TripleSettings(
    val sources: HashMap<String,Boolean>, // Which sources to include
    val guards: HashMap<String,Boolean>, // If true, then guard clauses are used.
    var virtualization: HashMap<String,Boolean>, // If true, virtualization is used. Otherwise, naive method is used.
    var jenaReasoner: String, // Must be either off, rdfs or owl
)

// Class managing triples from all the different sources, how to reason over them, and how to query them using SPARQL or DL queries.
class TripleManager(private val settings: Settings, val staticTable: StaticTable, private val interpreter: Interpreter?) {
    private val prefixMap = settings.prefixMap()

    // Default settings. These can be changed with REPL commands.
    var currentTripleSettings = TripleSettings(
        sources = hashMapOf("heap" to true, "staticTable" to true, "vocabularyFile" to true, "fmos" to true, "externalOntology" to (settings.background != "")),
        guards = hashMapOf("heap" to true, "staticTable" to true),
        virtualization = hashMapOf("heap" to true, "staticTable" to true, "fmos" to true),
        jenaReasoner = "owl"
    )


    // Main method used to deliver the Jena model to run SPARQL queries on.
    // When special settings are given, it will override the general settings
    fun getModel(specialSettings: TripleSettings = currentTripleSettings): Model {
        val model =  getModelUnionWithReasoning(specialSettings)

        // If the materialize flag is given, then write to file
        if (settings.materialize) {
            File(settings.outpath).mkdirs()
            File("${settings.outpath}/output.ttl").createNewFile()
            model.write(FileWriter("${settings.outpath}/output.ttl"),"TTL")
        }
        return model
    }

    // Main method used to deliver an OWLAPI ontology to run OWL queries on
    fun getOntology(tripleSettings: TripleSettings = currentTripleSettings): OWLOntology {
        return getOntologyFromModel(tripleSettings)
    }


    // A variant of getOntology which excludes the heap. This is used e.g. for type checking
    fun getStaticDataOntology(): OWLOntology {
        val specialTripleSettings = TripleSettings(currentTripleSettings.sources, currentTripleSettings.guards, currentTripleSettings.virtualization, currentTripleSettings.jenaReasoner)
        specialTripleSettings.sources["heap"] = false
        return getOntology(specialTripleSettings)
    }

    // Return an OWL ontology corresponding to the requested sources.
    private fun getOntologyFromModel(tripleSettings: TripleSettings): OWLOntology {
        val model = getModelUnion(tripleSettings)
        val manager: OntologyManager = OntManagers.createManager()

        // Settings related to how model is loaded into an ontology.
        val conf: OntLoaderConfiguration = manager.ontologyLoaderConfiguration
        conf.isPerformTransformation = true

        val ontology: Ontology = manager.addOntology(model.graph, conf)
        return ontology
    }


    // Get the Jena model including all requested sources and the requested reasoner
    private fun getModelUnionWithReasoning(tripleSettings: TripleSettings): Model {
        val modelUnion = getModelUnion(tripleSettings)
        val reasoner = getJenaReasoner(tripleSettings) ?: return modelUnion  // Get correct reasoner based on settings
        return ModelFactory.createInfModel(reasoner, modelUnion)
    }


    // Model merging the graphs of the requested sources.
    // Also decides whether to use virtualization or naive approach
    private fun getModelUnion(tripleSettings: TripleSettings): Model {
        val includedGraphs = mutableListOf<Graph>()
        includedGraphs.add(ModelFactory.createDefaultModel().graph) // New default graph. New statements are inserted here.
        if (tripleSettings.sources.getOrDefault("staticTable", false)) {
            if (tripleSettings.virtualization.getOrDefault("staticTable", false)) { includedGraphs.add(StaticTableGraph(tripleSettings)) }
            else { includedGraphs.add(getStaticTableModelNaive(tripleSettings).graph) }
        }
        if (tripleSettings.sources.getOrDefault("heap", false)) {
            if (tripleSettings.virtualization.getOrDefault("heap", false)) { includedGraphs.add(HeapGraph(tripleSettings, interpreter!!)) }
            else { includedGraphs.add(getHeapModelNaive(tripleSettings, interpreter!!).graph) }
        }
        if (tripleSettings.sources.getOrDefault("fmos", false)) {
            if (tripleSettings.virtualization.getOrDefault("fmos", false)) { includedGraphs.add(FMOGraph()) }
            else { includedGraphs.add(getFMOModelNaive().graph) }
        }
        if (tripleSettings.sources.getOrDefault("vocabularyFile", false)) {
            includedGraphs.add(getVocabularyModel().graph)
        }
        if (tripleSettings.sources.getOrDefault("externalOntology", false)) {
            includedGraphs.add(getExternalOntologyAsModel().graph)
        }
        val model = ModelFactory.createModelForGraph(MultiUnion(includedGraphs.toTypedArray()))
        for ((key, value) in prefixMap) model.setNsPrefix(key, value)  // Adding prefixes
        return model
    }

    // Returns the Jena model containing statements from the external ontology.
    // If the external ontology is not given, then it returns an empty model
    private fun getExternalOntologyAsModel(): Model {
        val model = ModelFactory.createDefaultModel()
        if(settings.background != "") {
            var str  = ""
            for ((key, value) in prefixMap) str += "@prefix $key: <$value> .\n"
            str += settings.background + "\n"
            val s: InputStream = ByteArrayInputStream(str.toByteArray())
            model.read(s, null, "TTL")
        }
        return model
    }

    // Returns the Jena model containing statements from vocab.owl
    private fun getVocabularyModel(): Model {
        val vocabularyModel = ModelFactory.createDefaultModel()
        val vocabURL: URL = this::class.java.classLoader.getResource("vocab.owl") ?: return vocabularyModel
        var str = ""
        for ((key, value) in prefixMap) str += "@prefix $key: <$value> .\n"
        str += vocabURL.readText(Charsets.UTF_8)
        val iStream: InputStream = ByteArrayInputStream(str.toByteArray())
        return vocabularyModel.read(iStream, null, "TTL")
    }


    // Get the requested Jena reasoner
    private fun getJenaReasoner(tripleSettings: TripleSettings): Reasoner? {
        when (tripleSettings.jenaReasoner) {
            "off" -> { return null }
            "owl" -> { return ReasonerRegistry.getOWLReasoner() }
            "rdfs" -> { return ReasonerRegistry.getRDFSReasoner() }
        }
        return null
    }

    // A custom type of (nice)iterator which takes a list as input and iterates over them.
    // It iterates through all elements in the list from start to end.
    private class TripleListIterator(private val tripleList: List<Triple>): NiceIterator<Triple>() {
        var listIndex: Int = 0  // index of next element

        override fun hasNext(): Boolean = listIndex < tripleList.size

        override fun next(): Triple = tripleList[(listIndex++)]
    }

    // Helper method to crate triple with URIs in all three positions
    private fun uriTriple(s: String, p: String, o: String): Triple {
        return Triple(NodeFactory.createURI(s), NodeFactory.createURI(p), NodeFactory.createURI(o))
    }

    // If searchTriple matches candidateTriple, then candidateTriple will be added to matchList
    private fun addIfMatch(candidateTriple: Triple, searchTriple: Triple, matchList: MutableList<Triple>, pseudo: Boolean)  {
        // This is just a quick fix to resolve the problem with > and < in the uris. They appear for example when the stdlib.smol is used, since it has List<LISTT>.
        if (candidateTriple.subject.toString().contains(">")) return
        if (candidateTriple.subject.toString().contains("<")) return
        if (candidateTriple.predicate.toString().contains(">")) return
        if (candidateTriple.predicate.toString().contains("<")) return
        if (candidateTriple.`object`.toString().contains(">")) return
        if (candidateTriple.`object`.toString().contains("<")) return
        if (searchTriple.matches(candidateTriple) && !pseudo) matchList.add(candidateTriple)
    }

    private fun getFMOModelNaive(): Model {
        return writeToFileAndReadToModel(FMOGraph())
    }

    // Get model for the static table in the naive way:
    // Extract all triples from the StaticTableGraph, put in model, write model to file, read file to model, return model
    private fun getStaticTableModelNaive(tripleSettings: TripleSettings): Model {
        return writeToFileAndReadToModel(StaticTableGraph(tripleSettings))
    }

    // Get model for the heap in the naive way:
    // Extract all triples from the Heap, put in model, write model to file, read file to model, return model
    private fun getHeapModelNaive(tripleSettings: TripleSettings, interpreter: Interpreter): Model {
        return writeToFileAndReadToModel(HeapGraph(tripleSettings, interpreter))
    }

    // Helper method for the naive approach
    private fun writeToFileAndReadToModel(g: Graph): Model {
        val m1 = ModelFactory.createDefaultModel()

        // Insert into m1
        val it = g.find()
        for (i in it) {
            val p = ResourceFactory.createProperty(i.predicate.toString())
            val o = ResourceFactory.createResource(i.`object`.toString())
            m1.createResource(i.subject.toString()).addProperty(p, o)
        }

        // Write model m1 to file
        File(settings.outpath).mkdirs()
        File("${settings.outpath}/output-naive.ttl").createNewFile()
        m1.write(FileWriter("${settings.outpath}/output-naive.ttl"),"TTL")

        // Read into model m2
        var m2 = ModelFactory.createDefaultModel()
        val uri = File("${settings.outpath}/output-naive.ttl").toURL().toString()
        m2.read(uri, "TTL")

        return m2
    }

    private inner class FMOGraph : GraphBase() {
        override fun graphBaseFind(searchTriple: Triple): ExtendedIterator<Triple> {
            if(interpreter == null)
                return TripleListIterator(mutableListOf())

            val rdf = prefixMap["rdf"]
            val smol = prefixMap["smol"]
            val run = prefixMap["run"]

            val matchingTriples: MutableList<Triple> = mutableListOf()

            for( fmo in interpreter.simMemory ){
                val name = fmo.key.literal
                val value = fmo.value.path
                val valueNode = getLiteralNode(LiteralExpr(value, STRINGTYPE), settings)

                val resTriple =
                    Triple(
                        NodeFactory.createURI("${run}${name}"),
                        NodeFactory.createURI("${smol}loads"),
                        valueNode
                    )
                addIfMatch(resTriple, searchTriple, matchingTriples, false)
                addIfMatch(uriTriple("${run}${name}", "${rdf}type", "${smol}Simulation"), searchTriple, matchingTriples, false)
            }

            return TripleListIterator(matchingTriples)
        }

    }

    // Graph representing the static table
    // If pseudo is set, we always return all triples. This is needed for type checking, where graphBaseFind is not called
    private inner class StaticTableGraph(val tripleSettings: TripleSettings, val pseudo: Boolean = false): GraphBase() {

        // Returns an iterator of all triples in the static table that matches searchTriple
        // graphBaseFind only constructs the triples that match searchTriple.
        public override fun graphBaseFind(searchTriple: Triple): ExtendedIterator<Triple> {
            val useGuardClauses = tripleSettings.guards.getOrDefault("staticTable", true)
            val fieldTable: Map<String,FieldEntry> = staticTable.fieldTable
            val methodTable: Map<String,Map<String,MethodInfo>> = staticTable.methodTable
            val hierarchy: MutableMap<String, MutableSet<String>> = staticTable.hierarchy

            // Prefixes
            val rdf = prefixMap["rdf"]
            val rdfs = prefixMap["rdfs"]
            val owl = prefixMap["owl"]
            val prog = prefixMap["prog"]
            val smol = prefixMap["smol"]
            val domain = prefixMap["domain"]

            // Guard clause checking that the subject of the searchTriple starts with prog. Otherwise, return no triples.
            // This assumes that all triples generated by this method uses prog as the prefix for the subject.
            if (useGuardClauses) {
                if (searchTriple.subject is Node_URI){
                    if (searchTriple.subject.nameSpace != prog) return TripleListIterator(mutableListOf())
                }
            }

            // Guard clause: checking if the predicate of the search triple is one of the given possible URIs
            if (useGuardClauses) {
                if (searchTriple.predicate is Node_URI){
                    val possiblePredicates = mutableListOf("${rdf}type", "${rdfs}range", "${rdfs}domain", "${rdfs}subClassOf", "${smol}hasMethod", "${smol}hasField")
                    val anyEqual = possiblePredicates.any { it == searchTriple.predicate.uri }
                    if (!anyEqual) return TripleListIterator(mutableListOf())
                }
            }

            // Guard clause: set of possible object prefixes it limited
            if (useGuardClauses) {
                if (searchTriple.getObject() is Node_URI){
                    val possibleObjectPrefixes = mutableListOf(smol, owl, prog)
                    val anyEqual = possibleObjectPrefixes.any { it == searchTriple.getObject().nameSpace }
                    if (!anyEqual) return TripleListIterator(mutableListOf())
                }
            }


            val matchingTriples: MutableList<Triple> = mutableListOf()

            // Generate triples for fields (and classes)
            for(classObj in fieldTable){
                val className: String = classObj.key

                addIfMatch(uriTriple("${prog}${className}", "${rdf}type", "${smol}Class"), searchTriple, matchingTriples, pseudo)
                addIfMatch(uriTriple("${prog}${className}", "${rdf}type", "${owl}Class" ), searchTriple, matchingTriples, pseudo)
                addIfMatch(uriTriple("${prog}${className}", "${rdfs}subClassOf", "${prog}Object" ), searchTriple, matchingTriples, pseudo)

                for(fieldEntry in classObj.value){
                    val fieldName: String = classObj.key+"_"+fieldEntry.name
                    // Guard clause: Skip this fieldName when the subject of the search triple is different from both "${prog}${className}" and "${prog}$fieldName"
                    if (useGuardClauses) {
                        if (searchTriple.subject is Node_URI){
                            if (searchTriple.subject.uri != "${prog}${className}" && searchTriple.subject.uri != "${prog}$fieldName") continue
                        }
                    }

                    addIfMatch(uriTriple("${prog}${className}", "${smol}hasField", "${prog}${fieldName}"), searchTriple, matchingTriples, pseudo)
                    addIfMatch(uriTriple("${prog}${fieldName}", "${rdf}type", "${smol}Field"), searchTriple, matchingTriples, pseudo)
                    addIfMatch(uriTriple("${prog}${fieldName}", "${rdfs}domain", "${prog}${className}"), searchTriple, matchingTriples, pseudo)

                    when (fieldEntry.type) {
                        INTTYPE -> {
                            addIfMatch(uriTriple("${prog}${fieldName}", "${rdf}type", "${owl}DatatypeProperty"), searchTriple, matchingTriples, pseudo)
                            addIfMatch(uriTriple("${prog}${fieldName}", "${rdfs}range", XSDDatatype.XSDinteger.uri), searchTriple, matchingTriples, pseudo)
                        }
                        STRINGTYPE -> {
                            addIfMatch(uriTriple("${prog}${fieldName}", "${rdf}type", "${owl}DatatypeProperty"), searchTriple, matchingTriples, pseudo)
                            addIfMatch(uriTriple("${prog}${fieldName}", "${rdfs}range", XSDDatatype.XSDstring.uri), searchTriple, matchingTriples, pseudo)
                        }
                        BOOLEANTYPE -> {
                            addIfMatch(uriTriple("${prog}${fieldName}", "${rdf}type", "${owl}DatatypeProperty"), searchTriple, matchingTriples, pseudo)
                            addIfMatch(uriTriple("${prog}${fieldName}", "${rdfs}range", XSDDatatype.XSDboolean.uri), searchTriple, matchingTriples, pseudo)
                        }
                        DOUBLETYPE -> {
                            addIfMatch(uriTriple("${prog}${fieldName}", "${rdf}type", "${owl}DatatypeProperty"), searchTriple, matchingTriples, pseudo)
                            addIfMatch(uriTriple("${prog}${fieldName}", "${rdfs}range", XSDDatatype.XSDdouble.uri), searchTriple, matchingTriples, pseudo)
                        }
                        else -> {
                            if(fieldEntry.type !is SimulatorType) {
                                addIfMatch(
                                    uriTriple("${prog}${fieldName}", "${rdf}type", "${owl}FunctionalProperty"),
                                    searchTriple,
                                    matchingTriples,
                                    pseudo
                                )
                                addIfMatch(
                                    uriTriple("${prog}${fieldName}", "${rdf}type", "${owl}ObjectProperty"),
                                    searchTriple,
                                    matchingTriples,
                                    pseudo
                                )
                                addIfMatch(
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

            // Generate triples for all methods
            for(classObj in methodTable){
                for(method in classObj.value){
                    val methodName: String = classObj.key+"_"+method.key

                    // Suggestion: should this also be called for rules and domains? Is rules/domains considered to be methods?
                    // example of generated triples from rules:
                    // (prog:Course smol:hasMethod prog:Course_ruleGetLecturer)
                    // (prog:Course_ruleGetLecturer a smol:Method)
                    // (prog:Course_ruleGetLecturer a owl:NamedIndividual)
                    addIfMatch(uriTriple("${prog}${classObj.key}", "${smol}hasMethod", "${prog}${methodName}"), searchTriple, matchingTriples, pseudo)
                    addIfMatch(uriTriple("${prog}${methodName}", "${rdf}type", "${owl}NamedIndividual"), searchTriple, matchingTriples, pseudo)
                    addIfMatch(uriTriple("${prog}${methodName}", "${rdf}type", "${smol}Method"), searchTriple, matchingTriples, pseudo)

                    // Suggestion: The code below is very extensive and should be compressed/refactored.
                    //rule
                    if(method.value.isRule ) {
                        when (method.value.retType) {
                            INTTYPE -> {
                                addIfMatch(uriTriple("${prog}${methodName}_builtin_res", "${rdf}type", "${owl}DatatypeProperty"), searchTriple, matchingTriples, pseudo)
                                addIfMatch(uriTriple("${prog}${methodName}_builtin_res", "${rdfs}range", XSDDatatype.XSDinteger.uri), searchTriple, matchingTriples, pseudo)
                            }
                            STRINGTYPE -> {
                                addIfMatch(uriTriple("${prog}${methodName}_builtin_res", "${rdf}type", "${owl}DatatypeProperty"), searchTriple, matchingTriples, pseudo)
                                addIfMatch(uriTriple("${prog}${methodName}_builtin_res", "${rdfs}range", XSDDatatype.XSDstring.uri), searchTriple, matchingTriples, pseudo)
                            }
                            BOOLEANTYPE -> {
                                addIfMatch(uriTriple("${prog}${methodName}_builtin_res", "${rdf}type", "${owl}DatatypeProperty"), searchTriple, matchingTriples, pseudo)
                                addIfMatch(uriTriple("${prog}${methodName}_builtin_res", "${rdfs}range", XSDDatatype.XSDboolean.uri), searchTriple, matchingTriples, pseudo)
                            }
                            DOUBLETYPE -> {
                                addIfMatch(uriTriple("${prog}${methodName}_builtin_res", "${rdf}type", "${owl}DatatypeProperty"), searchTriple, matchingTriples, pseudo)
                                addIfMatch(uriTriple("${prog}${methodName}_builtin_res", "${rdfs}range", XSDDatatype.XSDdouble.uri), searchTriple, matchingTriples, pseudo)
                            }
                            else -> {
                                addIfMatch(uriTriple("${prog}${methodName}_builtin_res", "${rdf}type", "${owl}FunctionalProperty"), searchTriple, matchingTriples, pseudo)
                                addIfMatch(uriTriple("${prog}${methodName}_builtin_res", "${rdf}type", "${owl}ObjectProperty"), searchTriple, matchingTriples, pseudo)
                                addIfMatch(uriTriple("${prog}${methodName}_builtin_res", "${rdfs}range", "${prog}${method.value.retType}"), searchTriple, matchingTriples, pseudo)
                            }
                        }
                    }
                    if(method.value.isDomain ) {
                        when (method.value.retType) {
                            INTTYPE -> {
                                addIfMatch(uriTriple("${domain}${methodName}_builtin_res", "${rdf}type", "${owl}DatatypeProperty"), searchTriple, matchingTriples, pseudo)
                                addIfMatch(uriTriple("${domain}${methodName}_builtin_res", "${rdfs}range", XSDDatatype.XSDinteger.uri), searchTriple, matchingTriples, pseudo)
                            }
                            STRINGTYPE -> {
                                addIfMatch(uriTriple("${domain}${methodName}_builtin_res", "${rdf}type", "${owl}DatatypeProperty"), searchTriple, matchingTriples, pseudo)
                                addIfMatch(uriTriple("${domain}${methodName}_builtin_res", "${rdfs}range", XSDDatatype.XSDstring.uri), searchTriple, matchingTriples, pseudo)
                            }
                            BOOLEANTYPE -> {
                                addIfMatch(uriTriple("${domain}${methodName}_builtin_res", "${rdf}type", "${owl}DatatypeProperty"), searchTriple, matchingTriples, pseudo)
                                addIfMatch(uriTriple("${domain}${methodName}_builtin_res", "${rdfs}range", XSDDatatype.XSDboolean.uri), searchTriple, matchingTriples, pseudo)
                            }
                            DOUBLETYPE -> {
                                addIfMatch(uriTriple("${domain}${methodName}_builtin_res", "${rdf}type", "${owl}DatatypeProperty"), searchTriple, matchingTriples, pseudo)
                                addIfMatch(uriTriple("${domain}${methodName}_builtin_res", "${rdfs}range", XSDDatatype.XSDdouble.uri), searchTriple, matchingTriples, pseudo)
                            }
                            else -> {
                                addIfMatch(uriTriple("${domain}${methodName}_builtin_res", "${rdf}type", "${owl}FunctionalProperty"), searchTriple, matchingTriples, pseudo)
                                addIfMatch(uriTriple("${domain}${methodName}_builtin_res", "${rdf}type", "${owl}ObjectProperty"), searchTriple, matchingTriples, pseudo)
                                addIfMatch(uriTriple("${domain}${methodName}_builtin_res", "${rdfs}range", "${prog}${method.value.retType}"), searchTriple, matchingTriples, pseudo)
                            }
                        }
                    }
                }
            }

            // Generate triples for the class hierarchy
            val allClasses: MutableSet<String> = methodTable.keys.toMutableSet()
            for(classObj in hierarchy.entries){
                for(subClass in classObj.value){
                    addIfMatch(uriTriple("${prog}${subClass}", "${rdfs}subClassOf", "${prog}${classObj.key}"), searchTriple, matchingTriples, pseudo)
                    allClasses -= subClass
                }
            }
            // allClasses now only contains classes without any ancestors. They should be subclass of Object
            for(classObj in allClasses) addIfMatch(uriTriple("${prog}${classObj}", "${rdfs}subClassOf", "${prog}Object"), searchTriple, matchingTriples, pseudo)

            return TripleListIterator(matchingTriples)
        }
    }


    // Graph representing the heap
    private inner class HeapGraph(val tripleSettings: TripleSettings, interpreter: Interpreter, val pseudo: Boolean = false): GraphBase() {
        var interpreter: Interpreter = interpreter

        // Returns an iterator of all triples in the heap that matches searchTriple
        // graphBaseFind only constructs/fetches the triples that match searchTriple.
        public override fun graphBaseFind(searchTriple: Triple): ExtendedIterator<Triple> {
            val useGuardClauses = tripleSettings.guards.getOrDefault("heap", true)
            val settings: Settings = interpreter.settings
            val heap: GlobalMemory = interpreter.heap

            // Prefixes
            val rdf = interpreter.settings.prefixMap()["rdf"]
            val owl = interpreter.settings.prefixMap()["owl"]
            val prog = interpreter.settings.prefixMap()["prog"]
            val smol = interpreter.settings.prefixMap()["smol"]
            val run = interpreter.settings.prefixMap()["run"]
            val domain = interpreter.settings.prefixMap()["domain"]

            // Guard clause checking that the subject of the searchTriple starts with "run:" or "domain:". Otherwise, return no triples.
            // This guard should be removed or changed if we change the triples we want to be generated from the heap.
            if (useGuardClauses) {
                if (searchTriple.subject is Node_URI) {
                    if (searchTriple.subject.nameSpace != run && searchTriple.subject.nameSpace != domain) {
                        return TripleListIterator( mutableListOf() )
                    }
                }
            }

            val matchingTriples: MutableList<Triple> = mutableListOf()

            for(obj in heap.keys){
                val subjectString = "${run}${obj.literal}"

                // Guard clause. If this obj does not match to the subject of the search triple, then continue to the next obj
                if (useGuardClauses) {
                    if (searchTriple.subject is Node_URI){
                        if (searchTriple.subject.nameSpace == run) {
                            if (searchTriple.subject.uri != subjectString) { continue }
                        }
                    }
                }

                addIfMatch(uriTriple(subjectString, "${rdf}type", "${owl}NamedIndividual"), searchTriple, matchingTriples, pseudo)
                addIfMatch(uriTriple(subjectString, "${rdf}type", "${smol}Object"), searchTriple, matchingTriples, pseudo)
                addIfMatch(uriTriple(subjectString, "${rdf}type", "${prog}${(obj.tag as BaseType).name}"), searchTriple, matchingTriples, pseudo)

                /** this code adds the rule triple directly to the KB */
                if(interpreter.staticInfo.methodTable[obj.tag.name] != null)
                    for (m in interpreter.staticInfo.methodTable[obj.tag.name]!!.entries) {
                        var retVal: Pair<LiteralExpr, LiteralExpr>? = null
                        if (m.value.isRule) {

                            // Guard on the predicate. If the predicate is not what we search for, then we can skip evalCall below.
                            val predicateString = settings.replaceKnownPrefixesNoColon("prog:${m.value.declaringClass}_${m.key}_builtin_res")
                            if (useGuardClauses) {
                                if (searchTriple.predicate is Node_URI){
                                    if (searchTriple.predicate.uri != predicateString) continue
                                }
                            }

                            retVal = interpreter.evalCall(obj.literal, obj.tag.name, m.key)
                            val resNode = getLiteralNode(retVal.second, settings)
                            val resTriple =
                                Triple(
                                    NodeFactory.createURI(settings.replaceKnownPrefixesNoColon("run:${obj.literal}")),
                                    NodeFactory.createURI(predicateString),
                                    resNode
                                )
                            addIfMatch(resTriple, searchTriple, matchingTriples, pseudo)

                        }
                        if (m.value.isDomain && heap[obj]!!.containsKey("__models")) {
                            val models =
                                heap[obj]!!.getOrDefault(
                                    "__models",
                                    LiteralExpr("ERROR")
                                ).literal.removeSurrounding("\"")

                            // Guard on the predicate. If the predicate is not what we search for, then we can skip evalCall below.
                            val predicateString = "$domain${m.value.declaringClass}_${m.key}_builtin_res"
                            if (useGuardClauses) {
                                if (searchTriple.predicate is Node_URI){
                                    if (searchTriple.predicate.uri != predicateString) continue
                                }
                            }

                            if(retVal == null) retVal = interpreter.evalCall(obj.literal, obj.tag.name, m.key)
                            val resNode = getLiteralNode(retVal.second, settings)
                            val resTriple =
                                Triple(
                                    NodeFactory.createURI(settings.replaceKnownPrefixesNoColon(models)),
                                    NodeFactory.createURI(predicateString),
                                    resNode
                                )
                            addIfMatch(resTriple, searchTriple, matchingTriples, pseudo)
                        }
                    }

                // Generating triples for all fields values
                for(store in heap[obj]!!.keys) {

                    if (store == "__models") {
                        // Connect object to a model
                        val modelString = heap[obj]!!.getOrDefault(store, LiteralExpr("ERROR")).literal.removeSurrounding("\"")
                        val modelURI = settings.replaceKnownPrefixesNoColon(modelString)
                        addIfMatch(uriTriple(subjectString, "${domain}models", modelURI), searchTriple, matchingTriples, pseudo)
                    }
                    else if (store == "__describe") {
                        // Connect model to the description
                        var description: String = heap[obj]!!.getOrDefault(store, LiteralExpr("ERROR")).literal

                        // Guard on the subject of the description.
                        // If the first string in the description (which equals the URI of the model) does not match the searchTriple subject, then continue to the next store
                        val modelURI: String = settings.replaceKnownPrefixesNoColon(description.split(" ")[0])
                        if (useGuardClauses) {
                            if (searchTriple.subject is Node_URI){
                                if (searchTriple.subject.uri != modelURI) continue
                            }
                        }

                        // Parse and load the description into a jena model.
                        var extendedDescription = ""
                        //Here we must now check which models clause we take
                        val staticInfo = interpreter.staticInfo
                        if(staticInfo.modelsTable[obj.tag.name] != null && staticInfo.modelsTable[obj.tag.name]!!.isNotEmpty()){
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
                        extendedDescription += description
                        val m: Model = ModelFactory.createDefaultModel().read(IOUtils.toInputStream(extendedDescription, "UTF-8"), null, "TTL")
                        // Consider each triple and add it if it matches the search triple.
                        for (st in m.listStatements()) addIfMatch(st.asTriple(), searchTriple, matchingTriples, pseudo)
                    }
                    else {
                        // Generate triples for each of the fields of the object.
                        val predicateString = "${prog}${obj.tag}_${store}"

                        // Guard on the predicate. If the current predicate does not match the predicate of the search triple, then continue to the next store
                        if (useGuardClauses) {
                            if (searchTriple.predicate is Node_URI){
                                if (searchTriple.predicate.uri != predicateString) continue
                            }
                        }

                        val target: LiteralExpr = heap[obj]!!.getOrDefault(store, LiteralExpr("ERROR"))
                        val candidateTriple = Triple(NodeFactory.createURI(subjectString), NodeFactory.createURI(predicateString), getLiteralNode(target, settings))
                        addIfMatch(candidateTriple, searchTriple, matchingTriples, pseudo)
                    }
                }
            }
            return TripleListIterator(matchingTriples)
        }
    }


    // Given a LiteralExpr, return the correct type of node
    fun getLiteralNode(target: LiteralExpr, settings: Settings): Node {
        val smol = settings.prefixMap()["smol"]
        val run = settings.prefixMap()["run"]
        return if (target.literal == "null") NodeFactory.createURI("${smol}null")
        else if (target.tag == ERRORTYPE || target.tag == STRINGTYPE) NodeFactory.createLiteral(target.literal.removeSurrounding("\""), XSDDatatype.XSDstring)
        else if (target.tag == INTTYPE) NodeFactory.createLiteral(target.literal, XSDDatatype.XSDinteger)
        else if (target.tag == BOOLEANTYPE) NodeFactory.createLiteral(target.literal.toLowerCase(), XSDDatatype.XSDboolean)
        else if (target.tag == DOUBLETYPE) NodeFactory.createLiteral(target.literal, XSDDatatype.XSDdouble)
        else NodeFactory.createURI("${run}${target.literal}")
    }
}
