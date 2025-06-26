@file:Suppress("unused", "CanBeParameter")

package no.uio.microobject.type

import com.github.owlcs.ontapi.owlapi.objects.entity.OWLObjectPropertyImpl
import no.uio.microobject.antlr.WhileParser
import no.uio.microobject.data.TripleManager
import no.uio.microobject.main.Settings
import org.apache.jena.graph.Node
import org.apache.jena.query.Query
import org.apache.jena.query.QueryFactory
import org.apache.jena.sparql.syntax.Element
import org.apache.jena.sparql.syntax.ElementGroup
import org.apache.jena.sparql.syntax.ElementPathBlock
import org.semanticweb.HermiT.Configuration
import org.semanticweb.HermiT.Reasoner
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserImpl
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLAnnotation
import org.semanticweb.owlapi.model.OWLAxiom
import org.semanticweb.owlapi.model.OWLClassExpression
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OntologyConfigurator
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl
import uk.ac.manchester.cs.owl.owlapi.OWLObjectSomeValuesFromImpl
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl
import java.util.HashSet

data class Edge(val label : String, val inverted : Boolean, var next : DL)

data class DL(val name: String, val tree : MutableList<Edge>, val origin : Node?){
    fun find(name: String) : DL? {
        if (name == this.name) return this
        return tree.firstOrNull { it.next.find(name) != null }?.next
    }
    fun contains(dl:DL) : Boolean{
        return this == dl || tree.any { it.next.contains(dl) }
    }
    fun collectNames() : Set<String> {
        if(!name.startsWith("?")) return setOf() //case: this is a literal
        return tree.fold(setOf(name), {x, nx -> x + nx.next.collectNames()})
    }
    fun merge(dl : DL)  {
        val names = dl.collectNames().intersect(collectNames())
        val dlHere = find(names.first())!!
        val dlThere = dl.find(names.first())!!

        invertNewNode(dl, dlThere)
        dlHere.tree.addAll(dlThere.tree)
    }
    fun add(dl : DL){
        if(dl.name == this.name)
            tree.addAll(dl.tree)
        else
            tree.forEach { it.next.add(dl) }
    }
}



fun invertNewNode(root: DL, newRoot : DL){
    if(root == newRoot) return
    if(!root.collectNames().contains(newRoot.name)) return
    val child = root.tree.first { it.next.contains(newRoot) }
    invertNewNode(child.next, newRoot)
    root.tree.remove(child)
    child.next.tree.add(Edge(child.label, true, root))
}



class QueryChecker(
    private val settings: Settings,
    private val query: String,
    private val type: Type,
    private val ctx: WhileParser.StatementContext,
    private val varName : String
) : TypeErrorLogger()  {

    private var formula = ""

    fun type(tripleManager: TripleManager) : Boolean{
        val successBuild = buildTree()
        if(!successBuild) {
            log("Building the tree or formula for the query failed", ctx, Severity.WARNING)
            return false
        }
        return check(tripleManager)
    }

    fun getOntologyNoHeap(tripleManager : TripleManager) : OWLOntology{
        val save = tripleManager.currentTripleSettings.sources["heap"]
        tripleManager.currentTripleSettings.sources["heap"] = false
        val ontology = tripleManager.getOntology()
        tripleManager.currentTripleSettings.sources["heap"] = save == true
        return ontology
    }


    private fun check(tripleManager: TripleManager) : Boolean{
        try {
            val ontology = getOntologyNoHeap(tripleManager)

            val reasoner = Reasoner(Configuration(), ontology)

            val tString = if(extractTypeClass() != null) extractTypeClass()
            else {
                log("Failed to extract OWL expression for target type", ctx)
                return false
            }
            val owlSub = getQueryExpression(tripleManager)
            if(owlSub != null) {
                var owlSup : OWLClassExpression =  OWLClassImpl(IRI.create(settings.progPrefix + tString))
                if(!settings.punning)
                    owlSup = OWLObjectSomeValuesFromImpl(OWLObjectPropertyImpl(IRI.create(settings.prefixMap()["smol"] + "implements")), owlSup)
                val res = reasoner.isEntailed(OWLSubClassOfAxiomImpl(owlSub,owlSup, HashSet()))

                if (!res)
                    log(
                        "Could not check query $query: specified type is $type",
                        ctx
                    )
                return res
            } else {
                log("Failed to extract OWL expression for query", ctx)
                return false
            }
        } catch (e: Exception){
            log("Failed to typecheck query (Exception: ${e.message}) ", ctx)
            e.printStackTrace()
            return false
        }
    }

    private fun getQueryExpression(tripleManager : TripleManager) : OWLClassExpression?{
        try {
            val out = settings.replaceKnownPrefixes(formula)
            val m = OWLManager.createOWLOntologyManager()
            val ontology = getOntologyNoHeap(tripleManager)
            val parser = ManchesterOWLSyntaxParserImpl(OntologyConfigurator(), m.owlDataFactory)
            parser.setDefaultOntology(ontology)
            return parser.parseClassExpression(out)
        } catch (e: Exception) {
            if(settings.verbose) e.printStackTrace()
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



    private fun buildTree() : Boolean {

        val toCheck = "$sparqlPrefix\n\n $query\n"
        if(toCheck.contains("%")) {
            log("%n constants are not supported yet", ctx, Severity.WARNING)
            return false
        }

        val query = QueryFactory.create(toCheck)

        if(!query.isSelectType) {
            log("non-select queries are not supported yet", ctx)
            return false
        }

        if(varName == "obj" && (query.projectVars.size != 1 || query.projectVars.first().name != varName)){
            log("access-queries must have a single extracted variable called ?obj", ctx)
            return false
        }

        val dl = buildTree(query)
        if(dl != null) {
            val f = buildFormula(dl)
            if(f != null)
                formula = f
            else return false
        }
        else return false
        return true
    }


    //TODO: this returns a tree, but we do no check if a variable occurs twice, so it actually could be a graph
    fun buildTree(query: Query) : DL?{
        val pattern = query.queryPattern
        if(pattern !is ElementGroup) return null
        val orig = DL("?obj",mutableListOf(), null)
        val dls = mutableListOf(orig)
        for (p in pattern.elements )
            buildTreeInternal(p, dls)

        dls.remove(orig)
        while (true) {
            val origNames = orig.collectNames()
            val mergeable = dls.filter { origNames.intersect(it.collectNames()).isNotEmpty() }
            dls.removeAll(mergeable)
            if (mergeable.isNotEmpty()) {
                for (dl in mergeable)
                    orig.merge(dl)
            } else break
        }

        return orig
    }

    fun buildTreeInternal(element : Element, dls : MutableList<DL>) {

        if(element !is ElementPathBlock) {
            log("This kind of query is not supported", ctx, Severity.WARNING)
            return
        }

        for(f in element.pattern.list){
            if(f.isTriple){
                val sub  = f.subject
                val predicate = f.predicate
                val obj  = f.`object`
                val target = dls.firstOrNull{ it.find(sub.toString()) != null }
                val newDL = DL(
                    sub.toString(),
                    mutableListOf(Edge(predicate.uri, false, DL(obj.toString(), mutableListOf(), obj))),
                    sub
                )
                if(target != null)  target.add(newDL)
                else dls.add(newDL)
            } else {
                log("This kind of query is not supported", ctx, Severity.WARNING)
            }
        }
    }


    fun buildFormula(dl : DL)  : String? {
        if(dl.origin != null && dl.origin.isURI) return "<${dl.name}>"
        if(dl.origin != null && dl.origin.isLiteral) return dl.name.substring(0,dl.name.indexOfFirst { it == '^' }).removeSurrounding("\"")
        var ret = "owl:Thing"
        for(n in dl.tree){
            var nextString = ""
            if(n.label == "a"){
                nextString = "<${buildFormula(n.next)}>"
            } else if(n.next.origin!!.isLiteral && !n.inverted) {
                nextString = "(<${n.label}> VALUE ${buildFormula(n.next)})"
            } else if(!n.inverted){
                nextString = "(<${n.label}> SOME ${buildFormula(n.next)})"
            } else {
                nextString = "(inverse(<${n.label}>) SOME ${buildFormula(n.next)})"
            }
            ret = if(ret == "owl:Thing") nextString else "$ret AND $nextString"
        }
        return ret
    }




    private val sparqlPrefix =
        """
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
