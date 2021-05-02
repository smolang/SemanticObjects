package microobject.test.basic

import antlr.microobject.gen.WhileLexer
import antlr.microobject.gen.WhileParser
import io.kotlintest.specs.StringSpec
import microobject.data.Translate
import microobject.main.Settings
import microobject.type.Severity
import microobject.type.TypeChecker
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File
import kotlin.test.assertEquals

class BasicTest : StringSpec() {
    private fun load(path : String) : Int {
        val lexer = WhileLexer(CharStreams.fromFileName(path))
        val tokens = CommonTokenStream(lexer)
        val parser = WhileParser(tokens)
        val tree = parser.program()

        val visitor = Translate()
        val pair = visitor.generateStatic(tree)

        val tC = TypeChecker(tree, Settings(false,  "/tmp/mo","","urn:"), pair.second)

        tC.check()
        tC.report()
        return tC.error.filter { it.severity == Severity.ERROR }.size
    }

    init {
        "parsing Jacobi"{
            if (!(File("examples/SimulationDemo/Prey.fmu").exists()
                  && File("examples/SimulationDemo/Predator.fmu").exists())) {
                // TODO: ignoring the test with a message would be better here
                kotlin.test.assertTrue(true)
            } else {
                assertEquals(load(this::class.java.classLoader.getResource("Jacobi.smol").file), 0)
            }
        }
    }
}
