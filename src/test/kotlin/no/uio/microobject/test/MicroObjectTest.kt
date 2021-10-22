package no.uio.microobject.test

import no.uio.microobject.antlr.WhileLexer
import no.uio.microobject.antlr.WhileParser
import io.kotlintest.specs.StringSpec
import no.uio.microobject.data.RuleGenerator
import no.uio.microobject.data.Translate
import no.uio.microobject.main.Settings
import no.uio.microobject.runtime.GlobalMemory
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.InterpreterBridge
import no.uio.microobject.runtime.StackEntry
import no.uio.microobject.type.TypeChecker
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File
import java.util.*
import no.uio.microobject.data.TripleManager

open class MicroObjectTest : StringSpec() {
    protected enum class StringLoad {STMT, CLASS, PRG, PATH, RES}
    protected var settings = Settings(false, false,  "/tmp/mo","","","urn:")
    protected fun loadBackground(path : String){
        val file = File(path)
        val backgr = file.readText()
        settings = Settings(false, false,  "/tmp/mo",backgr,"","urn:")
    }
    private fun loadString(program : String) : WhileParser.ProgramContext{
        val lexer = WhileLexer(CharStreams.fromString(program))
        val tokens = CommonTokenStream(lexer)
        val parser = WhileParser(tokens)
        return parser.program()
    }

    private fun loadPath(path : String) : WhileParser.ProgramContext{
        val localPath = if(System.getProperty("os.name").contains("Windows")) path.removePrefix("/") else path
        val lexer = WhileLexer(CharStreams.fromFileName(localPath))
        val tokens = CommonTokenStream(lexer)
        val parser = WhileParser(tokens)
        return parser.program()
    }

    private fun loadClass(classString : String) : WhileParser.ProgramContext{
        val program = "$classString\n main skip; end"
        val lexer = WhileLexer(CharStreams.fromString(program))
        val tokens = CommonTokenStream(lexer)
        val parser = WhileParser(tokens)
        return parser.program()
    }

    private fun loadStatement(stmtString : String) : WhileParser.ProgramContext{
        val program = "main $stmtString end"
        val lexer = WhileLexer(CharStreams.fromString(program))
        val tokens = CommonTokenStream(lexer)
        val parser = WhileParser(tokens)
        return parser.program()
    }

    protected fun initInterpreter(str : String, loadAs : StringLoad = StringLoad.PATH) : Pair<Interpreter, TypeChecker> {
        val ast = when(loadAs){
            StringLoad.STMT -> loadStatement(str)
            StringLoad.PRG -> loadString(str)
            StringLoad.CLASS -> loadClass(str)
            StringLoad.PATH -> loadPath(str)
            StringLoad.RES -> loadPath(this::class.java.classLoader.getResource("$str.smol").file)
        }
        val visitor = Translate()
        val pair = visitor.generateStatic(ast)

        val settings = Settings(false, false, "/tmp/mo","","","urn:")
        val tripleManager = TripleManager(settings, pair.second, null)

        val tC = TypeChecker(ast, settings, tripleManager)
        tC.collect()
        val iB = InterpreterBridge(null)
        val rules = RuleGenerator(settings).generateBuiltins(ast, iB)


        val initGlobalStore: GlobalMemory = mutableMapOf(Pair(pair.first.obj, mutableMapOf()))

        val initStack = Stack<StackEntry>()
        initStack.push(pair.first)
        val interpreter = Interpreter(
            initStack,
            initGlobalStore,
            mutableMapOf(),
            pair.second,
            settings,
            rules,
        )
        iB.interpreter = interpreter
        return Pair(interpreter, tC)
    }

    protected fun initTc(str : String, loadAs : StringLoad = StringLoad.PATH) : Pair<TypeChecker, WhileParser.ProgramContext> {

        val ast = when(loadAs){
            StringLoad.STMT -> loadStatement(str)
            StringLoad.PRG -> loadString(str)
            StringLoad.CLASS -> loadClass(str)
            StringLoad.PATH -> loadPath(str)
            StringLoad.RES -> loadPath(this::class.java.classLoader.getResource("$str.smol").file)
        }
        val visitor = Translate()
        val pair = visitor.generateStatic(ast)

        val settings = Settings(false, false, "/tmp/mo","","","urn:")
        val tripleManager = TripleManager(settings, pair.second, null)

        val tC = TypeChecker(ast, settings, tripleManager)
        tC.collect()
        return Pair(tC, ast)
    }

    protected fun retrieveClass(name : String, prog : WhileParser.ProgramContext) : List<WhileParser.Class_defContext>{
        return prog.class_def().filter { it.className.text == name }
    }

    protected fun retrieveMethod(name : String, classDef : WhileParser.Class_defContext) : List<WhileParser.Method_defContext>{
        return classDef.method_def().filter { it.NAME().text == name }
    }

    protected fun executeUntilBreak(interpreter : Interpreter){
        while(interpreter.makeStep());
    }
}
