@file:Suppress("ControlFlowWithEmptyBody")

package no.uio.microobject.runtime

import java.io.File
import java.util.*
import no.uio.microobject.antlr.WhileLexer
import no.uio.microobject.antlr.WhileParser
import no.uio.microobject.data.Expression
import no.uio.microobject.data.Translate
import no.uio.microobject.data.TripleManager
import no.uio.microobject.main.Settings
import no.uio.microobject.type.TypeChecker
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.apache.jena.query.ResultSetFormatter
import org.semanticweb.HermiT.Reasoner
import org.semanticweb.owlapi.reasoner.OWLReasoner

class Command(
    val name: String,
    private val repl: REPL,
    val command: (String) -> Boolean,
    val help: String,
    val requiresParameter: Boolean = false,
    val parameterHelp: String = ""
){
    fun execute(param: String): Boolean {
        if (requiresParameter && param == "") {
            repl.printRepl("Command $name expects 1 parameter $parameterHelp.")
            return false
        }
        return command(param)
    }
}

@Suppress("DEPRECATION") // ReasonerFactory is deprecated by HermiT but I keep it like this to make a change easier
class REPL(private val settings: Settings) {
    private var interpreter: Interpreter? = null
    private val commands: MutableMap<String, Command> = mutableMapOf()
    private var rules = ""
    init{
        initCommands()
    }

    fun command(str: String, param: String): Boolean {
        if (str == "help") {
            for (cmd in commands.values.toSet().sortedBy { it.name })
                print("${cmd.name}\n\t- ${cmd.help}")
            for (cmd in commands.values.distinct()) {
                print("${cmd.name}\t\t- ${cmd.help}")
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
    }

    fun runAndTerminate(){
        while (!interpreter!!.stack.empty() && interpreter!!.makeStep());
    }

    fun printRepl(str: String) {
        println("MO> $str \n")
    }

    private fun initInterpreter(path: String) {
        val stdLib = this::class.java.classLoader.getResource("StdLib.smol").readText() + "\n\n"
        val program =  File(path).readText(Charsets.UTF_8)
        val lexer = WhileLexer(CharStreams.fromString(stdLib + program))
        val tokens = CommonTokenStream(lexer)
        val parser = WhileParser(tokens)
        val tree = parser.program()

        val visitor = Translate()
        val pair = visitor.generateStatic(tree)

        // making a triplemanager without any interpreter instance. This can be used to do type checking.
        var tripleManager = TripleManager(settings, pair.second, null)
        val tC = TypeChecker(tree, settings, tripleManager)
        tC.check()
        tC.report()

        val iB = InterpreterBridge(null)


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
            requiresParameter = true
        )
        commands["reada"] = Command(
            "reada",
            this,
            { str -> initInterpreter(str); while (interpreter!!.makeStep()); false },
            "reads a file and runs auto",
            parameterHelp = "Path to a .smol file",
            requiresParameter = true
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
            Command("dump", this, { dump(); false }, "dumps into \${tmp_path}/output.ttl")
        commands["auto"] = Command(
            "auto",
            this,
            { while (interpreter!!.makeStep()); false },
            "continues execution until the next breakpoint"
        )
        val step = Command(
            "step",
            this,
            { interpreter!!.makeStep(); false },
            "executes one step"
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
            requiresParameter = true
        )
        commands["consistency"] = Command(
            "consistency",
            this,
            { _ ->
                var ontology = interpreter!!.tripleManager.getCompleteOntology()
                var reasoner : OWLReasoner = Reasoner.ReasonerFactory().createReasoner(ontology)
                ontology.classesInSignature().forEach { println(it) }
                printRepl("HermiT result ${reasoner.isConsistent}")
                false
            },
            "prints all classes and checks that the internal ontology is consistent"
        )
        commands["class"] = Command(
            "class",
            this,
            { str ->
                var outString = "Instances of $str:\n"
                for (node in interpreter!!.owlQuery(str)) {
                    // N node can appearently have more than one entity. Print all entities in all nodes.
                    var prefix = ""
                    for (entity in node.entities) {
                        outString = outString + prefix + "$entity"
                        prefix = ", "
                    }
                    outString += "\n"
                }
                printRepl(outString)
                false
            },
            "returns all members of a class",
            parameterHelp = "class expression in Manchester Syntax, e.r., \"<smol:Class>\"",
            requiresParameter = true
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
