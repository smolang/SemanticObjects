package no.uio.microobject.data

import com.github.owlcs.ontapi.OntManagers
import com.github.owlcs.ontapi.OntologyManager
import com.github.owlcs.ontapi.config.OntLoaderConfiguration
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.main.ReasonerMode
import java.io.*
import no.uio.microobject.main.Settings
import no.uio.microobject.main.testModel
import no.uio.microobject.runtime.*
import no.uio.microobject.type.*
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.Graph
import org.apache.jena.graph.impl.GraphBase
import org.apache.jena.graph.Node
import org.apache.jena.graph.Node_URI
import org.apache.jena.graph.NodeFactory
import org.apache.jena.graph.Triple
import org.apache.jena.graph.compose.MultiUnion
import org.apache.jena.rdf.model.*
import org.apache.jena.rdfconnection.RDFConnectionFactory
import org.apache.jena.reasoner.Reasoner
import org.apache.jena.reasoner.ReasonerRegistry
import org.apache.jena.riot.RiotException
import org.apache.jena.util.iterator.ExtendedIterator
import org.apache.jena.util.iterator.NiceIterator
import org.javafmi.wrapper.Simulation
import org.semanticweb.owlapi.model.OWLOntology
import java.net.URL
import java.util.*
import kotlin.collections.HashMap


// Settings controlling the TripleManager.
data class TripleSettings(
    val sources: HashMap<String,Boolean>, // Which sources to include
    val guards: HashMap<String,Boolean>, // If true, then guard clauses are used.
    var virtualization: HashMap<String,Boolean>, // If true, virtualization is used. Otherwise, naive method is used.
    var jenaReasoner: ReasonerMode, // Must be either off, rdfs or owl
    var fusekiModel: Model? = null // If given, then this model is used instead of the FusekiGraph
)

// Class managing triples from all the different sources, how to reason over them, and how to query them using SPARQL or DL queries.
class TripleManager(val settings: Settings, val staticTable: StaticTable, private val interpreter: Interpreter?) {
    private val prefixMap = settings.prefixMap()

    // Default settings. These can be changed with REPL commands.
    var currentTripleSettings = TripleSettings(
        sources = hashMapOf("heap" to true, "staticTable" to true, "vocabularyFile" to true, "fmos" to true, "externalOntology" to (settings.background != ""), "urlOntology" to (settings.tripleStore != "")),
        guards = hashMapOf("heap" to true, "staticTable" to true),
        virtualization = hashMapOf("heap" to true, "staticTable" to true, "fmos" to true),
        jenaReasoner = settings.reasoner,
        fusekiModel = null
    )


    // Main method used to deliver the Jena model to run SPARQL queries on.
    // When special settings are given, it will override the general settings
    fun getModel(specialSettings: TripleSettings = currentTripleSettings): Model {
        val model =  getModelUnionWithReasoning(specialSettings)

        // If the materialize flag is given, then write to file
        if (settings.materialize) {
            File(settings.outdir).mkdirs()
            File("${settings.outdir}/output.ttl").createNewFile()
            model.write(FileWriter("${settings.outdir}/output.ttl"),"TTL")
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
        if (settings.materialize) {
            File(settings.outdir).mkdirs()
            File("${settings.outdir}/output.ttl").createNewFile()
            model.write(FileWriter("${settings.outdir}/output.ttl"),"TTL")
        }
        val manager: OntologyManager = OntManagers.createManager()

        // Settings related to how model is loaded into an ontology.
        val conf: OntLoaderConfiguration = manager.ontologyLoaderConfiguration
        conf.isPerformTransformation = true

        return manager.addOntology(model.graph, conf)
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
        if (tripleSettings.sources.getOrDefault("urlOntology", false)) {
            if (tripleSettings.fusekiModel == null)
                tripleSettings.fusekiModel = getTripleStoreOntologyAsModel()
            includedGraphs.add(tripleSettings.fusekiModel!!.graph)
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

    private fun getTripleStoreOntologyAsModel(): Model {
        // Test case only
        if (testModel != null) return testModel as Model
        // Normal behaviour with a Fuseki environment
        return RDFConnectionFactory.connect(settings.tripleStore + "/data").fetch()
    }

    /**
     * Regenerate the triple store model. We'll do so by fetching again the data
     * This will be called when the triple store is updated, and we want to update the model.
     */
    fun regenerateTripleStoreModel() {
        currentTripleSettings.fusekiModel = getTripleStoreOntologyAsModel()
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
            ReasonerMode.off -> { return null }
            ReasonerMode.owl -> { return ReasonerRegistry.getOWLReasoner() }
            ReasonerMode.rdfs -> { return ReasonerRegistry.getRDFSReasoner() }
        }
    }

    // A custom type of (nice)iterator which takes a list as input and iterates over them.
    // It iterates through all elements in the list from start to end.
    private class TripleListIterator(private val tripleList: List<Triple>): NiceIterator<Triple>() {
        var listIndex: Int = 0  // index of next element

        override fun hasNext(): Boolean = listIndex < tripleList.size

        override fun next(): Triple = tripleList[(listIndex++)]
    }


    // If searchTriple matches candidateTriple, then candidateTriple will be added to matchList
    fun addIfMatch(candidateTriple: Triple?, searchTriple: Triple?, matchList: MutableList<Triple>, pseudo: Boolean)  {
        if (searchTriple == null) return
        if (candidateTriple == null) return
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
        File(settings.outdir).mkdirs()
        File("${settings.outdir}/output-naive.ttl").createNewFile()
        m1.write(FileWriter("${settings.outdir}/output-naive.ttl"),"TTL")

        // Read into model m2
        val m2 = ModelFactory.createDefaultModel()
        val uri = File("${settings.outdir}/output-naive.ttl").toURI().toURL().toString()
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
                val simulationObject = fmo.value
                var simulationURI = "${run}${name}"

                addIfMatch(uriTriple(simulationURI, "${rdf}type", "${smol}Simulation"), searchTriple, matchingTriples, false)
                addIfMatch(literalTriple(simulationURI, "${smol}loads", simulationObject.path, STRINGTYPE, settings), searchTriple, matchingTriples, false)
                addIfMatch(literalTriple(simulationURI, "${smol}time", simulationObject.time, DOUBLETYPE, settings), searchTriple, matchingTriples, false)
                addIfMatch(literalTriple(simulationURI, "${smol}pseudoOffset", simulationObject.pseudoOffset, DOUBLETYPE, settings), searchTriple, matchingTriples, false)
                addIfMatch(literalTriple(simulationURI, "${smol}role", simulationObject.role, STRINGTYPE, settings), searchTriple, matchingTriples, false)

                var simulator : Simulation = simulationObject.sim
                var modelDescription = simulator.modelDescription
                var simulatorURI = "${simulationURI}_simulator"
                var modelDescriptionURI = "${run}${name}_modelDescription"

                addIfMatch(uriTriple(simulationURI, "${smol}simulator", simulatorURI), searchTriple, matchingTriples, false)
                addIfMatch(uriTriple(simulatorURI, "${smol}modelDescription", modelDescriptionURI), searchTriple, matchingTriples, false)
                addIfMatch(literalTriple(modelDescriptionURI, "${smol}generatorTool", modelDescription.generationTool, STRINGTYPE, settings), searchTriple, matchingTriples, false)
                addIfMatch(literalTriple(modelDescriptionURI, "${smol}modelName", modelDescription.modelName, STRINGTYPE, settings), searchTriple, matchingTriples, false)

                for (v in modelDescription.getModelVariables()) {
                    var variableURI = "${run}${name}_var_${v.name}"

                    addIfMatch(uriTriple(modelDescriptionURI, "${smol}variable", variableURI), searchTriple, matchingTriples, false)
                    addIfMatch(literalTriple(variableURI, "${smol}variableName", v.name, STRINGTYPE, settings), searchTriple, matchingTriples, false)
                    addIfMatch(literalTriple(variableURI, "${smol}typeName", v.typeName, STRINGTYPE, settings), searchTriple, matchingTriples, false)
                    addIfMatch(literalTriple(variableURI, "${smol}causality", v.causality, STRINGTYPE, settings), searchTriple, matchingTriples, false)
                    addIfMatch(literalTriple(variableURI, "${smol}variability", v.variability, STRINGTYPE, settings), searchTriple, matchingTriples, false)
                    addIfMatch(literalTriple(variableURI, "${smol}valueReference", v.valueReference, INTTYPE, settings), searchTriple, matchingTriples, false)
                    addIfMatch(literalTriple(variableURI, "${smol}description", v.description, STRINGTYPE, settings), searchTriple, matchingTriples, false)
                }
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
                liftClass(classObj,
                    this@TripleManager,
                    searchTriple,
                    tripleSettings,
                    matchingTriples,
                    pseudo)
            }

            // Generate triples for all methods
            for(classObj in methodTable) {
                for (method in classObj.value) {
                    liftMethod(
                        method,
                        classObj.key,
                        this@TripleManager,
                        searchTriple,
                        matchingTriples,
                        pseudo
                    )
                }
            }

            // Generate triples for the class hierarchy
            val allClasses: MutableSet<String> = methodTable.keys.toMutableSet()
            for(classObj in hierarchy.entries){
                for(subClass in classObj.value){
                    addIfMatch(uriTriple("${prog}${subClass}", "${smol}subClass", "${prog}${classObj.key}"), searchTriple, matchingTriples, pseudo)
                    allClasses -= subClass
                }
            }

            // allClasses now only contains classes without any ancestors. They should be subclass of Object
            for(classObj in allClasses) addIfMatch(uriTriple("${prog}${classObj}", "${smol}subClass", "${prog}Object"), searchTriple, matchingTriples, pseudo)

            return TripleListIterator(matchingTriples)
        }
    }

    // Graph representing the heap
    private inner class HeapGraph(val tripleSettings: TripleSettings, interpreter: Interpreter, val pseudo: Boolean = false): GraphBase() {
        var interpreter: Interpreter = interpreter

        // Returns an iterator of all triples in the heap that matches searchTriple
        // graphBaseFind only constructs/fetches the triples that match searchTriple.
        override fun graphBaseFind(searchTriple: Triple): ExtendedIterator<Triple> {
            val useGuardClauses = false //tripleSettings.guards.getOrDefault("heap", true)
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
                if(staticTable.hiddenSet.contains(obj.tag.getPrimary().getNameString())) continue;

                val subjectString = "${run}${obj.literal}"

                // Guard clause. If this obj does not match to the subject of the search triple, then continue to the next obj
                if (useGuardClauses && searchTriple.subject is Node_URI && searchTriple.subject.nameSpace == run) {
                    if (searchTriple.subject.uri != subjectString) { continue }
                }

                addIfMatch(uriTriple(subjectString, "${rdf}type", "${owl}NamedIndividual"), searchTriple, matchingTriples, pseudo)
                addIfMatch(uriTriple(subjectString, "${rdf}type", "${smol}Object"), searchTriple, matchingTriples, pseudo)
                addIfMatch(uriTriple(subjectString, "${smol}implements", "${prog}${(obj.tag as BaseType).name}"), searchTriple, matchingTriples, pseudo)

                /** this code adds the rule triples directly to the KB */
                if(interpreter.staticInfo.methodTable[obj.tag.name] != null)
                    for (m in interpreter.staticInfo.methodTable[obj.tag.name]!!.entries.filter { it.value.isRule || it.value.isDomain })
                       liftRuleMethod(m, this@TripleManager, searchTriple, interpreter,obj, matchingTriples, pseudo, useGuardClauses)


                if(heap[obj]!!.containsKey("__models")) {
                    val modelString = heap[obj]!!.getOrDefault("__models", LiteralExpr("ERROR")).literal.removeSurrounding("\"")
                    val modelURI = settings.replaceKnownPrefixesNoColon(modelString)
                    addIfMatch(uriTriple(subjectString, "${domain}links", modelURI), searchTriple, matchingTriples, pseudo)
                }
                if(heap[obj]!!.containsKey("__describe"))
                    generateLinkage(this@TripleManager,searchTriple,interpreter,obj, matchingTriples,pseudo,heap,useGuardClauses)

                // Generating triples for all fields values
                for(store in heap[obj]!!.keys.filter { it != "__models" && it != "__describe" })
                    liftField(store,this@TripleManager,searchTriple,interpreter,obj,matchingTriples,pseudo,heap,useGuardClauses)

            }
            return TripleListIterator(matchingTriples)
        }
    }
}

fun uriTriple(s: String, p: String, o: String): Triple {
    return Triple(NodeFactory.createURI(s), NodeFactory.createURI(p), NodeFactory.createURI(o))
}

fun literalTriple(s: String, p: String, o: Any?, type: BaseType, settings: Settings): Triple? {
    if (o == null) return null
    return Triple(
        NodeFactory.createURI(s),
        NodeFactory.createURI(p),
        getLiteralNode(LiteralExpr(o.toString(), type), settings)
    )
}

// Given a LiteralExpr, return the correct type of node
fun getLiteralNode(target: LiteralExpr, settings: Settings): Node {
    val smol = settings.prefixMap()["smol"]
    val run = settings.prefixMap()["run"]
    return if (target.literal == "null") NodeFactory.createURI("${smol}null")
    else if (target.tag == ERRORTYPE || target.tag == STRINGTYPE) NodeFactory.createLiteral(target.literal.removeSurrounding("\""), XSDDatatype.XSDstring)
    else if (target.tag == INTTYPE) NodeFactory.createLiteral(target.literal, XSDDatatype.XSDinteger)
    else if (target.tag == BOOLEANTYPE) NodeFactory.createLiteral(target.literal.lowercase(Locale.getDefault()), XSDDatatype.XSDboolean)
    else if (target.tag == DOUBLETYPE) NodeFactory.createLiteral(target.literal, XSDDatatype.XSDdouble)
    else NodeFactory.createURI("${run}${target.literal}")
}