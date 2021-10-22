package no.uio.microobject.data

import com.github.owlcs.ontapi.OntManagers
import no.uio.microobject.data.*
import no.uio.microobject.main.Settings
import no.uio.microobject.runtime.*
import no.uio.microobject.type.*
import org.apache.commons.io.IOUtils
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.*
import org.apache.jena.graph.impl.GraphBase
import org.apache.jena.rdf.model.*
import org.apache.jena.reasoner.ReasonerRegistry
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner
import org.apache.jena.reasoner.rulesys.Rule
import org.apache.jena.util.iterator.*
import org.semanticweb.owlapi.model.*
import org.simpleframework.xml.stream.Verbosity
import java.io.*


// Class managing triples, models and ontologies based on all the data we consider
class TripleManager(settings : Settings, staticTable : StaticTable, interpreter : Interpreter?) {
    val settings : Settings = settings
    val staticTable : StaticTable = staticTable
    val interpreter : Interpreter? = interpreter
    val prefixMap = settings.prefixMap()


    // Get the ontology representing only the static data. This is used e.g. for type checking.
    fun getStaticDataOntology() : OWLOntology {
        // Using ONT-API to connect Jena with the OWLAPI
        var manager : OWLOntologyManager = OntManagers.createManager();
        var ontology : OWLOntology = manager.createOntology(IRI.create("${settings.langPrefix}ontology"));
        // This model corresponds to the ontology, so adding to the model also adds to ontology, if they are legal OWL axioms.
        var model : Model = (ontology as com.github.owlcs.ontapi.Ontology).asGraphModel();

        // Read vocab.owl and background data
        var allTriplesString : String = ""
        for ((key, value) in prefixMap) allTriplesString += "@prefix $key: <$value> .\n"
        val vocabURL : java.net.URL = this::class.java.classLoader.getResource("vocab.owl")
        allTriplesString += vocabURL.readText(Charsets.UTF_8) + "\n"
        val s : InputStream = ByteArrayInputStream(allTriplesString.toByteArray())
        model.read(s, null, "TTL")

        // Adding prefixes
        for ((key, value) in prefixMap) model.setNsPrefix(key, value)

        // write model to file if the materialize flag is given
        if (settings.materialize) {
            model.write(FileWriter("${settings.outpath}/output.ttl"),"TTL")
        }

        return ontology
    }


    // This returns an OWL ontology containing all OWL axioms.
    // The corresponding Jena model includes all RDF triples
    // Triples/axioms are gathered from: vocab.owl, background data, heap, static table
    // TODO: add simulation data
    fun getCompleteOntology() : OWLOntology {
        // Using ONT-API to connect Jena with the OWLAPI
        var manager : OWLOntologyManager = OntManagers.createManager();
        var ontology : OWLOntology = manager.createOntology(IRI.create("${settings.langPrefix}ontology"));
        // This model corresponds to the ontology, so adding to the model also adds to ontology, if they are legal OWL axioms.
        var model : Model = (ontology as com.github.owlcs.ontapi.Ontology).asGraphModel();

        // Add triples manually to model/ontology. Can be used when debugging.
        // var allTriplesStringPre : String = ""
        // for ((key, value) in prefixMap) allTriplesStringPre += "@prefix $key: <$value> .\n"
        // var triplesHardCoded = "run:obj6 a prog:Student ." +
        // "prog:Student owl:disjointWith prog:Course ."
        // allTriplesStringPre += triplesHardCoded
        // val sPre : InputStream = ByteArrayInputStream(allTriplesStringPre.toByteArray())
        // model.read(sPre, null, "TTL")

        // Read vocab.owl and background data
        var allTriplesString : String = ""
        for ((key, value) in prefixMap) allTriplesString += "@prefix $key: <$value> .\n"
        val vocabURL : java.net.URL = this::class.java.classLoader.getResource("vocab.owl")
        allTriplesString += vocabURL.readText(Charsets.UTF_8) + "\n"
        if(settings.background != "") allTriplesString += settings.background + "\n"
        val s : InputStream = ByteArrayInputStream(allTriplesString.toByteArray())
        model.read(s, null, "TTL")

        if (interpreter != null) {
            // Add triples from static table
            var staticTableGraphModel : Model = ModelFactory.createModelForGraph(StaticTableGraph(interpreter))
            model.add(staticTableGraphModel)

            // Add triples from heap
            var heapGraphModel : Model = ModelFactory.createModelForGraph(HeapGraph(interpreter))
            model.add(heapGraphModel)
        }

         //For debugging: Listing all OWL axioms if needed
         if(settings.verbose) {
             println("List of all owl-axioms in the complete model/ontology:");
             for (axiom in ontology.axioms()) println(axiom)
         }



        if (interpreter != null) {
            var rules = interpreter.rules
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
            File("${settings.outpath}").mkdirs()
            File("${settings.outpath}/output.ttl").createNewFile()
            model.write(FileWriter("${settings.outpath}/output.ttl"),"TTL")
        }

        return ontology
    }

    // Using ONT-API to return the model corresponding to the complete ontology
    fun getCompleteModel() : Model {
        val ontology : OWLOntology = getCompleteOntology()
        val model = (ontology as com.github.owlcs.ontapi.Ontology).asGraphModel()

        // Turn on reasoning if background knowledge is given.
        if(settings.background != "") {
            if(settings.verbose) println("Using background knowledge...")
            return ModelFactory.createInfModel(ReasonerRegistry.getOWLReasoner(), model)
        }
        return model
    }


    // Return the graph corresponding to the complete model
    fun getCompleteGraph() : Graph {
        return getCompleteModel().getGraph()
    }

}








// A custom type of (nice)iterator which takes a list as input and iterates over them.
// It iterates through all elements in the list from start to end.
class TripleListIterator(tripleList : List<Triple>) : NiceIterator<Triple>() {
    val tripleList : List<Triple> = tripleList
    var listIndex : Int = 0  // index of next element

    override public fun hasNext(): Boolean {
        if (listIndex < tripleList.size) return true
        return false
    }

    override public fun next(): Triple {
        this.listIndex = this.listIndex + 1
        return tripleList[(listIndex-1)]
    }
}

// Helper method to crate triple with URIs in all three positions
fun uriTriple(s : String, p : String, o : String) : Triple {
    return Triple(NodeFactory.createURI(s), NodeFactory.createURI(p), NodeFactory.createURI(o))
}

// If searchTriple matches candidateTriple, then candidateTriple will be added to matchList
fun addIfMatch(candidateTriple : Triple, searchTriple : Triple, matchList : MutableList<Triple> ) : Unit {
    if (searchTriple.matches(candidateTriple)) matchList.add(candidateTriple)
}

// Graph representing the static table
class StaticTableGraph(interpreter: Interpreter) : GraphBase() {
    var interpreter : Interpreter = interpreter

    // Returns an iterator of all triples in the static table that matches searchTriple
    // graphBaseFind only constructs the triples that match searchTriple.
    override protected fun graphBaseFind(searchTriple : Triple): ExtendedIterator<Triple> {
        val prefixMap : HashMap<String, String> = interpreter.prefixMap
        val fieldTable : Map<String,FieldEntry> = interpreter.staticInfo.fieldTable
        val methodTable : Map<String,Map<String,MethodInfo>> = interpreter.staticInfo.methodTable
        val hierarchy : MutableMap<String, MutableSet<String>> = interpreter.staticInfo.hierarchy

        // Prefixes
        val rdf = prefixMap.get("rdf")
        val rdfs = prefixMap.get("rdfs")
        val owl = prefixMap.get("owl")
        val prog = prefixMap.get("prog")
        val smol = prefixMap.get("smol")

        // Guard clause checking that the subject of the searchTriple starts with prog. Otherwise, return no triples.
        // This assumes that all triples generated by this method uses prog as the prefix for the subject.
        if (searchTriple.getSubject() is Node_URI){
            if (searchTriple.getSubject().getNameSpace() != prog) return TripleListIterator(mutableListOf<Triple>())
        }

        // Guard clause: checking if the predicate of the search triple is one of the given URIs
        if (searchTriple.getPredicate() is Node_URI){
            var possiblePredicates = mutableListOf("${rdf}type", "${smol}hasField", "${rdfs}domain", "${smol}hasMethod", "${rdfs}subClassOf")
            val anyEqual = possiblePredicates.any { it == searchTriple.getPredicate().getURI() }
            if (!anyEqual) return TripleListIterator(mutableListOf<Triple>())
        }

        // Guard clause: set of possible object prefixes it limited
        if (searchTriple.getObject() is Node_URI){
            var possibleObjectPrefixes = mutableListOf(smol, owl, prog)
            val anyEqual = possibleObjectPrefixes.any { it == searchTriple.getObject().getNameSpace() }
            if (!anyEqual) return TripleListIterator(mutableListOf<Triple>())
        }


        var matchingTriples : MutableList<Triple> = mutableListOf<Triple>()

        // Generate triples for classes and fields
        for(classObj in fieldTable){
            val className : String = classObj.key

            addIfMatch(uriTriple("${prog}${className}", "${rdf}type", "${smol}Class"), searchTriple, matchingTriples)
            addIfMatch(uriTriple("${prog}${className}", "${rdf}type", "${owl}Class" ), searchTriple, matchingTriples)

            for(fieldEntry in classObj.value){
                val fieldName : String = classObj.key+"_"+fieldEntry.name

                // Guard clause: Skip this fieldName when the subject of the search triple is different from both "${prog}${className}" and "${prog}$fieldName"
                if (searchTriple.getSubject() is Node_URI){
                    if (searchTriple.getSubject().getURI() != "${prog}${className}" && searchTriple.getSubject().getURI() != "${prog}$fieldName") continue
                }

                addIfMatch(uriTriple("${prog}${className}", "${smol}hasField", "${prog}${fieldName}"), searchTriple, matchingTriples)
                addIfMatch(uriTriple("${prog}${fieldName}", "${rdf}type", "${smol}Field"), searchTriple, matchingTriples)
                addIfMatch(uriTriple("${prog}${fieldName}", "${rdfs}domain", "${prog}${className}"), searchTriple, matchingTriples)

                if(fieldEntry.type == INTTYPE || fieldEntry.type == STRINGTYPE) {
                    addIfMatch(uriTriple("${prog}${fieldName}", "${rdf}type", "${owl}DatatypeProperty"), searchTriple, matchingTriples)
                } else {
                    addIfMatch(uriTriple("${prog}${fieldName}", "${rdf}type", "${owl}FunctionalProperty"), searchTriple, matchingTriples)
                    addIfMatch(uriTriple("${prog}${fieldName}", "${rdf}type", "${owl}ObjectProperty"), searchTriple, matchingTriples)
                }
            }
        }

        // Generate triples for all methods
        for(classObj in methodTable){
            for(method in classObj.value){
                val methodName : String = classObj.key+"_"+method.key
                addIfMatch(uriTriple("${prog}${classObj.key}", "${smol}hasMethod", "${prog}${methodName}"), searchTriple, matchingTriples)
                addIfMatch(uriTriple("${prog}${methodName}", "${rdf}type", "${owl}NamedIndividual"), searchTriple, matchingTriples)
                addIfMatch(uriTriple("${prog}${methodName}", "${rdf}type", "${smol}Method"), searchTriple, matchingTriples)
            }
        }

        // Generate triples for the class hierarchy
        var allClasses : Set<String> = methodTable.keys
        for(classObj in hierarchy.entries){
            for(subClass in classObj.value){
                addIfMatch(uriTriple("${prog}${subClass}", "${rdfs}subClassOf", "${prog}${classObj.key}"), searchTriple, matchingTriples)
                allClasses -= subClass
            }
        }
        // allClasses now only contains classes without any ancestors. They should be subclass of Object
        for(classObj in allClasses) addIfMatch(uriTriple("${prog}${classObj}", "${rdfs}subClassOf", "${prog}Object"), searchTriple, matchingTriples)

        return TripleListIterator(matchingTriples)
    }
}


// Graph representing the heap
class HeapGraph(interpreter: Interpreter) : GraphBase() {
    var interpreter : Interpreter = interpreter

    // Returns an iterator of all triples in the heap that matches searchTriple
    // graphBaseFind only constructs the triples that match searchTriple.
    override protected fun graphBaseFind(searchTriple : Triple): ExtendedIterator<Triple> {
        val settings : Settings = interpreter.settings
        val heap : GlobalMemory = interpreter.heap
        val prefixMap : HashMap<String, String> = interpreter.prefixMap

        // Prefixes
        val rdf = prefixMap.get("rdf")
        val owl = prefixMap.get("owl")
        val prog = prefixMap.get("prog")
        val smol = prefixMap.get("smol")
        val run = prefixMap.get("run")
        val domain = prefixMap.get("domain")

        // Guard clause checking that the subject of the searchTriple starts with "run:" or "domain:". Otherwise, return no triples.
        // This guard should be removed or changed if we change the triples we want to be generated from the heap.
        if (searchTriple.getSubject() is Node_URI){
            if (searchTriple.getSubject().getNameSpace() != run && searchTriple.getSubject().getNameSpace() != domain ) return TripleListIterator(mutableListOf<Triple>())
        }

        var matchingTriples : MutableList<Triple> = mutableListOf<Triple>()

        for(obj in heap.keys){
            val subjectString : String = "${run}${obj.literal}"

            // Guard clause. If this obj does not match to the subject of the search triple, then continue to the next obj
            if (searchTriple.getSubject() is Node_URI){
                if (searchTriple.getSubject().getNameSpace() == run) {
                    if (searchTriple.getSubject().getURI() != subjectString) continue
                }
            }

            addIfMatch(uriTriple(subjectString, "${rdf}type", "${owl}NamedIndividual"), searchTriple, matchingTriples)
            addIfMatch(uriTriple(subjectString, "${rdf}type", "${smol}Object"), searchTriple, matchingTriples)
            addIfMatch(uriTriple(subjectString, "${rdf}type", "${prog}${(obj.tag as BaseType).name}"), searchTriple, matchingTriples)

            // Generating triples for all fields values
            for(store in heap[obj]!!.keys) {

                if (store == "__models") {
                    // Connect object to a model
                    val modelString = heap[obj]!!.getOrDefault(store, LiteralExpr("ERROR")).literal.removeSurrounding("\"")
                    val modelURI = settings.replaceKnownPrefixesNoColon(modelString)
                    addIfMatch(uriTriple(subjectString, "${domain}models", modelURI), searchTriple, matchingTriples)
                }
                else if (store == "__describe") {
                    // Connect model to the description
                    val description : String = heap[obj]!!.getOrDefault(store, LiteralExpr("ERROR")).literal

                    // Guard on the subject of the description.
                    // If the first string in the description (which equals the URI of the model) does not match the searchTriple subject, then continue to the next store
                    val modelURI : String = settings.replaceKnownPrefixesNoColon(description.split(" ")[0])
                    if (searchTriple.getSubject() is Node_URI){
                        if (searchTriple.getSubject().getURI() != modelURI) continue
                    }

                    // Parse and load the description into a jena model.
                    var extendedDescription : String = ""
                    for ((key, value) in prefixMap) extendedDescription += "@prefix $key: <$value> .\n"
                    extendedDescription += description
                    val m : Model = ModelFactory.createDefaultModel().read(IOUtils.toInputStream(extendedDescription, "UTF-8"), null, "TTL");
                    // Consider each triple and add it if it matches the search triple.
                    for (st in m.listStatements()) addIfMatch(st.asTriple(), searchTriple, matchingTriples)
                }
                else {
                    // Generate triples for each of the fields of the object.
                    val predicateString : String = "${prog}${obj.tag}_${store}"

                    // Guard on the predicate. If the current predicate does not match the predicate of the search triple, then continue to the next store
                    if (searchTriple.getPredicate() is Node_URI){
                        if (searchTriple.getPredicate().getURI() != predicateString) continue
                    }

                    // TODO: For some reason ints are not displayed with the correct datatype when dumped or validated.
                    // We need to go over this section once more to make sure that ints, strings etc. are managed correctly.
                    val target : LiteralExpr = heap[obj]!!.getOrDefault(store, LiteralExpr("ERROR"))
                    if (target.literal == "null") {
                        val candidateTriple : Triple = Triple(NodeFactory.createURI(subjectString), NodeFactory.createURI(predicateString), NodeFactory.createURI("${smol}null") )
                        addIfMatch(candidateTriple, searchTriple, matchingTriples)
                    }
                    else if (target.tag == ERRORTYPE || target.tag == STRINGTYPE) {
                        val candidateTriple : Triple = Triple(NodeFactory.createURI(subjectString), NodeFactory.createURI(predicateString), NodeFactory.createLiteral(target.literal.removeSurrounding("\""), XSDDatatype.XSDstring) )
                        addIfMatch(candidateTriple, searchTriple, matchingTriples)
                    }
                    else if (target.tag == INTTYPE) {
                        val candidateTriple : Triple = Triple(NodeFactory.createURI(subjectString), NodeFactory.createURI(predicateString), NodeFactory.createLiteral("${target.literal}", XSDDatatype.XSDinteger) )
                        addIfMatch(candidateTriple, searchTriple, matchingTriples)
                    }
                    else {
                        val candidateTriple : Triple = uriTriple(subjectString, predicateString, "${run}${target.literal}")
                        addIfMatch(candidateTriple, searchTriple, matchingTriples)
                    }
                }
            }
        }
        return TripleListIterator(matchingTriples)
    }
}

















