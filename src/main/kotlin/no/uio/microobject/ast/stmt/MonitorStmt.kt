package no.uio.microobject.ast.stmt

import com.sksamuel.hoplite.ConfigLoader
import no.uio.microobject.ast.*
import no.uio.microobject.ast.expr.FALSEEXPR
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.ast.expr.TRUEEXPR
import no.uio.microobject.runtime.*
import no.uio.microobject.type.*
import org.apache.jena.datatypes.xsd.XSDDatatype
import java.io.File

import eu.larkc.csparql.core.engine.CsparqlQueryResultProxy
import eu.larkc.csparql.common.RDFTuple

data class MonitorStmt(val target : Location, val query: Expression, val params : List<Expression>, val pos : Int = -1, val declares: Type?) :
    Statement {
    override fun toString(): String = "$target := monitor($query, ${params.joinToString(",")})"
    override fun getRDF(): String {
        var s = """
            prog:stmt${this.hashCode()} rdf:type smol:MonitorStatement.
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


    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        val name = Names.getObjName("Monitor")
        interpreter.streamManager.registerQuery(name, query, params, stackFrame.store, interpreter.heap, stackFrame.obj)
        if (declares is ComposedType && declares.getPrimary().getNameString().equals("Monitor")) {
            // only consider the first type for now. e.g., Monitor<Double>
            interpreter.streamManager.addMonitor(name, MonitorObject(name, declares.params[0]))
        } else {
            throw Exception("Monitor statement can only be assigned to type Monitor<T>")
        }
        return replaceStmt(AssignStmt(target, name, declares = declares), stackFrame)
    }

}

class MonitorObject(private val name: LiteralExpr, private val declaredType: Type) {
    
    private fun iriToLiteral(iri: String, interpreter: Interpreter): LiteralExpr {

        if (iri.endsWith("^^http://www.w3.org/2001/XMLSchema#integer")) return LiteralExpr(iri.split("^^")[0], INTTYPE)
        if (iri.endsWith("^^http://www.w3.org/2001/XMLSchema#boolean")) return LiteralExpr(iri.split("^^")[0], BOOLEANTYPE)
        if (iri.endsWith("^^http://www.w3.org/2001/XMLSchema#double")) return LiteralExpr(iri.split("^^")[0], DOUBLETYPE)
        if (iri.endsWith("^^http://www.w3.org/2001/XMLSchema#string")) return LiteralExpr(iri.split("^^")[0], STRINGTYPE)
        val literal = iri.removePrefix(interpreter.settings.runPrefix)
        for (obj in interpreter.heap.keys + interpreter.simMemory.keys)
            if (obj.literal.equals(literal)) return obj
        return LiteralExpr("ERROR")
    }

    fun getWindowResults(interpreter: Interpreter): LiteralExpr {
        val rdfTable = interpreter.streamManager.getQueryResults(name)
        var list = LiteralExpr("null")

        if (rdfTable != null) {

            val resIt = rdfTable.iterator()
            while (resIt.hasNext()) {
                val rdfTuple = resIt.next()
                try {
                    // only consider first result for now
                    var literal = iriToLiteral(rdfTuple.get(0), interpreter)
                    if (literal.tag != declaredType)
                        throw Exception("Monitor parameter has incorrect type")

                    val name = Names.getObjName("List")
                    val newMemory: Memory = mutableMapOf()                    
                    newMemory["content"] = literal
                    newMemory["next"] = list
                    interpreter.heap[name] = newMemory
                    list = name

                } catch (e: Exception) {
                    throw Exception("Error while processing query result: ${e.message}")
                }

            }
        }
        return list
    }
}

