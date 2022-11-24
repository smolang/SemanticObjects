package no.uio.microobject.ast.stmt

import no.uio.microobject.ast.*
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry
import no.uio.microobject.type.DOUBLETYPE
import no.uio.microobject.type.INTTYPE
import no.uio.microobject.type.STRINGTYPE
import no.uio.microobject.type.Type
import org.semanticweb.owlapi.model.OWLNamedIndividual
import org.semanticweb.owlapi.reasoner.NodeSet

data class OwlStmt(val target : Location, val query: Expression, val pos : Int = -1, val declares: Type?) : Statement {
    override fun toString(): String = "$target := derive($query)"
    override fun getRDF(): String {
        return """
            prog:stmt${this.hashCode()} rdf:type smol:OwlStatement.
            prog:stmt${this.hashCode()} smol:hasTarget prog:loc${target.hashCode()}.
            prog:stmt${this.hashCode()} smol:hasQuery prog:expr${query.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent() + target.getRDF() + query.getRDF()
    }

    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        if (query !is LiteralExpr || query.tag != STRINGTYPE) {
            throw Exception("Please provide a string as the input to a derive statement")
        }

        val res : NodeSet<OWLNamedIndividual> = interpreter.owlQuery(query.literal)
        var list = LiteralExpr("null")
        for (r in res) {
            val name = Names.getObjName("List")
            val newMemory: Memory = mutableMapOf()
            val found = r.toString().removePrefix("Node( <").split("#")[1].removeSuffix("> )")

            val foundAny = interpreter.heap.keys.firstOrNull { it.literal == found }
            if(foundAny != null) newMemory["content"] = LiteralExpr(found, foundAny.tag)
            else {
                if(found.startsWith("\"")) newMemory["content"] = LiteralExpr(found, STRINGTYPE)
                else if(found.matches("\\d+".toRegex())) newMemory["content"] = LiteralExpr(found, INTTYPE)
                else if(found.matches("\\d+.\\d+".toRegex())) newMemory["content"] = LiteralExpr(found, DOUBLETYPE)
                else throw Exception("Concept returned unknown object/literal: $found")
            }

            newMemory["next"] = list
            interpreter.heap[name] = newMemory
            list = name
        }
        return replaceStmt(AssignStmt(target, list, declares = declares), stackFrame)
    }
}