package no.uio.microobject.ast.stmt

import com.sksamuel.hoplite.ConfigLoader
import no.uio.microobject.ast.*
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.runtime.*
import no.uio.microobject.type.DOUBLETYPE
import no.uio.microobject.type.INTTYPE
import no.uio.microobject.type.STRINGTYPE
import no.uio.microobject.type.Type
import org.apache.jena.datatypes.xsd.XSDDatatype
import java.io.File

// For ontology-based reflexion
data class AccessStmt(val target : Location, val query: Expression, val params : List<Expression>, val pos : Int = -1, val mode : AccessMode = SparqlMode, val declares: Type?) :
    Statement {
    override fun toString(): String = "$target := access($query, ${params.joinToString(",")})"
    override fun getRDF(): String {
        var s = """
            prog:stmt${this.hashCode()} rdf:type smol:AccessStatement.
            prog:stmt${this.hashCode()} smol:hasTarget prog:loc${target.hashCode()}.
            prog:stmt${this.hashCode()} smol:hasQuery prog:expr${query.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent()
        for (i in params.indices){
            s += "prog:stmt${this.hashCode()} smol:hasParameter [smol:hasParameterIndex $i ; smol:hasParameterValue prog:expr${params[i].hashCode()}; ].\n"
            s += params[i].getRDF()
        }
        // return s + target.getRDF()
        return s + target.getRDF() + query.getRDF()
        // '${literal.removePrefix("\"").removeSuffix("\"")}'
    }

    private fun evalInflux(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        val path = (mode as InfluxDBMode).config.removeSurrounding("\"")
        val config = ConfigLoader().loadConfigOrThrow<InfluxDBConnection>(File(path))

        val str = interpreter.prepareQuery(query, params, stackFrame.store, interpreter.heap, stackFrame.obj)
        val vals = config.queryOneSeries(str.removeSurrounding("\""))
        var list = LiteralExpr("null")
        for(r in vals){
            val name = Names.getObjName("List")
            val newMemory: Memory = mutableMapOf()
            newMemory["content"] = LiteralExpr(r.toString(), DOUBLETYPE)
            newMemory["next"] = list
            interpreter.heap[name] = newMemory
            list = name
        }
        return replaceStmt(AssignStmt(target, list, declares = declares), stackFrame)
    }
    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        if(mode is InfluxDBMode) return evalInflux(heapObj, stackFrame, interpreter)

        /* stmt.mode == SparqlMode */
        val str = interpreter.prepareQuery(query, params, stackFrame.store, interpreter.heap, stackFrame.obj)
        val results = interpreter.query(str.removePrefix("\"").removeSuffix("\""))
        var list = LiteralExpr("null")
        if (results != null) {
            for (r in results) {
                val obres = r.get("?obj")
                    ?: throw Exception("Could not select ?obj variable from results, please select using only ?obj")
                val name = Names.getObjName("List")
                val newMemory: Memory = mutableMapOf()

                val found = obres.toString().removePrefix(interpreter.settings.runPrefix)
                val objNameCand = if(found.startsWith("\\\"")) found.replace("\\\"","\"") else found
                for (ob in interpreter.heap.keys) {
                    if (ob.literal == objNameCand) {
                        newMemory["content"] = LiteralExpr(objNameCand, ob.tag)
                        break
                    }
                }
                if (!newMemory.containsKey("content")) {
                    if(obres.isLiteral && obres.asNode().literalDatatype == XSDDatatype.XSDstring) newMemory["content"] =
                        LiteralExpr("\"" + found + "\"", STRINGTYPE)
                    else if(obres.isLiteral && obres.asNode().literalDatatype == XSDDatatype.XSDinteger)
                        newMemory["content"] = LiteralExpr(found.split("^^")[0], INTTYPE)
                    else if(objNameCand.matches("\\d+".toRegex()) || objNameCand.matches("\\d+\\^\\^http://www.w3.org/2001/XMLSchema#integer".toRegex()))
                        newMemory["content"] = LiteralExpr(found.split("^^")[0], INTTYPE)
                    else if(objNameCand.matches("\\d+.\\d+".toRegex())) newMemory["content"] =
                        LiteralExpr(found, DOUBLETYPE)
                    else throw Exception("Query returned unknown object/literal: $found")
                }
                newMemory["next"] = list
                interpreter.heap[name] = newMemory
                list = name
            }
        }
        return EvalResult(
            StackEntry(
                AssignStmt(target, list, declares = declares),
                stackFrame.store,
                stackFrame.obj,
                stackFrame.id
            ), listOf()
        )

    }
}