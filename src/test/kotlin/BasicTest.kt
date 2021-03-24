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
        for( str in listOf("double", "overload", "types", "poly", "destroy"))
        "parsing $str"{
            assertEquals(load(this::class.java.classLoader.getResource("$str.smol").file), 0)
        }
        "parsing Jacobi"{
            if (!(File("examples/SimulationDemo/Prey.fmu").exists()
                  && File("examples/SimulationDemo/Predator.fmu").exists())) {
                // TODO: ignoring the test with a message would be better here
                kotlin.test.assertTrue(true)
            } else {
                assertEquals(load(this::class.java.classLoader.getResource("Jacobi.smol").file), 0)
            }
        }
        "parsing scene"{
            assertEquals(load(this::class.java.classLoader.getResource("scene.smol").file), 1)
        }
        "parsing TwoThreeTree"{
            assertEquals(load(this::class.java.classLoader.getResource("TwoThreeTree.smol").file), 1)
        }
        "parsing test_assign"{
            assertEquals(load(this::class.java.classLoader.getResource("test_assign.smol").file), 11)
        }
        "parsing test_call"{
            assertEquals(load(this::class.java.classLoader.getResource("test_call.smol").file), 8)
        }
        "parsing test_override"{
            assertEquals(load(this::class.java.classLoader.getResource("test_override.smol").file), 5)
        }
    }
}
