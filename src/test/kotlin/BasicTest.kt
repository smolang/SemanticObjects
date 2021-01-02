
import antlr.microobject.gen.WhileLexer
import antlr.microobject.gen.WhileParser
import io.kotlintest.specs.StringSpec
import microobject.data.TypeChecker
import microobject.runtime.REPL
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import kotlin.test.assertNotNull

class BasicTest : StringSpec() {
    private fun load(path : String) : Boolean {
        val lexer = WhileLexer(CharStreams.fromFileName(path))
        val tokens = CommonTokenStream(lexer)
        val parser = WhileParser(tokens)
        val tree = parser.program()

        val tC = TypeChecker(tree)
        tC.check()
        return tC.report()
    }

    init {
        for( str in listOf("double", "geo", "overload", "scene", "simulate", "test", "TwoThreeTree", "types"))
        "parsing $str"{
            assert(load(this::class.java.classLoader.getResource("$str.smol").file))
        }
    }
}