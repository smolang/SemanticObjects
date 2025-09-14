package no.uio.microobject.ast.stmt

import com.sksamuel.hoplite.ConfigLoader
import no.uio.microobject.ast.*
import no.uio.microobject.ast.expr.FALSEEXPR
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.ast.expr.TRUEEXPR
import no.uio.microobject.runtime.*
import no.uio.microobject.type.*
import no.uio.microobject.data.TripleSettings
import no.uio.microobject.main.ReasonerMode
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.rdf.model.Model
import java.io.File

import eu.larkc.csparql.core.engine.CsparqlQueryResultProxy
import eu.larkc.csparql.common.RDFTuple

data class PushStaticStmt(val target : Location, val sources: Expression, val pos : Int = -1, val declares: Type?) : Statement {
    override fun toString(): String = "$target:=pushStatic(sources=$sources)"
    override fun getRDF(): String {
        var s = "prog:stmt${this.hashCode()} rdf:type smol:PushStaticStmt.".trimIndent()
        return s
    }


    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        val namedIri = interpreter.streamManager.getStaticNamedIri()

        // parse sources from comma-separated string sources
        val sourcesLit = interpreter.eval(sources, stackFrame)
        if (sourcesLit.tag != STRINGTYPE) {
            throw Exception("The sources parameter in pushStatic statement must be a string literal")
        }
        val sourcesStr = sourcesLit.literal.removeSurrounding("\"")
        val sourcesList = sourcesStr.split(",")
        val sourcesMap = hashMapOf(
            "heap"             to false,
            "staticTable"      to false,
            "vocabularyFile"   to false,
            "externalOntology" to false,
            "urlOntology"      to false,
            "fmos"             to false
        )
        for (s in sourcesList) {
            val trimmed = s.trim()
            if (sourcesMap.containsKey(trimmed)) {
                sourcesMap[trimmed] = true
            } else {
                throw Exception("Unknown source '$trimmed' in pushStatic statement, only comma-separated [heap, staticTable, vocabularyFile, externalOntology, urlOntology, fmos] are allowed")
            }
        }

        val ts = TripleSettings(
            sources = sourcesMap,
            guards = hashMapOf("heap" to true, "staticTable" to true),
            virtualization = hashMapOf("heap" to true, "staticTable" to true, "fmos" to true),
            jenaReasoner = interpreter.settings.reasoner,
            cachedModel = null
        )

        val model = interpreter.tripleManager.getModel(ts)

        interpreter.streamManager.putStaticNamedGraph(namedIri, model)
        val resultLit = LiteralExpr(namedIri, STRINGTYPE)
        return replaceStmt(AssignStmt(target, resultLit, declares = declares), stackFrame)
    }

}
