@file:Suppress("ControlFlowWithEmptyBody")

package microobject.runtime

import microobject.data.Expression
import microobject.data.Translate
import microobject.gen.WhileLexer
import microobject.gen.WhileParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.semanticweb.HermiT.Reasoner
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserImpl
import org.semanticweb.owlapi.model.OntologyConfigurator
import org.semanticweb.owlapi.reasoner.OWLReasoner
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*

class Command(val name : String,
              private val repl : REPL,
              val command : (String) -> Boolean,
              val help : String,
              val requiresParameter : Boolean = false,
              val parameterHelp : String = "",
              val requiresDump : Boolean = false,
              val invalidatesDump : Boolean = false ){
    fun execute(param : String) : Boolean {
        if(requiresDump) repl.dump()
        if(requiresParameter && param == ""){
            repl.printRepl("Command $name expects 1 parameter (/path/to/.mo/file)")
            return false
        }
        val res = command(param)
        if(invalidatesDump) repl.validDump = false
        return res
    }
}

class REPL(private var apache: String) {
    private var interpreter: Interpreter? = null
    var validDump = false
    private var m = OWLManager.createOWLOntologyManager()
    private var ontology = m.loadOntologyFromOntologyDocument(File("/tmp/mo/output.ttl"))
    private var reasoner: OWLReasoner = Reasoner.ReasonerFactory().createReasoner(ontology)
    private val commands: MutableMap<String, Command> = mutableMapOf()
    init {
        initCommands()
    }
    fun dump(): String {
        if (!validDump) {

            val res = interpreter!!.dumpTtl()
            //printRepl(res)

            val output = File("/tmp/mo/output.ttl")
            output.parentFile.mkdirs()
            if (!output.exists()) output.createNewFile()
            output.writeText(res)
            m = OWLManager.createOWLOntologyManager()
            ontology = m.loadOntologyFromOntologyDocument(File("/tmp/mo/output.ttl"))
            reasoner = Reasoner.ReasonerFactory().createReasoner(ontology)
        }
        validDump = true
        return "/tmp/mo/output.ttl"
    }


    fun printRepl(str: String) {
        println("MO> $str \n")
    }

    private fun initInterpreter(path: String) {
        val lexer = WhileLexer(CharStreams.fromFileName(path))
        val tokens = CommonTokenStream(lexer)
        val parser = WhileParser(tokens)
        val tree = parser.program()

        val visitor = Translate()
        val pair = visitor.generateStatic(tree)


        val initGlobalStore: GlobalMemory = mutableMapOf(Pair(pair.first.obj, mutableMapOf()))

        val initStack = Stack<StackEntry>()
        initStack.push(pair.first)
        interpreter = Interpreter(
            initStack,
            initGlobalStore,
            pair.second
        )
    }

    private fun initCommands() {
        commands["exit"] = Command("exit", this, { true }, "exits the shell")
        commands["read"] = Command(
            "read",
            this,
            { str -> initInterpreter(str); false },
            "reads a file",
            parameterHelp = "Path to a .mo file",
            requiresParameter = true,
            invalidatesDump = true
        )
        commands["examine"] =
            Command("examine", this, { printRepl(interpreter!!.toString()); false }, "prints state in internal format")
        commands["info"] = Command(
            "info",
            this,
            { interpreter!!.staticInfo.toString(); false },
            "prints static information in internal format"
        )
        commands["dump"] =
            Command("dump", this, { dump(); false }, "dumps into /tmp/mo/output.ttl", invalidatesDump = true)
        commands["auto"] = Command(
            "auto",
            this,
            { while (interpreter!!.makeStep()); false },
            "dumps into /tmp/mo/output.ttl",
            invalidatesDump = true
        )
        commands["step"] = Command(
            "step",
            this,
            { interpreter!!.makeStep(); false },
            "dumps into /tmp/mo/output.ttl",
            invalidatesDump = true
        )
        commands["validate"] = Command(
            "validate",
            this,
            { str ->
                val p = Runtime.getRuntime().exec("$apache/shacl validate --data /tmp/mo/output.ttl --shapes $str")
                p.waitFor()
                var str = "jena output: \n"
                val lineReader = BufferedReader(InputStreamReader(p.inputStream))
                lineReader.lines().forEach { x: String? -> if (x != null) str += "$x\n" }
                printRepl(str)
                false
            },
            "validates against a SHACL file",
            parameterHelp = "path to a SHACL file",
            requiresParameter = true,
            requiresDump = true
        )
        commands["query"] = Command(
            "query",
            this,
            { str ->

                val out =
                    """
                    PREFIX : <urn:>
                    PREFIX owl: <http://www.w3.org/2002/07/owl#> 
                    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> 
                    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> 
                    
                    $str
                """.trimIndent()
                val output = File("/tmp/mo/output.rq")
                output.parentFile.mkdirs()
                if (!output.exists()) output.createNewFile()
                output.writeText(out)

                this.command("query-file", "/tmp/mo/output.rq")
                false
            },
            "executes a SPARQL query",
            parameterHelp = "SPARQL query",
            requiresParameter = true,
            requiresDump = true
        )

        commands["query-file"] = Command(
            "query-file",
            this,
            { str ->
                val command =
                    "$apache/sparql --data=/tmp/mo/output.ttl --query=$str"
                val p = Runtime.getRuntime().exec(command)
                p.waitFor()
                var str = "jena output: \n"
                val lineReader = BufferedReader(InputStreamReader(p.inputStream))
                lineReader.lines().forEach { x: String? -> if (x != null) str += "$x\n" }
                printRepl(str)
                false
            },
            "executes a SPARQL query from a file",
            parameterHelp = "SPARQL file",
            requiresParameter = true,
            requiresDump = true
        )

        commands["consistency"] = Command(
            "consistency",
            this,
            { str ->
                ontology.classesInSignature().forEach { println(it) }
                printRepl("HermiT result ${reasoner.isConsistent}")
                false
            },
            "prints all classes and checks that the internal ontology is consistent",
            requiresDump = true
        )
        commands["class"] = Command(
            "class",
            this,
            { str ->
                val parser = ManchesterOWLSyntaxParserImpl(OntologyConfigurator(), m.owlDataFactory)
                parser.setDefaultOntology(ontology)
                val res = reasoner.getInstances(parser.parseClassExpression(str))
                printRepl("HermiT result $res")
                false
            },
            "returns all members of a class",
            parameterHelp = "class expression in Manchester Syntax, e.r., \"<urn:MOXField>\"",
            requiresParameter = true,
            requiresDump = true
        )
        commands["eval"] = Command(
            "eval",
            this,
            { str ->
                val lexer2 = WhileLexer(CharStreams.fromString(str))
                val tokens2 = CommonTokenStream(lexer2)
                val parser2 = WhileParser(tokens2)
                val tree2 = parser2.expression()
                val visitor2 = Translate()
                val newExpr = visitor2.visit(tree2) as Expression

                printRepl(interpreter!!.evalTopMost(newExpr).literal)
                false
            },
            "evaluates a .mo expression in the current frame",
            parameterHelp = "a .mo expression",
            requiresParameter = true,
            requiresDump = true
        )
    }


    fun command(str: String, param: String): Boolean {
        if (str == "help") {
            for (cmd in commands.values) {
                print("${cmd.name}\t\t\t\t-\t${cmd.help}")
                if (cmd.requiresParameter)
                    print(", parameter: ${cmd.parameterHelp}")
                println()
            }
        }else if (interpreter == null && str != "read"){
            printRepl("No file loaded. Please \"read\" a file to continue.")
        } else if (commands.containsKey(str)) {
            return commands[str]!!.execute(param)
        } else {
            printRepl("Unknown command $str. Enter \"help\" to get a list of available commands.")
        }
        return false
    }
}