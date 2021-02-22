import antlr.microobject.gen.WhileLexer
import antlr.microobject.gen.WhileParser
import io.kotlintest.specs.StringSpec
import microobject.type.Severity
import microobject.type.TypeChecker
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import kotlin.test.assertEquals

class BasicTest : StringSpec() {
    private fun load(path : String) : Int {
        val lexer = WhileLexer(CharStreams.fromFileName(path))
        val tokens = CommonTokenStream(lexer)
        val parser = WhileParser(tokens)
        val tree = parser.program()

        val tC = TypeChecker(tree)
        tC.check()
        tC.report()
        return tC.error.filter { it.severity == Severity.ERROR }.size
    }

    init {
        for( str in listOf("double", "overload", "scene", "TwoThreeTree", "types", "poly"))
        "parsing $str"{
            assertEquals(load(this::class.java.classLoader.getResource("$str.smol").file), 0)
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