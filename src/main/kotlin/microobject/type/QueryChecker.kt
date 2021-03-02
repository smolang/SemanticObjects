package microobject.type

import antlr.microobject.gen.WhileParser
import microobject.main.Settings
import microobject.runtime.State
import microobject.runtime.StaticTable
import org.apache.jena.graph.Node_Concrete
import org.apache.jena.query.QueryFactory
import org.apache.jena.sparql.syntax.ElementGroup
import org.apache.jena.sparql.syntax.ElementPathBlock
import org.semanticweb.HermiT.Configuration
import org.semanticweb.HermiT.Reasoner
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.IRI
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl

class QueryChecker(
    private val settings: Settings,
    private val query: String,
    private val type: Type,
    private val ctx: WhileParser.Sparql_statementContext
    ) : TypeErrorLogger() {
    private val sparqlPrefix =
        """
                    PREFIX : <urn:>
                    PREFIX smol: <${settings.langPrefix}>
                    PREFIX prog: <${settings.progPrefix}>
                    PREFIX run: <${settings.runPrefix}>
                    PREFIX owl: <http://www.w3.org/2002/07/owl#> 
                    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> 
                    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> 
                    PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> 
                    PREFIX domain: <${settings.domainPrefix}> 
                """.trimIndent()


    private fun extractTypeClass() : String? {
        if(type !is ComposedType || (type is ComposedType && type.getPrimary() != BaseType("List")) ) {
            log("Access statements are only allowed to target List type variables with concrete parameter type", ctx)
            return null //only storing in lists
        }
        val inner = type.params.first()
        if(inner !is BaseType) {
            log("Access statements are only allowed to target List type variables with concrete parameter type", ctx)
            return null //simple classes only
        }
        return inner.toString()
    }

    fun type(staticTable: StaticTable) : Boolean{
        try {
            val test = settings.prefixes() + "\n"+ State.HEADER + "\n" + State.VOCAB + "\n" + settings.background + "\n" + State.MINIMAL + "\n" + staticTable.dumpClasses()
            val m = OWLManager.createOWLOntologyManager()
            val ontology = m.loadOntologyFromOntologyDocument(test.byteInputStream())

            val reasoner = Reasoner(Configuration(), ontology)

            val qString = if(extractQueryClass() != null) extractQueryClass()
                          else return false
            val tString = if(extractTypeClass() != null)extractTypeClass()
                          else return false

            val owlSub = OWLClassImpl(IRI.create(qString))
            val owlSup = OWLClassImpl(IRI.create(settings.progPrefix+tString))
            val axiom = OWLSubClassOfAxiomImpl(owlSub, owlSup, emptyList())
            return reasoner.isEntailed(axiom)
        } catch (e: Exception){
            println(e.message)
            return false
        }
    }

    private fun extractQueryClass() : String? {
        val toCheck = "$sparqlPrefix\n\n $query\n"
        if(toCheck.contains("%")) {
            log("%n constants are not supported yet", ctx)
            return null
        }

        val query = QueryFactory.create(toCheck)
        if(!query.isSelectType) {
            log("non-select queries are not supported yet", ctx)
            return null
        }

        if(query.projectVars.size != 1 || query.projectVars.first().name != "obj"){
            log("Queries must have a single extracted variable called ?obj", ctx)
            return null
        }

        val pattern = query.queryPattern
        if(pattern !is ElementGroup || pattern.elements.size != 1) {
            log("Only instance queries are supported", ctx)
            return null
        }
        val elem = pattern.elements.first()
        if(elem !is ElementPathBlock) {
            log("Only instance queries are supported", ctx)
            return null
        }
        val subpattern = elem.pattern
        if(subpattern.list.size != 1) {
            log("Only instance queries are supported", ctx)
            return null
        }
        val triple = subpattern.list.first().asTriple()
        if(triple.predicate !is Node_Concrete) {
            log("Only instance queries are supported", ctx)
            return null
        }
        val pred = triple.predicate.toString()
        if(pred != "http://www.w3.org/1999/02/22-rdf-syntax-ns#type") {
            log("Only instance queries are supported", ctx)
            return null
        }
        if(triple.`object` !is Node_Concrete&&triple.subject != query.projectVars.first()) {
            log("Only instance queries are supported", ctx)
            return null
        }
        return triple.`object`.toString()
    }
}