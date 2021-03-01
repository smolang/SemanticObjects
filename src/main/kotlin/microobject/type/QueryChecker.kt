package microobject.type

import microobject.main.Settings
import microobject.runtime.State
import microobject.runtime.StaticTable
import org.apache.jena.graph.Node_Concrete
import org.apache.jena.query.QueryFactory
import org.apache.jena.sparql.core.PathBlock
import org.apache.jena.sparql.syntax.ElementGroup
import org.apache.jena.sparql.syntax.ElementPathBlock
import org.semanticweb.HermiT.Configuration
import org.semanticweb.HermiT.Reasoner
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLClass
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl

class QueryChecker(private val settings: Settings, private val query: String, private val type:Type) {
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


    fun extractTypeClass() : String? {
        if(type !is ComposedType || (type is ComposedType && type.getPrimary() != BaseType("List")) )
            return null //only storing in lists
        val inner = type.params.first()
        if(inner !is BaseType)
            return null //simple classes only
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
            val owlSup = OWLClassImpl(IRI.create(settings.progPrefix+":"+tString))
            val axiom = OWLSubClassOfAxiomImpl(owlSub, owlSup, emptyList())
            return reasoner.isEntailed(axiom)
        } catch (e: Exception){
            println(e.message)
            return false
        }
    }

    fun extractQueryClass() : String? {
        val toCheck = "$sparqlPrefix\n\n $query\n"
        if(toCheck.contains("%")) return null   // No checks for constants

        val query = QueryFactory.create(toCheck)
        if(!query.isSelectType) return null            //select only

        if(query.projectVars.size != 1) return null
        if(query.projectVars.first().name != "obj") return null

        val pattern = query.queryPattern
        if(pattern !is ElementGroup || pattern.elements.size != 1) return null
        val elem = pattern.elements.first()
        if(elem !is ElementPathBlock) return null
        val subpattern = elem.pattern
        if(subpattern.list.size != 1) return null
        val triple = subpattern.list.first().asTriple()
        if(triple.predicate !is Node_Concrete) return null
        val pred = triple.predicate.toString()
        if(triple.predicate.toString() != "http://www.w3.org/1999/02/22-rdf-syntax-ns#type") return null
        if(triple.`object` !is Node_Concrete)
            if(triple.subject != query.projectVars.first()) return null
        return triple.`object`.toString()
    }
}