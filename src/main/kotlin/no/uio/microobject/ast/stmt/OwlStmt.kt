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
    override fun toString(): String = "$target := member($query)"
    override fun getRDF(): String {
        return """
            prog:stmt${this.hashCode()} rdf:type smol:OwlStatement.
            prog:stmt${this.hashCode()} smol:hasTarget prog:loc${target.hashCode()}.
            prog:stmt${this.hashCode()} smol:hasQuery prog:expr${query.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent() + target.getRDF() + query.getRDF()
    }

    // How it works:
    // 1. Calls interpreter.owlQuery() (defined in Interpreter.kt:161-170) which:
    // - Parses the Manchester syntax OWL query string
    // - Uses HermiT reasoner to get instances matching the class expression
    // - Returns a NodeSet<OWLNamedIndividual>
    // 2. Builds a linked list from the query results:
    // - Iterates through each individual in the result set
    // - Creates a List object for each result
    // - Extracts the individual name from the URI (after the #)
    // - Links them together in a singly-linked list structure
    // 3. Returns an assignment that assigns the constructed list to the target variable

    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
//        println("[DEBUG OwlStmt] === member() function called ===")
//        println("[DEBUG OwlStmt] Target variable: $target")
//        println("[DEBUG OwlStmt] Query expression: $query")
        if (query !is LiteralExpr || query.tag != STRINGTYPE) {
            throw Exception("Please provide a string as the input to a derive statement")
        }

        val res : NodeSet<OWLNamedIndividual> = interpreter.owlQuery(query.literal)
//        println("[DEBUG OwlStmt] OWL reasoner returned ${res.entities().count()} individuals")
//        if (res.isEmpty) {
//            println("[DEBUG OwlStmt] WARNING: Reasoner returned EMPTY result set!")
//        } else {
//            println("[DEBUG OwlStmt] Reasoner found matches - processing results...")
//        }
        var list = LiteralExpr("null")
        for (r in res) {
            val name = Names.getObjName("List")
            val newMemory: Memory = mutableMapOf()
            val found = r.toString().removePrefix("Node( <").split("#")[1].removeSuffix("> )")
//            println("[DEBUG OwlStmt] Processing result: ${r.toString()}")
//            println("[DEBUG OwlStmt] Extracted name: '$found'")
//            println("[DEBUG OwlStmt] Looking up in heap (heap size: ${interpreter.heap.size})...")
            val foundAny = interpreter.heap.keys.firstOrNull { it.literal == found }
            if(foundAny != null) {
//                println("[DEBUG OwlStmt] ✓ Found in heap: $found (type: ${foundAny.tag})")
                newMemory["content"] = LiteralExpr(found, foundAny.tag)
            }
            else {
//                println("[DEBUG OwlStmt] ✗ NOT found in heap, attempting literal parsing...")
                if(found.startsWith("\"")) {
//                    println("[DEBUG OwlStmt] Parsed as STRING")
                    newMemory["content"] = LiteralExpr(found, STRINGTYPE)
                } else if(found.matches("\\d+".toRegex())) {
//                    println("[DEBUG OwlStmt] Parsed as INT")
                    newMemory["content"] = LiteralExpr(found, INTTYPE)
                } else if(found.matches("\\d+.\\d+".toRegex())) {
//                    println("[DEBUG OwlStmt] Parsed as DOUBLE")
                    newMemory["content"] = LiteralExpr(found, DOUBLETYPE)
                } else {
//                    println("[DEBUG OwlStmt] ERROR: Cannot parse '$found'")
                    throw Exception("Concept returned unknown object/literal: $found")
                }
            }

            newMemory["next"] = list
            interpreter.heap[name] = newMemory
            list = name
//            println("[DEBUG OwlStmt] Added to list: $name")
        }
//        println("[DEBUG OwlStmt] === member() complete ===")
//        println("[DEBUG OwlStmt] Final list head: $list")
        return replaceStmt(AssignStmt(target, list, declares = declares), stackFrame)
    }
}