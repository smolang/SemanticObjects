@file:Suppress("ControlFlowWithEmptyBody")

package no.uio.microobject.runtime

import no.uio.microobject.antlr.WhileLexer
import no.uio.microobject.antlr.WhileParser
import no.uio.microobject.data.Expression
import no.uio.microobject.data.RuleGenerator
import no.uio.microobject.data.Translate
import no.uio.microobject.main.Settings
import no.uio.microobject.type.TypeChecker
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.apache.jena.query.ResultSetFormatter
import org.semanticweb.HermiT.Reasoner
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxParserImpl
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.semanticweb.owlapi.model.OntologyConfigurator
import org.semanticweb.owlapi.reasoner.OWLReasoner
import java.io.File
import java.util.*

class Command(
    val name: String,
    private val repl: REPL,
    val command: (String) -> Boolean,
    val help: String,
    val requiresParameter: Boolean = false,
    val parameterHelp: String = "",
    val requiresDump: Boolean = false,
    val invalidatesDump: Boolean = false
){
    fun execute(param: String) : Boolean {
        if(requiresDump) repl.dump()
        if(requiresParameter && param == ""){
            repl.printRepl("Command $name expects 1 parameter $parameterHelp.")
            return false
        }
        val res = command(param)
        if(invalidatesDump) repl.validDump = false
        return res
    }
}

@Suppress("DEPRECATION") // ReasonerFactory is deprecated by HermiT but I keep it like this to make a change easier
class REPL(private val settings: Settings) {
    private var interpreter: Interpreter? = null
    var validDump = false
    private lateinit var m : OWLOntologyManager
    private lateinit var ontology : OWLOntology
    private lateinit var reasoner : OWLReasoner
    private val commands: MutableMap<String, Command> = mutableMapOf()
    private var rules = ""
    init {
        initOntology()
        initCommands()
    }

    // TODO: this method will be removed when we have unified jena models and OWLAPI/Hermit
    private fun initOntology(){
        val dir = File("${settings.outpath}/output.ttl")
        dir.parentFile.mkdirs()
        if (!dir.exists()) dir.createNewFile()
        m = OWLManager.createOWLOntologyManager()
        ontology = m.loadOntologyFromOntologyDocument(File("${settings.outpath}/output.ttl"))
        reasoner = Reasoner.ReasonerFactory().createReasoner(ontology)
    }


    fun command(str: String, param: String): Boolean {
        if (str == "help") {
            for (cmd in commands.values.toSet().sortedBy { it.name }) {
                print("${cmd.name}\n\t- ${cmd.help}")
                if (cmd.requiresParameter)
                    print(", parameter: ${cmd.parameterHelp}")
                println()
            }
        }else if (interpreter == null && str != "read" && str != "reada" && str != "exit"){
            printRepl("No file loaded. Please \"read\" a file to continue.")
        } else if (commands.containsKey(str)) {
            return try{
                commands[str]!!.execute(param)
            } catch (e: Exception) {
                printRepl("Command $str $param caused an exception. Internal state may be inconsistent.")
                e.printStackTrace()
                false
            }
        } else {
            printRepl("Unknown command $str. Enter \"help\" to get a list of available commands.")
        }
        return false
    }
    fun dump() {
        interpreter!!.dump()
        // update ontology from this new dump
        initOntology()
        validDump = true
    }

    fun runAndTerminate(){
        while (!interpreter!!.stack.empty() && interpreter!!.makeStep());
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

        val tC = TypeChecker(tree, settings, pair.second)
        tC.check()
        tC.report()

        val iB = InterpreterBridge(null)
        rules = RuleGenerator(settings).generateBuiltins(tree, iB)


        val initGlobalStore: GlobalMemory = mutableMapOf(Pair(pair.first.obj, mutableMapOf()))

        val initStack = Stack<StackEntry>()
        initStack.push(pair.first)
        interpreter = Interpreter(
            initStack,
            initGlobalStore,
            mutableMapOf(),
            pair.second,
            settings,
            rules,
        )
        iB.interpreter = interpreter
    }

    private fun initCommands() {
        commands["exit"] = Command("exit", this, { true }, "exits the shell")
        commands["read"] = Command(
            "read",
            this,
            { str -> initInterpreter(str); false },
            "reads a file",
            parameterHelp = "Path to a .smol file",
            requiresParameter = true,
            invalidatesDump = true
        )
        commands["reada"] = Command(
            "reada",
            this,
            { str -> initInterpreter(str); while (interpreter!!.makeStep()); false },
            "reads a file and runs auto",
            parameterHelp = "Path to a .smol file",
            requiresParameter = true,
            invalidatesDump = true
        )
        commands["info"] = Command(
            "info",
            this,
            { printRepl(interpreter!!.staticInfo.toString()); false },
            "prints static information in internal format"
        )
        val examine =
            Command("examine", this, { printRepl(interpreter!!.toString()); false }, "prints state in internal format")
        commands["examine"] = examine
        commands["e"] = examine
        commands["dump"] =
            Command("dump", this, { dump(); false }, "dumps into /tmp/mo/output.ttl", invalidatesDump = true)
        commands["auto"] = Command(
            "auto",
            this,
            { while (interpreter!!.makeStep()); false },
            "continues execution until the next breakpoint",
            invalidatesDump = true
        )
        val step = Command(
            "step",
            this,
            { interpreter!!.makeStep(); false },
            "executes one step",
            invalidatesDump = true
        )
        commands["step"] = step
        commands["s"] = step

        val query =  Command(
            "query",
            this,
            { str ->
                val results = interpreter!!.query(str)
                printRepl("\n" + ResultSetFormatter.asText(results))
                false
            },
            "executes a SPARQL query",
            parameterHelp = "SPARQL query",
            requiresParameter = true
        )
        commands["query"] = query
        commands["q"] = query

        commands["plot"] = Command(
            "plot",
            this,
            { str ->
                val params = str.split(" ")
                if(params.size != 4 && params.size != 2) {
                    printRepl("plot expects 2 or 4 parameters, separated by blanks, got ${params.size}.")
                }else {
                    val q = if(params.size == 4)
                         "SELECT ?at ?val WHERE { ?m smol:roleName \"${params[0]}\"; smol:ofPort [smol:withName \"${params[1]}\"]; smol:withValue ?val; smol:atTime ?at. FILTER (?at >= ${params[2]} && ?at <= ${params[3]}) }  ORDER BY ASC(?at)"
                    else "SELECT ?at ?val WHERE { ?m smol:roleName \"${params[0]}\"; smol:ofPort [smol:withName \"${params[1]}\"]; smol:withValue ?val; smol:atTime ?at }  ORDER BY ASC(?at)"
                    val results = interpreter!!.query(q)

                    printRepl("Executed $q")
                    if(results != null) {
                        var out = ""
                        for (r in results) {
                            out += r.getLiteral("at").double.toString() + "\t"+r.getLiteral("val").double.toString() + "\n"
                        }

                        val output = File("${settings.outpath}/plotting.data")
                        if (!output.exists()) output.createNewFile()
                        output.writeText(out)
                        val output2 = File("${settings.outpath}/plotting.gnu")
                        if (!output2.exists()) output.createNewFile()
                        output2.writeText("set terminal postscript \n set output \"${settings.outpath}/out.ps\" \n plot \"${settings.outpath}/plotting.data\" with linespoints")


                        val rt = Runtime.getRuntime()
                        val proc = rt.exec("which gnuplot")
                        proc.waitFor()
                        val exitVal = proc.exitValue()
                        val proc2 = rt.exec("which atril")
                        proc2.waitFor()
                        val exitVal2 = proc2.exitValue()
                        if(exitVal != 0 || exitVal2 != 0){
                            printRepl("Cannot find gnuplot or atril, try to plot and display the files in ${settings.outpath} manually.")
                        } else {
                            printRepl("Plotting....")
                            Runtime.getRuntime().exec("gnuplot ${settings.outpath}/plotting.gnu")
                            Runtime.getRuntime().exec("atril ${settings.outpath}/out.ps")
                        }
                    }
                }
                false
            },
            "plot ROLE PORT FROM TO runs gnuplot on port PORT of role ROLE from FROM to TO. FROM and TO are optional",
            requiresDump = true,
            requiresParameter = true
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
                var hermString = "Ontology:\n"
                ontology.classesInSignature().filter { it.toString().startsWith("<") }
                                                      .forEach { hermString += "Class: $it\n" }
                ontology.objectPropertiesInSignature().filter { it.toString().startsWith("<") }
                                                      .forEach { hermString += "ObjectProperty: $it\n" }
                ontology.individualsInSignature().forEach { hermString += "Individual: $it\n" }


                parser.prefixManager.setPrefix("smol:","<https://github.com/Edkamb/SemanticObjects#>")
                parser.prefixManager.setPrefix("prog:","<urn:>")
                parser.prefixManager.setPrefix("run:","<urn:>")
                parser.setStringToParse(hermString)
                parser.parseOntology(ontology)
                var unprefixed = str.replace("smol:", "https://github.com/Edkamb/SemanticObjects#")
                unprefixed = unprefixed.replace("prog:", "urn:")
                unprefixed = unprefixed.replace("run:", "urn")
                val expr = parser.parseClassExpression(unprefixed)
                val res = reasoner.getInstances(expr)
                printRepl("HermiT result $res")
                false
            },
            "returns all members of a class",
            parameterHelp = "class expression in Manchester Syntax, e.r., \"<smol:Class>\"",
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
            "evaluates a .smol expression in the current frame",
            parameterHelp = "a .smol expression",
            requiresParameter = true,
        )
    }

    fun terminate() {
        interpreter?.terminate()
    }
}
