import antlr.microobject.gen.WhileLexer
import antlr.microobject.gen.WhileParser
import io.kotlintest.specs.StringSpec
import microobject.data.Translate
import microobject.main.Settings
import microobject.type.TypeChecker
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TypeTest  : StringSpec() {
    private fun initTest(filename: String): Pair<TypeChecker, WhileParser.ProgramContext> {
        val path = this::class.java.classLoader.getResource("$filename.smol").file
        val lexer = WhileLexer(CharStreams.fromFileName(path))
        val tokens = CommonTokenStream(lexer)
        val parser = WhileParser(tokens)
        val tree = parser.program()

        val visitor = Translate()
        val pair = visitor.generateStatic(tree)

        val tC = TypeChecker(tree, Settings(false,  "/tmp/mo","","urn:"), pair.second)
        tC.collect()

        return Pair(tC, tree)
    }

    private fun retrieveClass(name : String, prog : WhileParser.ProgramContext) : List<WhileParser.Class_defContext>{
        return prog.class_def().filter { it.NAME(0).text == name }
    }

    private fun retrieveMethod(name : String, classDef : WhileParser.Class_defContext) : List<WhileParser.Method_defContext>{
        return classDef.method_def().filter { it.NAME().text == name }
    }

    private fun checkMet(className : String, metName : String, filename: String) : TypeChecker{
        val pair = initTest(filename)
        val classes = retrieveClass(className, pair.second)
        assert(classes.size == 1)
        val methods = retrieveMethod(metName, classes[0])
        assert(methods.size == 1)
        val met = methods[0]
        pair.first.checkMet(met, className)
        return pair.first
    }

    private fun checkClass(className : String, filename: String) : TypeChecker{
        val pair = initTest(filename)
        val classes = retrieveClass(className, pair.second)
        assert(classes.size == 1)
        val classDef = classes[0]
        pair.first.checkClass(classDef)
        return pair.first
    }

    init {
        "Assign Test Success"{
            assert(checkMet("Test", "assignSuccess1", "test_assign" ).report(false))
            assert(checkMet("Test", "assignSuccess2", "test_assign" ).report(false))
            assert(checkMet("TestGen", "success", "test_assign" ).report(false))
        }
        for(i in 1..7) {
            "Assign Test Fail $i"{
                val tC = checkMet("Test", "fail$i", "test_assign")
                assertFalse(tC.report(false))
                assertEquals(tC.error.size, 1)
            }
        }
        for(i in 1..4) {
            "Assign Test Gen Fail $i"{
                val tC = checkMet("TestGen", "fail$i", "test_assign")
                assertFalse(tC.report(false))
                assertEquals(tC.error.size, 1)
            }
        }

        "Query check success"{
            val tC = checkMet("Test", "m1", "type_query")
            assert(tC.report(false))
        }

        "Query check fail"{
            val tC = checkMet("Test", "m2", "type_query")
            assertFalse(tC.report(false))
        }

        "Call Test Success"{
            assert(checkMet("Test", "success", "test_call" ).report(false))
        }
        for(i in 1..7){
            "Call Test Fail $i"{
                val tC = checkMet("Test", "fail$i", "test_call" )
                assertFalse(tC.report(false))
                assertEquals(tC.error.size, 1)
            }
        }



        "Override Test Success"{
            assert(checkClass("Success1", "test_override" ).report(false))
            assert(checkClass("Success2", "test_override" ).report(false))
        }
        "Override Test Fail 1"{
            val tC = checkClass("Fail1", "test_override" )
            assert(tC.report(false)) //it is just a warning
            assertEquals(tC.error.size, 1)
        }
        "Override Test Fail 2"{
            val tC = checkClass("Fail2", "test_override" )
            assertFalse(tC.report(false))
            assertEquals(tC.error.size, 1)
        }
        "Override Test Fail 3"{
            val tC = checkClass("Fail3", "test_override" )
            assert(tC.report(false)) //it is just a warning
            assertEquals(tC.error.size, 1)
        }
        "Override Test Fail 4"{
            val tC = checkClass("Fail4", "test_override" )
            assertFalse(tC.report(false))
            assertEquals(tC.error.size, 2)
        }
        "Override Test Fail 5"{
            val tC = checkClass("Fail5", "test_override" )
            assert(tC.report(false)) //it is just a warning
            assertEquals(tC.error.size, 1)
        }
        "Override Test Fail 6"{
            val tC = checkClass("Fail6", "test_override" )
            assertFalse(tC.report(false))
            assertEquals(tC.error.size, 1)
        }
        "Override Test Fail 7"{
            val tC = checkClass("Fail7", "test_override" )
            assertFalse(tC.report(false))
            assertEquals(tC.error.size, 1)
        }
    }
}