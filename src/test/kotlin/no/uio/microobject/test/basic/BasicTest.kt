package no.uio.microobject.test.basic

import no.uio.microobject.antlr.WhileLexer
import no.uio.microobject.antlr.WhileParser
import io.kotlintest.specs.StringSpec
import no.uio.microobject.data.Translate
import no.uio.microobject.main.Settings
import no.uio.microobject.type.Severity
import no.uio.microobject.type.TypeChecker
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File
import kotlin.test.assertEquals
import no.uio.microobject.data.TripleManager

class BasicTest : StringSpec() {
    private fun load(path : String) : Int {
        val localPath = if(System.getProperty("os.name").contains("Windows")) path.removePrefix("/") else path
        val stdLib = this::class.java.classLoader.getResource("StdLib.smol").readText() + "\n\n"
        val program =  File(localPath).readText(Charsets.UTF_8)
        val lexer = WhileLexer(CharStreams.fromString(stdLib + program))
        val tokens = CommonTokenStream(lexer)
        val parser = WhileParser(tokens)
        val tree = parser.program()

        val visitor = Translate()
        val pair = visitor.generateStatic(tree)


        val settings = Settings(false,false,  "/tmp/mo","","urn:", "", extraPrefixes = hashMapOf())
        val tripleManager = TripleManager(settings, pair.second, null)

        val tC = TypeChecker(tree, settings, tripleManager)

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
