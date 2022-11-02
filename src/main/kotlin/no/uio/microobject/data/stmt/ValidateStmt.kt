package no.uio.microobject.data.stmt

import no.uio.microobject.data.*
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry
import no.uio.microobject.type.Type
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.ShaclValidator
import org.apache.jena.shacl.Shapes
import java.io.File

data class ValidateStmt(val target : Location, val query: Expression, val pos : Int = -1, val declares: Type?) :
    Statement {
    override fun toString(): String = "$target := validate($query)"
    override fun getRDF(): String {
        return """
            prog:stmt${this.hashCode()} rdf:type smol:ShaclStatement.
            prog:stmt${this.hashCode()} smol:hasTarget prog:loc${target.hashCode()}.
            prog:stmt${this.hashCode()} smol:hasQuery prog:expr${query.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent() + target.getRDF() + query.getRDF()
    }

    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        if(query !is LiteralExpr) throw Exception("validate takes a file path in a String as a parameter")
        val fileName = query.literal.removeSurrounding("\"")
        val file = File(fileName)
        if(!file.exists()) throw Exception("file $fileName does not exist")
        val newFile = File("${interpreter.settings.outdir}/shape.ttl")
        if(!newFile.exists()) {
            File(interpreter.settings.outdir).mkdirs()
            newFile.createNewFile()
        }
        newFile.writeText(interpreter.settings.prefixes() + "\n"+ interpreter.settings.getHeader() + "\n@prefix sh: <http://www.w3.org/ns/shacl#>.\n")
        newFile.appendText(file.readText())
        val shapesGraph = RDFDataMgr.loadGraph("${interpreter.settings.outdir}/shape.ttl")
        val dataGraph = interpreter.tripleManager.getModel().graph

        val shapes: Shapes = Shapes.parse(shapesGraph)

        val report = ShaclValidator.get().validate(shapes, dataGraph)
        val resLit = if(report.conforms()) TRUEEXPR else FALSEEXPR
        return replaceStmt(AssignStmt(target, resLit, declares = declares), stackFrame)
    }
}