package microobject.type

import antlr.microobject.gen.WhileParser
import microobject.main.Settings
import microobject.runtime.StaticTable
import org.apache.jena.query.QueryFactory
import org.apache.jena.sparql.syntax.ElementGroup
import org.apache.jena.sparql.syntax.ElementPathBlock


class TreeQueryChecker(
    private val settings: Settings,
    private val query: String,
    private val type: Type,
    private val ctx: WhileParser.Sparql_statementContext
) : TypeErrorLogger()  {

    fun type(staticTable: StaticTable) : Boolean{
        buildTree()
        return false
    }

    fun buildTree() : MutableMap<String, MutableList<Pair<String, String>>>? {
        var result : MutableMap<String, MutableList<Pair<String, String>>> = mutableMapOf() //Pair("?obj", mutableListOf()))

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
            log("This kind of query is not supported", ctx)
            return null
        }
        val elem = pattern.elements.first()
        if(elem !is ElementPathBlock) {
            log("This kind of query is not supported", ctx)
            return null
        }
        val subpattern = elem.pattern

        for(f in subpattern.list){
            if(!f.isTriple){
                log("This kind of query is not supported", ctx)
                return null
            }
            var sub  = f.subject
            var pred = f.predicate
            var obj  = f.`object`
            if( pred.toString() == "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" && sub.isVariable && !obj.isVariable){
                val old = result.getOrDefault(sub.name, mutableListOf())
                old.add(Pair("a",obj.toString()))
                result[sub.name] = old
            } else if( !pred.isVariable && sub.isVariable && (obj.isVariable || obj.isLiteral)){
                val old = result.getOrDefault(sub.name, mutableListOf())
                old.add(Pair(pred.toString(),obj.toString()))
                result[sub.name] = old
            } else {
                log("This kind of query is not supported", ctx)
                return null
            }
        }
        println(result)
        return result
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