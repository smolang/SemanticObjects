@file:Suppress("unused", "CanBeParameter")

package microobject.type

import antlr.microobject.gen.WhileParser
import microobject.main.Settings
import microobject.runtime.State
import microobject.runtime.StaticTable
import org.apache.jena.query.Query
import org.apache.jena.query.QueryFactory
import org.apache.jena.sparql.syntax.ElementGroup
import org.apache.jena.sparql.syntax.ElementPathBlock
import org.semanticweb.HermiT.Configuration
import org.semanticweb.HermiT.Reasoner
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserImpl
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLClassExpression
import org.semanticweb.owlapi.model.OntologyConfigurator
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl

data class DLNode(val str : String, val isVar : Boolean)
data class DLEdge(val from : DLNode, val label : String, val to : DLNode)

class DLGraph(val query : Query, val logger : TypeErrorLogger, val ctx: WhileParser.Sparql_statementContext){


}


class QueryChecker(
    private val settings: Settings,
    private val query: String,
    private val type: Type,
    private val ctx: WhileParser.Sparql_statementContext
) : TypeErrorLogger()  {

    val incidence : MutableMap<DLNode, MutableSet<DLEdge>> = mutableMapOf()
    var formula = ""

    fun type(staticTable: StaticTable) : Boolean{
        val successBuild = buildTree()
        if(!successBuild) {
            log("Building the tree for the query failed", ctx)
            return false
        }
        val successQuery = buildFormula()
        if(!successQuery){
            log("Building the tree for the query failed", ctx)
            return false
        }
        return check(staticTable)
    }



    private fun check(staticTable: StaticTable) : Boolean{
        try {
            val test = settings.prefixes() + "\n"+ State.HEADER + "\n" + State.VOCAB + "\n" + settings.background + "\n" + State.MINIMAL + "\n" + staticTable.dumpClasses()
            val m = OWLManager.createOWLOntologyManager()
            val ontology = m.loadOntologyFromOntologyDocument(test.byteInputStream())

            val reasoner = Reasoner(Configuration(), ontology)

            val tString = if(extractTypeClass() != null) extractTypeClass()
            else {
                log("Failed to extract OWL expression for target type", ctx)
                return false
            }
            val owlSub = getQueryExpression(staticTable)
            if(owlSub != null) {
                val owlSup = OWLClassImpl(IRI.create(settings.progPrefix + tString))
                val subs = reasoner.getSuperClasses(owlSub)
                val res = subs.containsEntity(owlSup)
                if(!res){
                    log("Could not check query $query: specified type is $type, but inferred supertypes are $subs", ctx)
                }
                return res
            } else {
                log("Failed to extract OWL expression for query", ctx)
                return false
            }
        } catch (e: Exception){
            log("Failed to typecheck query (Exception: ${e.message}) ", ctx)
            return false
        }
    }

    private fun getQueryExpression(staticTable: StaticTable) : OWLClassExpression?{
        val test = settings.prefixes() + "\n"+ State.HEADER + "\n" + State.VOCAB + "\n" + settings.background + "\n" + State.MINIMAL + "\n" + staticTable.dumpClasses()
        try {
            val out = settings.replaceKnownPrefixes(formula)
            val m = OWLManager.createOWLOntologyManager()
            val ontology = m.loadOntologyFromOntologyDocument(test.byteInputStream())
            val parser = ManchesterOWLSyntaxParserImpl(OntologyConfigurator(), m.owlDataFactory)
            parser.setDefaultOntology(ontology)
            return parser.parseClassExpression(out)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun extractTypeClass() : String? {
        if(type !is ComposedType || type.getPrimary() != BaseType("List") ) {
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

    private fun build(current : DLNode, seenVars : MutableSet<DLNode>) : String? {
        var ret = "owl:Thing"
        if(seenVars.contains(current)) return null
        seenVars.add(current)
        if(!current.isVar) return current.str.substring(0,current.str.indexOfFirst { it == '^' }).removeSurrounding("\"")
        val next = incidence.getOrDefault(current, mutableSetOf())

        for(n in next){
            var nextString = ""
            if(n.label == "a"){
                nextString = "<${n.to.str}>"
            } else if(n.from == current && !seenVars.contains(n.to) && n.to.isVar){
                nextString = "(<${n.label}> SOME ${build(n.to, seenVars)})"
            } else if(n.from == current && !seenVars.contains(n.to) && !n.to.isVar){
                nextString = "(<${n.label}> VALUE ${build(n.to, seenVars)})"
            } else if(n.from != current && !seenVars.contains(n.from) && n.from.isVar){
                nextString = "(inverse(<${n.label}>) SOME ${build(n.to, seenVars)})"
            } else if(n.from != current && !seenVars.contains(n.from) && !n.from.isVar){
                nextString = "(inverse(<${n.label}>) VALUE ${build(n.to, seenVars)})"
            }
            ret = if(ret == "owl:Thing") if( nextString != "") nextString else ret else "$ret AND $nextString"
        }
        return ret
    }

    private fun buildFormula() : Boolean {
        val ret = build(DLNode("obj", true), mutableSetOf())
        if(ret != null){
            formula = ret
        }
        return ret != null
    }

    private fun buildTree() : Boolean {

        val toCheck = "$sparqlPrefix\n\n $query\n"
        if(toCheck.contains("%")) {
            log("%n constants are not supported yet", ctx)
            return false
        }

        val query = QueryFactory.create(toCheck)
        if(!query.isSelectType) {
            log("non-select queries are not supported yet", ctx)
            return false
        }

        if(query.projectVars.size != 1 || query.projectVars.first().name != "obj"){
            log("Queries must have a single extracted variable called ?obj", ctx)
            return false
        }



        val pattern = query.queryPattern
        if(pattern !is ElementGroup || pattern.elements.size != 1) {
            log("This kind of query is not supported", ctx)
            return false
        }
        val elem = pattern.elements.first()
        if(elem !is ElementPathBlock) {
            log("This kind of query is not supported", ctx)
            return false
        }

        for(f in elem.pattern.list){
            if(!f.isTriple){
                log("This kind of query is not supported", ctx)
                return false
            }
            val sub  = f.subject
            val predicate = f.predicate
            val obj  = f.`object`
            if( predicate.toString() == "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" && sub.isVariable && !obj.isVariable){
                val subNode = DLNode(sub.name, true)
                val objNode = DLNode(obj.toString(), false)
                val edge = DLEdge(subNode, "a", objNode)
                val old = incidence.getOrDefault(subNode, mutableSetOf())
                old.add(edge)
                incidence[subNode] = old
            } else if( !predicate.isVariable && sub.isVariable && (obj.isVariable || obj.isLiteral)){
                val subNode = DLNode(sub.name, true)
                val objNode = if(obj.isVariable) DLNode(obj.name, obj.isVariable) else DLNode(obj.toString(), obj.isVariable)
                val edge = DLEdge(subNode, predicate.toString(), objNode)
                val old = incidence.getOrDefault(subNode, mutableSetOf())
                old.add(edge)
                incidence[subNode] = old
                if(obj.isVariable){
                    val old2 = incidence.getOrDefault(objNode, mutableSetOf())
                    old2.add(edge)
                    incidence[objNode] = old2
                }
            } else {
                log("This kind of query is not supported", ctx)
                return false
            }
        }
        return true
    }



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
}