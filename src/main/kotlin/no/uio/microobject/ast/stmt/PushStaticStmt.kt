package no.uio.microobject.ast.stmt

import com.sksamuel.hoplite.ConfigLoader
import no.uio.microobject.ast.*
import no.uio.microobject.ast.expr.FALSEEXPR
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.ast.expr.TRUEEXPR
import no.uio.microobject.runtime.*
import no.uio.microobject.type.*
import no.uio.microobject.data.TripleSettings
import org.apache.jena.datatypes.xsd.XSDDatatype
import java.io.File

import eu.larkc.csparql.core.engine.CsparqlQueryResultProxy
import eu.larkc.csparql.common.RDFTuple

data class PushStaticStmt(val pos : Int = -1) : Statement {
    override fun toString(): String = "pushStatic()"
    override fun getRDF(): String {
        var s = "prog:stmt${this.hashCode()} rdf:type smol:PushStaticStmt.".trimIndent()
        return s
    }


    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        // fixed iri: prog:staticTable
        val namedIri = "${interpreter.settings.progPrefix}staticTable"

        // add only staticTable
        val ts = TripleSettings(
            sources = hashMapOf("heap" to false, "staticTable" to true, "vocabularyFile" to false, "fmos" to false, "externalOntology" to false, "urlOntology" to false),
            guards = hashMapOf("heap" to true, "staticTable" to true),
            virtualization = hashMapOf("heap" to true, "staticTable" to true, "fmos" to true),
            jenaReasoner = interpreter.settings.reasoner,
            cachedModel = null
        )
        val sTableModel = interpreter.tripleManager.getModel(ts)
        interpreter.streamManager.putStaticNamedGraph(namedIri, sTableModel)

        return EvalResult(null, emptyList())
    }

}
