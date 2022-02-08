package no.uio.microobject.data

import com.github.owlcs.ontapi.OntManagers
import com.github.owlcs.ontapi.Ontology
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
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.InfModel
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.Reasoner
import org.apache.jena.reasoner.ReasonerRegistry
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner
import org.apache.jena.reasoner.rulesys.Rule
import org.apache.jena.util.iterator.ExtendedIterator
import org.apache.jena.util.iterator.NiceIterator
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import java.net.URL


// Class managing triples from all the different sources, how to reason over them, and how to query them using SPARQL or DL queries.
// Change sources map to control which sources to include when querying
// Change guards map to control when to use guard clauses
// Change reasoner to control which reasoner to use
// Call getCompleteModel/getCompleteModelMultiGraph to get the Jena model to query on
class TripleManager(private val settings: Settings, val staticTable: StaticTable, private val interpreter: Interpreter?) {
    private val prefixMap = settings.prefixMap()

    val guards: HashMap<String,Boolean> = hashMapOf("heap" to true, "staticTable" to true) // If true then guard clauses are used.
    val sources: HashMap<String,Boolean> =  hashMapOf("heap" to true, "staticTable" to true, "vocabularyFile" to true, "externalOntology" to (settings.background != ""))  // Which sources are used when getCompleteModel is called
    var reasoner: String = "owl"  // Must be either off, rdfs or owl

    // Main access point
    fun getModel(): Model {
        return getCompleteModelMultiGraph()
//        return getCompleteModel()
    }

    // Should eventually be removed
    // Get the ontology representing only the static data. This is used e.g. for type checking.
    fun getStaticDataOntology(): OWLOntology {
        // Using ONT-API to connect Jena with the OWLAPI
        val manager: OWLOntologyManager = OntManagers.createManager()
        val ontology: OWLOntology = manager.createOntology(IRI.create("${settings.langPrefix}ontology"))
        // This model corresponds to the ontology, so adding to the model also adds to ontology, if they are legal OWL axioms.
        val model: Model = (ontology as com.github.owlcs.ontapi.Ontology).asGraphModel()

        // Read vocab.owl and background data
        var allTriplesString  = ""
        for ((key, value) in prefixMap) allTriplesString += "@prefix $key: <$value> .\n"
        val vocabURL: java.net.URL = this::class.java.classLoader.getResource("vocab.owl")
        allTriplesString += vocabURL.readText(Charsets.UTF_8) + "\n"
        if(settings.background != "") allTriplesString += settings.background + "\n"
        val s: InputStream = ByteArrayInputStream(allTriplesString.toByteArray())
        model.read(s, null, "TTL")

        // Adding prefixes
        for ((key, value) in prefixMap) model.setNsPrefix(key, value)

        // Add triples from static table
        val staticTableGraphModel: Model = ModelFactory.createModelForGraph(StaticTableGraph())
        model.add(staticTableGraphModel)

        // write model to file if the materialize flag is given
        if (settings.materialize) {
            if(!File("${settings.outpath}/output.ttl").exists()) {
                File(settings.outpath).mkdirs()
                File("${settings.outpath}/output.ttl").createNewFile()
            }
            model.write(FileWriter("${settings.outpath}/output.ttl"),"TTL")
        }

        return ontology
    }


    // Should eventually be removed
    // This returns an OWL ontology containing all OWL axioms.
    // The corresponding Jena model includes all RDF triples
    // Triples/axioms are gathered from: vocab.owl, background data, heap, static table
    // TODO: add simulation data
    fun getCompleteOntology(): OWLOntology {
        // Using ONT-API to connect Jena with the OWLAPI
        val manager: OWLOntologyManager = OntManagers.createManager()
        val ontology: OWLOntology = manager.createOntology(IRI.create("${settings.langPrefix}ontology"))
        // This model corresponds to the ontology, so adding to the model also adds to ontology, if they are legal OWL axioms.
        var model: Model = (ontology as com.github.owlcs.ontapi.Ontology).asGraphModel()

        // Add triples manually to model/ontology. Can be used when debugging.
        // var allTriplesStringPre: String = ""
        // for ((key, value) in prefixMap) allTriplesStringPre += "@prefix $key: <$value> .\n"
        // var triplesHardCoded = "run:obj6 a prog:Student ." +
        // "prog:Student owl:disjointWith prog:Course ."
        // allTriplesStringPre += triplesHardCoded
        // val sPre: InputStream = ByteArrayInputStream(allTriplesStringPre.toByteArray())
        // model.read(sPre, null, "TTL")

        // Read vocab.owl and background data
        var allTriplesString: String = ""
        for ((key, value) in prefixMap) allTriplesString += "@prefix $key: <$value> .\n"
        val vocabURL: java.net.URL = this::class.java.classLoader.getResource("vocab.owl")
        allTriplesString += vocabURL.readText(Charsets.UTF_8) + "\n"
        if(settings.background != "") allTriplesString += settings.background + "\n"
        val s: InputStream = ByteArrayInputStream(allTriplesString.toByteArray())
        model.read(s, null, "TTL")

        val staticTableGraphModel: Model = ModelFactory.createModelForGraph(StaticTableGraph())
        model.add(staticTableGraphModel)

        if (interpreter != null) {
            // Add triples from heap
            val heapGraphModel: Model = ModelFactory.createModelForGraph(HeapGraph(interpreter))
            model.add(heapGraphModel)
        }

        if (interpreter != null) {
            val rules = interpreter.rules
            // Add rules to the model if available.
            if(rules != "" || settings.backgroundrules != "") {
                if(settings.verbose) println("Loading generated builtin rules $rules and domain rules ${settings.backgroundrules}")
                val prefixes  = settings.prefixes()
                val reader = (prefixes+"\n"+rules+"\n"+settings.backgroundrules).byteInputStream().bufferedReader()
                val rParsed = Rule.rulesParserFromReader(BufferedReader(reader))
                val reasoner: org.apache.jena.reasoner.Reasoner = GenericRuleReasoner(Rule.parseRules(rParsed))
                val infModel = ModelFactory.createInfModel(reasoner, model)
                //infModel.prepare()
                model = infModel
            }
        }

        // Adding prefixes
        for ((key, value) in prefixMap) model.setNsPrefix(key, value)

        // write model to file if the materialize flag is given
        if (settings.materialize) {
            File(settings.outpath).mkdirs()
            File("${settings.outpath}/output.ttl").createNewFile()
            model.write(FileWriter("${settings.outpath}/output.ttl"),"TTL")
        }
        return ontology
    }

    // Using ONT-API to return the model corresponding to the complete ontology
    private fun getCompleteModel(): Model {
        val ontology: OWLOntology = getCompleteOntology()
        val model = (ontology as com.github.owlcs.ontapi.Ontology).asGraphModel()

        // Turn on reasoning if background knowledge is given.
        if(settings.background != "") {
            if(settings.verbose) println("Using background knowledge...")
            return ModelFactory.createInfModel(ReasonerRegistry.getOWLReasoner(), model)
        }
        return model
    }

    // Returns a Jena model with the statements from the external ontology.
    // If the external ontology is not given, then it returns an empty model
    private fun getExternalOntologyAsModel(): Model {
        val model = ModelFactory.createDefaultModel()
        var allTriplesString  = ""
        for ((key, value) in prefixMap) allTriplesString += "@prefix $key: <$value> .\n"
        if(settings.background != "") allTriplesString += settings.background + "\n"
        val s: InputStream = ByteArrayInputStream(allTriplesString.toByteArray())
        model.read(s, null, "TTL")
        return model
    }

    // Returns a Jena model corresponding to the file vocab.owl
    private fun getVocabularyModel():Model {
        val vocabularyModel = ModelFactory.createDefaultModel()
        val vocabURL: URL = this::class.java.classLoader.getResource("vocab.owl") ?: return vocabularyModel
        var str  = ""
        for ((key, value) in prefixMap) str += "@prefix $key: <$value> .\n"
        str += vocabURL.readText(Charsets.UTF_8) + "\n"
        val iStream: InputStream = ByteArrayInputStream(str.toByteArray())
        return vocabularyModel.read(iStream, null, "TTL")
    }


    // Model merging the graphs of the included sources.
    private fun getCompleteModelMultiGraph(): Model {
        val includedGraphs = mutableListOf<Graph>()
        if (sources.getOrDefault("staticTable", false)) { includedGraphs.add(StaticTableGraph()) }
        if (sources.getOrDefault("heap", false)) { includedGraphs.add(HeapGraph(interpreter!!)) }
        if (sources.getOrDefault("vocabularyFile", false)) { includedGraphs.add(getVocabularyModel().graph) }
        if (sources.getOrDefault("externalOntology", false)) { includedGraphs.add(getExternalOntologyAsModel().graph) }
        val unionModel = ModelFactory.createModelForGraph(MultiUnion(includedGraphs.toTypedArray()))

        // Turn on inference if requested
        val reasoner = getReasoner() ?: return unionModel  // Get correct reasoner based on settings
        return ModelFactory.createInfModel(reasoner, unionModel)
    }

    private fun getReasoner(): Reasoner? {
        when (reasoner) {
            "off" -> { return null }
            "owl" -> { return ReasonerRegistry.getOWLReasoner() }
            "rdfs" -> { return ReasonerRegistry.getRDFSReasoner() }
        }
        return null
    }

    // A custom type of (nice)iterator which takes a list as input and iterates over them.
    // It iterates through all elements in the list from start to end.
    class TripleListIterator(private val tripleList: List<Triple>): NiceIterator<Triple>() {
        var listIndex: Int = 0  // index of next element

        override fun hasNext(): Boolean = listIndex < tripleList.size

        override fun next(): Triple = tripleList[(listIndex++)]
    }

    // Helper method to crate triple with URIs in all three positions
    fun uriTriple(s: String, p: String, o: String): Triple {
        return Triple(NodeFactory.createURI(s), NodeFactory.createURI(p), NodeFactory.createURI(o))
    }

    // If searchTriple matches candidateTriple, then candidateTriple will be added to matchList
    fun addIfMatch(candidateTriple: Triple, searchTriple: Triple, matchList: MutableList<Triple>, pseudo: Boolean)  {
        if (searchTriple.matches(candidateTriple) && !pseudo) matchList.add(candidateTriple)
    }


    // Graph representing the static table
    // If pseudo is set, we always return all triples. This is needed for type checking, where graphBaseFind is not called
    inner class StaticTableGraph(val pseudo: Boolean = false): GraphBase() {

        // Returns an iterator of all triples in the static table that matches searchTriple
        // graphBaseFind only constructs the triples that match searchTriple.
        public override fun graphBaseFind(searchTriple: Triple): ExtendedIterator<Triple> {
            val useGuardClauses = guards.getOrDefault("staticTable", true)
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
                            addIfMatch(uriTriple("${prog}${fieldName}", "${rdf}type", "${owl}FunctionalProperty"), searchTriple, matchingTriples, pseudo)
                            addIfMatch(uriTriple("${prog}${fieldName}", "${rdf}type", "${owl}ObjectProperty"), searchTriple, matchingTriples, pseudo)
                            addIfMatch(uriTriple("${prog}${fieldName}", "${rdfs}range", "${prog}${fieldEntry.type}"), searchTriple, matchingTriples, pseudo)
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
    inner class HeapGraph(interpreter: Interpreter, val pseudo: Boolean = false): GraphBase() {
        var interpreter: Interpreter = interpreter

        // Returns an iterator of all triples in the heap that matches searchTriple
        // graphBaseFind only constructs/fetches the triples that match searchTriple.
        public override fun graphBaseFind(searchTriple: Triple): ExtendedIterator<Triple> {
            val useGuardClauses = guards.getOrDefault("heap", true)
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
