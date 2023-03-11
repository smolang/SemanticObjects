@file:Suppress("ControlFlowWithEmptyBody")

package no.uio.microobject.runtime

import java.io.File
import java.util.*
import java.time.LocalTime
import java.time.Duration
import no.uio.microobject.antlr.WhileLexer
import no.uio.microobject.antlr.WhileParser
import no.uio.microobject.ast.Expression
import no.uio.microobject.ast.Translate
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
    val command: (String) -> Boolean, // returns `true` if REPL should exit
    val help: String,
    val requiresParameter: Boolean = false,
    val parameterHelp: String = "",
    val requiresFile: Boolean = true
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

    init{
        initCommands()
    }

    fun command(str: String, param: String): Boolean {
        // returns `true` if REPL should exit
        val start = LocalTime.now()
        val result: Boolean
        if (str == "help") {
            for (cmd in commands.values.distinct()) {
                println(String.format("%-11s - %s", cmd.name, cmd.help))
                if (cmd.parameterHelp != "") {
                    println(String.format("%14s- parameter: %s", "", cmd.parameterHelp))
                }
            }
            result = false
        } else {
            val command = commands[str]
            if (command == null) {
                printRepl("Unknown command $str. Enter \"help\" to get a list of available commands.")
                result = false
            } else {
                if (interpreter == null && command.requiresFile){
                    printRepl("No file loaded. Please \"read\" a file to continue.")
                    result = false
                } else {
                    result = try {
                        command.execute(param)
                    } catch (e: Exception) {
                        printRepl("Command $str $param caused an exception. Internal state may be inconsistent.")
                        if(settings.verbose) e.printStackTrace() else printRepl("Trace suppressed, set the verbose flag to print it.")
                        false
                    }
                }
            }
        }
        if (settings.verbose && !result) {
            val elapsedTime = Duration.between(start, LocalTime.now())
            printRepl("Evaluation took ${elapsedTime.seconds}.${elapsedTime.nano} seconds")
        }
        return result
    }
    private fun dump(file: String) {
        interpreter!!.dump(file)
    }

    fun runAndTerminate(){
        while (!interpreter!!.stack.empty() && interpreter!!.makeStep());
    }

    fun printRepl(str: String) {
        println("MO-out> $str \n")
    }

    private fun initInterpreter(path: String) {
        val stdLib = this::class.java.classLoader.getResource("StdLib.smol").readText()
        val program =  File(path).readText(Charsets.UTF_8)
        val lexer = WhileLexer(CharStreams.fromString(program + "\n\n" + stdLib ))
        val tokens = CommonTokenStream(lexer)
        val parser = WhileParser(tokens)
        val tree = parser.program()

        val visitor = Translate()
        val pair = visitor.generateStatic(tree)

        // making a triplemanager without any interpreter instance. This can be used to do type checking.
        val tripleManager = TripleManager(settings, pair.second, null)
        val tC = TypeChecker(tree, settings, tripleManager)
        tC.check()
        tC.report()



        val initGlobalStore: GlobalMemory = mutableMapOf(Pair(pair.first.obj, mutableMapOf()))

        val initStack = Stack<StackEntry>()
        initStack.push(pair.first)
        interpreter = Interpreter(
            initStack,
            initGlobalStore,
            mutableMapOf(),
            mutableMapOf(),
            pair.second,
            settings
        )
    }

    private fun initCommands() {
        commands["exit"] = Command("exit", this, { true }, "exits the shell", requiresFile=false)
        commands["read"] = Command(
            "read",
            this,
            { str -> initInterpreter(str); false },
            "reads a file",
            requiresFile=false,
            parameterHelp = "Path to a .smol file",
            requiresParameter = true
        )
        commands["reada"] = Command(
            "reada",
            this,
            { str -> initInterpreter(str); while (interpreter!!.makeStep()); false },
            "reads a file and runs auto",
            parameterHelp = "Path to a .smol file",
            requiresParameter = true,
            requiresFile=false
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
            Command("dump",
                    this,
                    { str ->
                        val file = if (str == "") "output.ttl" else str
                        dump(file); false
                    },
                    "dumps into \${outdir}/\${file}",
                    parameterHelp = "file: filename, default \"output.ttl\""
            )
        commands["outdir"] = Command(
            "outdir",
            this,
            { str ->
                  if (str == "") {
                      printRepl("Current output directory is ${settings.outdir}")
                  } else {
                      settings.outdir = str
                  }
              false
            },
            "sets or prints the output path",
            parameterHelp = "path (optional): the new value of outdir",
        )

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

        commands["guards"] = Command(
            "guards",
            this,
            { str ->
                val p1 = listOf("heap", "staticTable")
                val p2 = listOf("true", "false")
                val p: List<String> = str.replace("\\s+".toRegex(), " ").trim().split(" ")
                if (p.size != 2) { printRepl("\n" + "This command requires exactly two parameters.") }
                else {
                    if  (!p1.contains(p[0])) { printRepl("\nFirst parameter must one of: $p1") }
                    if  (!p2.contains(p[1])) { printRepl("\nSecond parameter must one of: $p2") }
                    if (p1.contains(p[0]) && p2.contains(p[1])) {
                        interpreter!!.tripleManager.currentTripleSettings.guards[p[0]] = (p[1] == "true")
                        printRepl("Guard clauses in ${p[0]} set to: ${p[1]}")
                    }
                }
                false
            },
            "Enables/disables guard clauses when searching for triples in the heap or the static table",
            parameterHelp = "[heap|staticTable] [true|false]",
            requiresParameter = true
        )

        commands["virtual"] = Command(
            "virtual",
            this,
            { str ->
                val p1 = listOf("heap", "staticTable")
                val p2 = listOf("true", "false")
                val p: List<String> = str.replace("\\s+".toRegex(), " ").trim().split(" ")
                if (p.size != 2) { printRepl("\n" + "This command requires exactly two parameters.") }
                else {
                    if  (!p1.contains(p[0])) { printRepl("\nFirst parameter must one of: $p1") }
                    if  (!p2.contains(p[1])) { printRepl("\nSecond parameter must one of: $p2") }
                    if (p1.contains(p[0]) && p2.contains(p[1])) {
                        interpreter!!.tripleManager.currentTripleSettings.guards[p[0]] = (p[1] == "true")
                        printRepl("Virtualization for ${p[0]} set to: ${p[1]}")
                    }
                }
                false
            },
            "Enables/disables virtualization searching for triples in the heap or the static table. Warning: the alternative to virtualization is naive and slow.",
            parameterHelp = "[heap|staticTable] [true|false]",
            requiresParameter = true
        )

        commands["source"] = Command(
            "source",
            this,
            { str ->
                val p1 = listOf("heap", "staticTable", "vocabularyFile", "externalOntology")
                val p2 = listOf("true", "false")
                val p: List<String> = str.replace("\\s+".toRegex(), " ").trim().split(" ")
                if (p.size != 2) { printRepl("\n" + "This command requires exactly two parameters.") }
                else {
                    if  (!p1.contains(p[0])) { printRepl("\nFirst parameter must one of: $p1") }
                    if  (!p2.contains(p[1])) { printRepl("\nSecond parameter must one of: $p2") }
                    if (p1.contains(p[0]) && p2.contains(p[1])) {
                        interpreter!!.tripleManager.currentTripleSettings.sources[p[0]] = (p[1] == "true")
                        printRepl("Use source ${p[0]} set to ${p[1]}")
                    }
                }
                false
            },
            "Set which sources to include (true) or exclude (false) when querying",
            parameterHelp = "[heap|staticTable|vocabularyFile|externalOntology] [true|false]",
            requiresParameter = true
        )

        commands["reasoner"] = Command(
            "reasoner",
            this,
            { str ->
                val allowedParameters = listOf("off", "rdfs", "owl")
                val p: List<String> = str.replace("\\s+".toRegex(), " ").trim().split(" ")
                if (p.size != 1) { printRepl("\n" + "This command requires exactly one parameter.") }
                else {
                    if  (!allowedParameters.contains(p[0])) { printRepl("\nParameter must one of: $allowedParameters") }
                    else {
                        interpreter!!.tripleManager.currentTripleSettings.jenaReasoner = p[0]
                        printRepl("Reasoner changed to: ${p[0]}")
                    }
                }
                false
            },
            "Specify which Jena reasoner to use, or turn it off",
            parameterHelp = "[off|rdfs|owl]",
            requiresParameter = true
        )

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

                        val output = File("${settings.outdir}/plotting.tsv")
                        if (!output.exists()) output.createNewFile()
                        output.writeText(out)
                        val output2 = File("${settings.outdir}/plotting.gp")
                        if (!output2.exists()) output.createNewFile()
                        output2.writeText("set terminal postscript \n set output \"${settings.outdir}/out.ps\" \n plot \"${settings.outdir}/plotting.tsv\" with linespoints")


                        val rt = Runtime.getRuntime()
                        val proc = rt.exec("which gnuplot")
                        proc.waitFor()
                        val exitVal = proc.exitValue()
                        val proc2 = rt.exec("which atril")
                        proc2.waitFor()
                        val exitVal2 = proc2.exitValue()
                        if(exitVal != 0 || exitVal2 != 0){
                            printRepl("Cannot find gnuplot or atril, try to plot and display the files in ${settings.outdir} manually.")
                        } else {
                            printRepl("Plotting....")
                            Runtime.getRuntime().exec("gnuplot ${settings.outdir}/plotting.gp")
                            Runtime.getRuntime().exec("atril ${settings.outdir}/out.ps")
                            printRepl("Finished. Generated files are in ${settings.outdir}.")
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
                val ontology = interpreter!!.tripleManager.getOntology()
                val reasoner : OWLReasoner = Reasoner.ReasonerFactory().createReasoner(ontology)
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
        commands["verbose"] = Command(
            "verbose",
            this,
            { str ->
                if (str == "on" || str == "true") {
                    settings.verbose = true
                } else if (str == "off" || str == "false") {
                    settings.verbose = false
                }
                false
            },
            "Sets verbose output to on or off",
            parameterHelp = "`true` or `on` to switch on verbose output, `false` or `off` to switch it off",
            requiresParameter = true,
            requiresFile=false
        )
    }

    fun terminate() {
        interpreter?.terminate()
    }
}
