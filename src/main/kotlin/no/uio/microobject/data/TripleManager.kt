package no.uio.microobject.data

import com.github.owlcs.ontapi.OntManagers
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
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.reasoner.ReasonerRegistry
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner
import org.apache.jena.reasoner.rulesys.Rule
import org.apache.jena.util.iterator.ExtendedIterator
import org.apache.jena.util.iterator.NiceIterator
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager


// Class managing triples, models and ontologies based on all the data we consider
class TripleManager(val settings : Settings, val staticTable : StaticTable, val interpreter : Interpreter?) {
    val prefixMap = settings.prefixMap()


    // Get the ontology representing only the static data. This is used e.g. for type checking.
    fun getStaticDataOntology() : OWLOntology {
        // Using ONT-API to connect Jena with the OWLAPI
        val manager : OWLOntologyManager = OntManagers.createManager()
        val ontology : OWLOntology = manager.createOntology(IRI.create("${settings.langPrefix}ontology"))
        // This model corresponds to the ontology, so adding to the model also adds to ontology, if they are legal OWL axioms.
        val model : Model = (ontology as com.github.owlcs.ontapi.Ontology).asGraphModel()

        // Read vocab.owl and background data
        var allTriplesString  = ""
        for ((key, value) in prefixMap) allTriplesString += "@prefix $key: <$value> .\n"
        val vocabURL : java.net.URL = this::class.java.classLoader.getResource("vocab.owl")
        allTriplesString += vocabURL.readText(Charsets.UTF_8) + "\n"
        if(settings.background != "") allTriplesString += settings.background + "\n"
        val s : InputStream = ByteArrayInputStream(allTriplesString.toByteArray())
        model.read(s, null, "TTL")

        // Adding prefixes
        for ((key, value) in prefixMap) model.setNsPrefix(key, value)

        // Add triples from static table
        val staticTableGraphModel: Model = ModelFactory.createModelForGraph(StaticTableGraph(staticTable, settings))
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


    // This returns an OWL ontology containing all OWL axioms.
    // The corresponding Jena model includes all RDF triples
    // Triples/axioms are gathered from: vocab.owl, background data, heap, static table
    // TODO: add simulation data
    fun getCompleteOntology() : OWLOntology {
        // Using ONT-API to connect Jena with the OWLAPI
        val manager : OWLOntologyManager = OntManagers.createManager()
        val ontology : OWLOntology = manager.createOntology(IRI.create("${settings.langPrefix}ontology"))
        // This model corresponds to the ontology, so adding to the model also adds to ontology, if they are legal OWL axioms.
        var model : Model = (ontology as com.github.owlcs.ontapi.Ontology).asGraphModel()

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

        val staticTableGraphModel : Model = ModelFactory.createModelForGraph(StaticTableGraph(staticTable, settings))
        model.add(staticTableGraphModel)

        if (interpreter != null) {
            // Add triples from heap
            val heapGraphModel : Model = ModelFactory.createModelForGraph(HeapGraph(interpreter))
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
        return getCompleteModel().graph
    }

}








// A custom type of (nice)iterator which takes a list as input and iterates over them.
// It iterates through all elements in the list from start to end.
class TripleListIterator(tripleList : List<Triple>) : NiceIterator<Triple>() {
    val tripleList : List<Triple> = tripleList
    var listIndex : Int = 0  // index of next element

    override fun hasNext(): Boolean {
        if (listIndex < tripleList.size) return true
        return false
    }

    override fun next(): Triple {
        this.listIndex = this.listIndex + 1
        return tripleList[(listIndex-1)]
    }
}

// Helper method to crate triple with URIs in all three positions
fun uriTriple(s : String, p : String, o : String) : Triple {
    return Triple(NodeFactory.createURI(s), NodeFactory.createURI(p), NodeFactory.createURI(o))
}

// If searchTriple matches candidateTriple, then candidateTriple will be added to matchList
fun addIfMatch(candidateTriple : Triple, searchTriple : Triple, matchList : MutableList<Triple>, pseudo: Boolean)  {
    if (searchTriple.matches(candidateTriple) && !pseudo) matchList.add(candidateTriple)
}

// Graph representing the static table
// If pseudo is set, we always return all triples. This is needed for type checkng, where graphBaseFind is not called
class StaticTableGraph(val staticInfo: StaticTable, val settings: Settings, val pseudo : Boolean = false) : GraphBase() {

    // Returns an iterator of all triples in the static table that matches searchTriple
    // graphBaseFind only constructs the triples that match searchTriple.
    override fun graphBaseFind(searchTriple : Triple): ExtendedIterator<Triple> {
        val prefixMap : HashMap<String, String> = settings.prefixMap()
        val fieldTable : Map<String,FieldEntry> = staticInfo.fieldTable
        val methodTable : Map<String,Map<String,MethodInfo>> = staticInfo.methodTable
        val hierarchy : MutableMap<String, MutableSet<String>> = staticInfo.hierarchy

        // Prefixes
        val rdf = prefixMap["rdf"]
        val rdfs = prefixMap["rdfs"]
        val owl = prefixMap["owl"]
        val prog = prefixMap["prog"]
        val smol = prefixMap["smol"]

        // Guard clause checking that the subject of the searchTriple starts with prog. Otherwise, return no triples.
        // This assumes that all triples generated by this method uses prog as the prefix for the subject.
        if (searchTriple.subject is Node_URI){
            if (searchTriple.subject.nameSpace != prog) return TripleListIterator(mutableListOf())
        }

        // Guard clause: checking if the predicate of the search triple is one of the given URIs
        if (searchTriple.predicate is Node_URI){
            val possiblePredicates = mutableListOf("${rdf}type", "${smol}hasField", "${rdfs}domain", "${smol}hasMethod", "${rdfs}subClassOf")
            val anyEqual = possiblePredicates.any { it == searchTriple.predicate.uri }
            if (!anyEqual) return TripleListIterator(mutableListOf())
        }

        // Guard clause: set of possible object prefixes it limited
        if (searchTriple.getObject() is Node_URI){
            val possibleObjectPrefixes = mutableListOf(smol, owl, prog)
            val anyEqual = possibleObjectPrefixes.any { it == searchTriple.getObject().nameSpace }
            if (!anyEqual) return TripleListIterator(mutableListOf())
        }


        val matchingTriples : MutableList<Triple> = mutableListOf()

        // Generate triples for classes and fields
        for(classObj in fieldTable){
            val className : String = classObj.key

            addIfMatch(uriTriple("${prog}${className}", "${rdf}type", "${smol}Class"), searchTriple, matchingTriples, pseudo)
            addIfMatch(uriTriple("${prog}${className}", "${rdf}type", "${owl}Class" ), searchTriple, matchingTriples, pseudo)
            addIfMatch(uriTriple("${prog}${className}", "${rdfs}subClassOf", "${prog}Object" ), searchTriple, matchingTriples, pseudo)

            for(fieldEntry in classObj.value){
                val fieldName : String = classObj.key+"_"+fieldEntry.name

                // Guard clause: Skip this fieldName when the subject of the search triple is different from both "${prog}${className}" and "${prog}$fieldName"
                if (searchTriple.subject is Node_URI){
                    if (searchTriple.subject.uri != "${prog}${className}" && searchTriple.subject.uri != "${prog}$fieldName") continue
                }

                addIfMatch(uriTriple("${prog}${className}", "${smol}hasField", "${prog}${fieldName}"), searchTriple, matchingTriples, pseudo)
                addIfMatch(uriTriple("${prog}${fieldName}", "${rdf}type", "${smol}Field"), searchTriple, matchingTriples, pseudo)
                addIfMatch(uriTriple("${prog}${fieldName}", "${rdfs}domain", "${prog}${className}"), searchTriple, matchingTriples, pseudo)

                if(fieldEntry.type == INTTYPE ) {
                    addIfMatch(uriTriple("${prog}${fieldName}", "${rdf}type", "${owl}DatatypeProperty"), searchTriple, matchingTriples, pseudo)
                    addIfMatch(uriTriple("${prog}${fieldName}", "${rdfs}range", XSDDatatype.XSDinteger.uri), searchTriple, matchingTriples, pseudo)
                } else if(fieldEntry.type == STRINGTYPE) {
                    addIfMatch(uriTriple("${prog}${fieldName}", "${rdf}type", "${owl}DatatypeProperty"), searchTriple, matchingTriples, pseudo)
                    addIfMatch(uriTriple("${prog}${fieldName}", "${rdfs}range", XSDDatatype.XSDstring.uri), searchTriple, matchingTriples, pseudo)
                } else {
                    addIfMatch(uriTriple("${prog}${fieldName}", "${rdf}type", "${owl}FunctionalProperty"), searchTriple, matchingTriples, pseudo)
                    addIfMatch(uriTriple("${prog}${fieldName}", "${rdf}type", "${owl}ObjectProperty"), searchTriple, matchingTriples, pseudo)
                    addIfMatch(uriTriple("${prog}${fieldName}", "${rdfs}range", "${prog}${fieldEntry.type}"), searchTriple, matchingTriples, pseudo)
                }
            }
        }

        // Generate triples for all methods
        for(classObj in methodTable){
            for(method in classObj.value){
                val methodName : String = classObj.key+"_"+method.key
                addIfMatch(uriTriple("${prog}${classObj.key}", "${smol}hasMethod", "${prog}${methodName}"), searchTriple, matchingTriples, pseudo)
                addIfMatch(uriTriple("${prog}${methodName}", "${rdf}type", "${owl}NamedIndividual"), searchTriple, matchingTriples, pseudo)
                addIfMatch(uriTriple("${prog}${methodName}", "${rdf}type", "${smol}Method"), searchTriple, matchingTriples, pseudo)
            }
        }

        // Generate triples for the class hierarchy
        val allClasses : MutableSet<String> = methodTable.keys.toMutableSet()
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
class HeapGraph(interpreter: Interpreter, val pseudo : Boolean = false) : GraphBase() {
    var interpreter : Interpreter = interpreter

    // Returns an iterator of all triples in the heap that matches searchTriple
    // graphBaseFind only constructs the triples that match searchTriple.
    override fun graphBaseFind(searchTriple : Triple): ExtendedIterator<Triple> {
        val settings : Settings = interpreter.settings
        val heap : GlobalMemory = interpreter.heap

        // Prefixes
        val rdf = interpreter.settings.prefixMap()["rdf"]
        val owl = interpreter.settings.prefixMap()["owl"]
        val prog = interpreter.settings.prefixMap()["prog"]
        val smol = interpreter.settings.prefixMap()["smol"]
        val run = interpreter.settings.prefixMap()["run"]
        val domain = interpreter.settings.prefixMap()["domain"]

        // Guard clause checking that the subject of the searchTriple starts with "run:" or "domain:". Otherwise, return no triples.
        // This guard should be removed or changed if we change the triples we want to be generated from the heap.
        if (searchTriple.subject is Node_URI){
            if (searchTriple.subject.nameSpace != run && searchTriple.subject.nameSpace != domain ) return TripleListIterator(mutableListOf())
        }

        val matchingTriples : MutableList<Triple> = mutableListOf()

        for(obj in heap.keys){
            val subjectString : String = "${run}${obj.literal}"

            // Guard clause. If this obj does not match to the subject of the search triple, then continue to the next obj
            if (searchTriple.subject is Node_URI){
                if (searchTriple.subject.nameSpace == run) {
                    if (searchTriple.subject.uri != subjectString) continue
                }
            }

            addIfMatch(uriTriple(subjectString, "${rdf}type", "${owl}NamedIndividual"), searchTriple, matchingTriples, pseudo)
            addIfMatch(uriTriple(subjectString, "${rdf}type", "${smol}Object"), searchTriple, matchingTriples, pseudo)
            addIfMatch(uriTriple(subjectString, "${rdf}type", "${prog}${(obj.tag as BaseType).name}"), searchTriple, matchingTriples, pseudo)

            /** this code adds the rule triple directly to the KB */
            if(interpreter.staticInfo.methodTable[obj.tag.name] != null)
                for (m in interpreter.staticInfo.methodTable[obj.tag.name]!!.entries) {
                    var retVal : Pair<LiteralExpr, LiteralExpr>? = null
                    if (m.value.isRule) {
                        retVal = interpreter.evalCall(obj.literal, obj.tag.name, m.key)
                        val resNode = getLiteralNode(retVal.second, settings)
                        val resTriple =
                            Triple(
                                NodeFactory.createURI( settings.replaceKnownPrefixesNoColon("run:${obj.literal}")),
                                NodeFactory.createURI( settings.replaceKnownPrefixesNoColon("prog:${m.value.declaringClass}_${m.key}_builtin_res")),
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

                        if(retVal == null) retVal = interpreter.evalCall(obj.literal, obj.tag.name, m.key)
                        val resNode = getLiteralNode(retVal.second, settings)
                        val resTriple =
                            Triple(
                                NodeFactory.createURI(models),
                                NodeFactory.createURI("domain:${m.value.declaringClass}_${m.key}_builtin_res"),
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
                    val description : String = heap[obj]!!.getOrDefault(store, LiteralExpr("ERROR")).literal

                    // Guard on the subject of the description.
                    // If the first string in the description (which equals the URI of the model) does not match the searchTriple subject, then continue to the next store
                    val modelURI : String = settings.replaceKnownPrefixesNoColon(description.split(" ")[0])
                    if (searchTriple.subject is Node_URI){
                        if (searchTriple.subject.uri != modelURI) continue
                    }

                    // Parse and load the description into a jena model.
                    var extendedDescription : String = ""
                    for ((key, value) in interpreter.settings.prefixMap()) extendedDescription += "@prefix $key: <$value> .\n"
                    extendedDescription += description
                    val m : Model = ModelFactory.createDefaultModel().read(IOUtils.toInputStream(extendedDescription, "UTF-8"), null, "TTL")
                    // Consider each triple and add it if it matches the search triple.
                    for (st in m.listStatements()) addIfMatch(st.asTriple(), searchTriple, matchingTriples, pseudo)
                }
                else {
                    // Generate triples for each of the fields of the object.
                    val predicateString : String = "${prog}${obj.tag}_${store}"

                    // Guard on the predicate. If the current predicate does not match the predicate of the search triple, then continue to the next store
                    if (searchTriple.predicate is Node_URI){
                        if (searchTriple.predicate.uri != predicateString) continue
                    }

                    val target : LiteralExpr = heap[obj]!!.getOrDefault(store, LiteralExpr("ERROR"))
                    val candidateTriple : Triple = Triple(NodeFactory.createURI(subjectString), NodeFactory.createURI(predicateString), getLiteralNode(target, settings))
                    addIfMatch(candidateTriple, searchTriple, matchingTriples, pseudo)
                }
            }
        }
        return TripleListIterator(matchingTriples)
    }
}


// Given a LiteralExpr, return the correct type of node
fun getLiteralNode(target : LiteralExpr, settings : Settings) : Node {
    val smol = settings.prefixMap()["smol"]
    val run = settings.prefixMap()["run"]
    if (target.literal == "null") return NodeFactory.createURI("${smol}null")
    else if (target.tag == ERRORTYPE || target.tag == STRINGTYPE) return NodeFactory.createLiteral(target.literal.removeSurrounding("\""), XSDDatatype.XSDstring)
    else if (target.tag == INTTYPE) return NodeFactory.createLiteral(target.literal, XSDDatatype.XSDinteger)
    else if (target.tag == BOOLEANTYPE) return NodeFactory.createLiteral(target.literal.toLowerCase(), XSDDatatype.XSDboolean)
    else if (target.tag == DOUBLETYPE) return NodeFactory.createLiteral(target.literal, XSDDatatype.XSDdouble)
    else return NodeFactory.createURI("${run}${target.literal}")

}
