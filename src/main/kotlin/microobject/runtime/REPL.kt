@file:Suppress("ControlFlowWithEmptyBody")

package microobject.runtime

import microobject.data.Expression
import microobject.data.Translate
import microobject.gen.WhileLexer
import microobject.gen.WhileParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.ResultSetFormatter
import org.apache.jena.rdf.model.ModelFactory
import org.semanticweb.HermiT.Reasoner
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserImpl
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.semanticweb.owlapi.model.OntologyConfigurator
import org.semanticweb.owlapi.reasoner.OWLReasoner
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*


class Command(
    val name: String,
    private val repl: REPL,
    val command: (String) -> Boolean,
    val help: String,
    val requiresParameter: Boolean = false,
    val parameterHelp: String = "",
    val requiresDump: Boolean = false,
    val requiresApache: Boolean = false,
    val invalidatesDump: Boolean = false
){
    fun execute(param: String, apache: String) : Boolean {
        if(requiresDump) repl.dump()
        if(requiresParameter && param == ""){
            repl.printRepl("Command $name expects 1 parameter $parameterHelp.")
            return false
        }
        if(requiresApache && apache == ""){
            repl.printRepl("Command $name requires that a path to an apache jena installation is provided.")
            return false
        }
        val res = command(param)
        if(invalidatesDump) repl.validDump = false
        return res
    }
}

@Suppress("DEPRECATION") // ReasonerFactory is deprecated by HermiT but I keep it like this to make a change easier
class REPL(private val apache: String, private val outPath: String, private val verbose: Boolean) {
    private var interpreter: Interpreter? = null
    var validDump = false
    private lateinit var m : OWLOntologyManager
    private lateinit var ontology : OWLOntology
    private lateinit var reasoner : OWLReasoner
    private val commands: MutableMap<String, Command> = mutableMapOf()
    init {
        initOntology()
        initCommands()
    }

    private fun initOntology(){
        val dir = File("$outPath/output.ttl")
        dir.parentFile.mkdirs()
        if (!dir.exists()) dir.createNewFile()
        m = OWLManager.createOWLOntologyManager()
        ontology = m.loadOntologyFromOntologyDocument(File("$outPath/output.ttl"))
        reasoner = Reasoner.ReasonerFactory().createReasoner(ontology)
    }


    fun command(str: String, param: String): Boolean {
        if (str == "help") {
            for (cmd in commands.values) {
                print("${cmd.name}\n\t- ${cmd.help}")
                if (cmd.requiresParameter)
                    print(", parameter: ${cmd.parameterHelp}")
                println()
            }
        }else if (interpreter == null && str != "read" && str != "exit"){
            printRepl("No file loaded. Please \"read\" a file to continue.")
        } else if (commands.containsKey(str)) {
            try{
                return commands[str]!!.execute(param, apache)
            } catch (e: Exception) {
                printRepl("Command $str $param caused an exception. Internal state may be inconsistent.")
                e.printStackTrace()
                return false
            }
        } else {
            printRepl("Unknown command $str. Enter \"help\" to get a list of available commands.")
        }
        return false
    }
    fun dump() {
        if (!validDump) {

            val res = interpreter!!.dumpTtl()
            if(verbose) printRepl(res)

            val output = File("$outPath/output.ttl")
            output.parentFile.mkdirs()
            if (!output.exists()) output.createNewFile()
            output.writeText(res)
            initOntology()
        }
        validDump = true
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
            pair.second,
            outPath
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
            { printRepl(interpreter!!.staticInfo.toString()); false },
            "prints static information in internal format"
        )
        commands["dump"] =
            Command("dump", this, { dump(); false }, "dumps into /tmp/mo/output.ttl", invalidatesDump = true)
        commands["auto"] = Command(
            "auto",
            this,
            { while (interpreter!!.makeStep()); false },
            "continues execution until the next breakpoint",
            invalidatesDump = true
        )
        commands["step"] = Command(
            "step",
            this,
            { interpreter!!.makeStep(); false },
            "executes one step",
            invalidatesDump = true
        )
        commands["validate"] = Command(
            "validate",
            this,
            { str ->
                val p = Runtime.getRuntime().exec("$apache/shacl validate --data $outPath/output.ttl --shapes $str")
                p.waitFor()
                var out = "jena output: \n"
                val lineReader = BufferedReader(InputStreamReader(p.inputStream))
                lineReader.lines().forEach { x: String? -> if (x != null) out += "$x\n" }
                printRepl(out)
                false
            },
            "validates against a SHACL file",
            parameterHelp = "path to a SHACL file",
            requiresParameter = true,
            requiresApache = true,
            requiresDump = true
        )
        commands["query"] = Command(
            "query",
            this,
            { str ->
                val results = interpreter!!.query(str)
                printRepl("\n"+ResultSetFormatter.asText(results))
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
                    "$apache/sparql --data=$outPath/output.ttl --query=$str"
                val p = Runtime.getRuntime().exec(command)
                p.waitFor()
                var out = "jena output: \n"
                val lineReader = BufferedReader(InputStreamReader(p.inputStream))
                lineReader.lines().forEach { x: String? -> if (x != null) out += "$x\n" }
                printRepl(out)
                false
            },
            "executes a SPARQL query from a file",
            parameterHelp = "SPARQL file",
            requiresParameter = true,
            requiresApache = true,
            requiresDump = true
        )

        commands["consistency"] = Command(
            "consistency",
            this,
            { _ ->
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
}