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
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.semanticweb.owlapi.model.OntologyConfigurator
import org.semanticweb.owlapi.reasoner.OWLReasoner
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*


class REPL(private var apache: String) {
    private var interpreter : Interpreter? = null
    fun command(str: String, params: List<String>) : Boolean{

        when (str){
            "exit" -> {
                /** exit loop **/
                return true
            }
            "read" -> {
                /** read a .mo file **/
                if (params.size != 1) {
                    printRepl("Command $str expects 1 parameter (/path/to/.mo/file)")
                    return false
                }
                initInterpreter(params[0])
            }
            "examine" -> {
                /** standard debug output for the dynamic state **/
                if (interpreter == null) {
                    printRepl("Interpreter not initialized")
                    return false
                }
                printRepl(interpreter!!.toString())

            }
            "info" -> {
                /** standard debug output for static information **/
                if (interpreter == null) {
                    printRepl("Interpreter not initialized")
                    return false
                }
                printRepl(interpreter!!.staticInfo.toString())
            }
            "dump" -> {
                /** turtle dump. Returns a file path to use the dump **/
                if (interpreter == null) {
                    printRepl("Interpreter not initialized")
                    return false
                }
                val res = interpreter!!.dumpTtl()
                //printRepl(res)

                val output = File("/tmp/mo/output.ttl")
                output.parentFile.mkdirs()
                if (!output.exists()) output.createNewFile()
                output.writeText(res)
            }
            "validate" -> {
                /** validates against a SHACL file**/
                if (params.size != 1) {
                    printRepl("Command $str expects 1 parameter (/path/to/.shapes/file)")
                    return false
                }
                if (interpreter == null) {
                    printRepl("Interpreter not initialized")
                    return false
                }
                printRepl("Dumping current state....")
                this.command("dump", listOf())

                val command =
                    "$apache/shacl validate --data /tmp/mo/output.ttl --shapes ${params[0]}"

                val p = Runtime.getRuntime().exec(command)
                p.waitFor()
                var str = "jena output: \n"
                val lineReader = BufferedReader(InputStreamReader(p.inputStream))
                lineReader.lines().forEach { x: String? -> if (x != null) str += "$x\n" }
                printRepl(str)
                return false

            }
            "query" -> {
                /** executes a SPARQL query **/
                if (params.size != 1) {
                    printRepl("Command $str expects 1 parameter (SPARQL query) ")
                    return false
                }
                val out =
                    """
                    PREFIX : <urn:absolute:Test:>
                    PREFIX owl: <http://www.w3.org/2002/07/owl#> 
                    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> 
                    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> 
                    
                    ${params[0]}
                """.trimIndent()
                val output = File("/tmp/mo/output.rq")
                output.parentFile.mkdirs()
                if (!output.exists()) output.createNewFile()
                output.writeText(out)

                this.command("query-file", listOf("/tmp/mo/output.rq"))
            }
            "query-file" -> {
                /** executes a SPARQL query file **/
                if (params.size != 1) {
                    printRepl("Command $str expects 1 parameter (/path/to/.query/file)")
                    return false
                }
                if (interpreter == null) {
                    printRepl("Interpreter not initialized")
                    return false
                }
                printRepl("Dumping current state....")
                this.command("dump", listOf())

                val command =
                    "$apache/sparql --data=/tmp/mo/output.ttl --query=${params[0]}"
                val p = Runtime.getRuntime().exec(command)
                p.waitFor()
                var str = "jena output: \n"
                val lineReader = BufferedReader(InputStreamReader(p.inputStream))
                lineReader.lines().forEach { x: String? -> if (x != null) str += "$x\n" }
                printRepl(str)
                return false
            }
            "consistency" -> {
                if (interpreter == null) {
                    printRepl("Interpreter not initialized")
                    return false
                }

                this.command("dump", listOf())

                val m: OWLOntologyManager = OWLManager.createOWLOntologyManager()
                val o: OWLOntology = m.loadOntologyFromOntologyDocument(File("/tmp/mo/output.ttl"))
                val reasoner: OWLReasoner = Reasoner.ReasonerFactory().createReasoner(o)
                println(reasoner.isConsistent)
            }
            "class" -> {
                if (params.size != 1) {
                    printRepl("Command $str expects 1 parameter (.mo expression)")
                    return false
                }
                if (interpreter == null) {
                    printRepl("Interpreter not initialized")
                    return false
                }

                this.command("dump", listOf())

                val m: OWLOntologyManager = OWLManager.createOWLOntologyManager()
                val o: OWLOntology = m.loadOntologyFromOntologyDocument(File("/tmp/mo/output.ttl"))
                val reasoner: OWLReasoner = Reasoner.ReasonerFactory().createReasoner(o)
                val parser = ManchesterOWLSyntaxParserImpl(OntologyConfigurator(), m.owlDataFactory)
                parser.setDefaultOntology(o)
                val res = reasoner.getInstances(parser.parseClassExpression(params[0]))
                println(res)
            }
            "eval" -> {
                /** evaluates a .mo expression **/
                if (params.size != 1) {
                    printRepl("Command $str expects 1 parameter (.mo expression)")
                    return false
                }
                if (interpreter == null) {
                    printRepl("Interpreter not initialized")
                    return false
                }

                val newString = params[0]
                val lexer2 = WhileLexer(CharStreams.fromString(newString))
                val tokens2 = CommonTokenStream(lexer2)
                val parser2 = WhileParser(tokens2)
                val tree2 = parser2.expression()
                val visitor2 = Translate()
                val newExpr = visitor2.visit(tree2) as Expression

                printRepl(interpreter!!.evalTopMost(newExpr).literal)
            }
            "step" -> {
                /** executes next step in the interpreter **/
                if (interpreter == null) {
                    printRepl("Interpreter not initialized")
                    return false
                }
                interpreter!!.makeStep()
            }
            "auto" -> {
                /** executes rest of program (until next breakpoint) **/
                if (interpreter == null) {
                    printRepl("Interpreter not initialized")
                    return false
                }
                while (interpreter!!.makeStep());
            }
            "help" -> {
                /** prints help text **/
                printRepl(
                    """
                    
                    help                - prints this message
                    exit                - exits interactive shell
                    read <file>         - loads a .mo file
                    step                - executes one step
                    auto                - executes until termination or breakpoint
                    examine             - prints current state
                    dump                - prints current state in turtle format
                    info                - prints static information
                    eval <expr>         - evaluates an expression in the current state (in the topmost frame)
                    query <expr>        - executes a SPARQL query
                    query-file <file>   - executes a SPARQL query file
                    validate <file>     - validates against a SHACL graph file
                """.trimIndent()
                )

            }
            else -> printRepl("Unknown command \"$str\"")
        }
        return false
    }

    private fun printRepl(str: String){
        println("MO> $str \n")
    }

    private fun initInterpreter(path: String){
        val lexer = WhileLexer(CharStreams.fromFileName(path))
        val tokens = CommonTokenStream(lexer)
        val parser = WhileParser(tokens)
        val tree = parser.program()

        val visitor = Translate()
        val pair = visitor.generateStatic(tree)


        val initGlobalStore : GlobalMemory = mutableMapOf(Pair(pair.first.obj, mutableMapOf()))

        val initStack = Stack<StackEntry>()
        initStack.push(pair.first)
        interpreter = Interpreter(
            initStack,
            initGlobalStore,
            pair.second
        )
    }
}