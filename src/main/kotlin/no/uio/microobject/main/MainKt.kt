package no.uio.microobject.main

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.types.path
import no.uio.microobject.antlr.WhileLexer
import no.uio.microobject.antlr.WhileParser
import no.uio.microobject.backend.JavaBackend
import no.uio.microobject.data.Translate
import no.uio.microobject.runtime.REPL
import no.uio.microobject.type.TypeChecker
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File
import java.nio.file.Paths
import kotlin.system.exitProcess

data class Settings(val verbose : Boolean,      //Verbosity
                    val materialize : Boolean,  //Materialize
                    val outpath : String,       //path of temporary outputs
                    val background : String,    //owl background knowledge
                    val backgroundrules : String,    //owl background knowledge
                    val domainPrefix : String,  //prefix used in the domain model (domain:)
                    val progPrefix : String = "https://github.com/Edkamb/SemanticObjects/Program#",    //prefix for the program (prog:)
                    val runPrefix : String  = "https://github.com/Edkamb/SemanticObjects/Run${System.currentTimeMillis()}#",    //prefix for this run (run:)
                    val langPrefix : String = "https://github.com/Edkamb/SemanticObjects#"
                    ){
    fun replaceKnownPrefixes(string: String) : String{
        return string.replace("domain:", "$domainPrefix:")
            .replace("prog:", "$progPrefix:")
            .replace("run:", "$runPrefix:")
            .replace("smol:", "$langPrefix:")
    }
    fun replaceKnownPrefixesNoColon(string: String) : String{ //For the HermiT parser, BEWARE: requires that the prefixes and in #
        return string.replace("domain:", domainPrefix)
            .replace("prog:", progPrefix)
            .replace("run:", runPrefix)
            .replace("smol:", langPrefix)
    }
    fun prefixes() : String =
        """@prefix smol: <${langPrefix}> .
           @prefix prog: <${progPrefix}>.
           @prefix domain: <${domainPrefix}>.
           @prefix run: <${runPrefix}> .""".trimIndent()
}

class Main : CliktCommand() {
    val mainMode by option().switch(
        "--compile" to "compile", "-c" to "compile",
        "--execute" to "execute", "-e" to "execute",
        "--load" to "repl",       "-l" to "repl",
    ).default("repl")

    private val back         by option("--back",        "-b",  help="path to a .ttl file that contains OWL class definitions as background knowledge.").path()
    private val backrules    by option("--backrules",   "-br", help="path to a file that contains derivation rules in Jena syntax.").path()
    private val domainPrefix by option("--domain",      "-d",  help="prefix for domain:.").default("https://github.com/edkamb/SemanticObjects/ontologies/default#")
    private val input        by option("--input",       "-i",  help="path to a .smol file which is loaded on startup.").path()
    private val jOut         by option("--output",      "-o",  help="Java output.").path().default(Paths.get("/tmp/mo/java"))
    private val jPackage     by option("--package",     "-p",  help="Java package.").default("no.uio.microobject")
    private val jEnforce     by option("--semantic",    "-s",  help="Enforces a semantic translation, even in a non-semantic fragment.").flag()
    private val replay       by option("--replay",      "-r",  help="path to a file containing a series of shell commands.").path()
    private val tmp          by option("--tmp",         "-t",  help="path to a directory used to store temporary files.").path().default(Paths.get("/tmp/mo"))
    private val verbose      by option("--verbose",     "-v",  help="Verbose output.").flag()
    private val materialize  by option("--materialize", "-m",  help="Materialize triples and dump to file.").flag()

    override fun run() {
        org.apache.jena.query.ARQ.init()

        //check that background knowledge exists
        var backgr = ""
        if(back != null){
            val file = File(back.toString())
            if(file.exists()){
                backgr = file.readText()
            }else println("Could not find file for background knowledge: ${file.path}")
        }

        var backgrrules = ""
        if(backrules != null){
            val file = File(backrules.toString())
            if(file.exists()){
                backgrrules = file.readText()
            }else println("Could not find file for background knowledge: ${file.path}")
        }

        if (input == null && mainMode != "repl"){
            println("Error: please specify an input .smol file using \"--input\".")
            exitProcess(-1)
        }

        if(mainMode == "compile") {
            val lexer = WhileLexer(CharStreams.fromFileName(input.toString()))
            val tokens = CommonTokenStream(lexer)
            val parser = WhileParser(tokens)
            val tree = parser.program()

            val visitor = Translate()
            val pair = visitor.generateStatic(tree)

            val tC = TypeChecker(tree, Settings(verbose, materialize, tmp.toString(), backgr, backgrrules, domainPrefix), pair.second)
            tC.check()
            tC.report()

            val backend = JavaBackend(tree, pair.first.active, pair.second, jPackage, jEnforce)
            backend.writeOutput(jOut)
            print(backend.getOutput())
            return
        }

        val repl = REPL( Settings(verbose, materialize, tmp.toString(), backgr, backgrrules, domainPrefix))
        if(input != null){
            repl.command("read", input.toString())
        }
        if(replay != null){
            val str = replay.toString()
            File(str).forEachLine {
                if(!it.startsWith("#") && !(it == "")) {
                    println("MO-auto> $it")
                    val splits = it.split(" ", limit = 2)
                    val left = if(splits.size == 1) "" else splits[1]
                    repl.command(splits.first(), left)
                }
            }
        }
        if(mainMode == "repl"){
            println("Interactive shell started.")
            do {
                print("MO>")
                val next = readLine() ?: break
                val splits = next.split(" ", limit = 2)
                val left = if(splits.size == 1) "" else splits[1]
            } while (!repl.command(splits.first(), left))
        }else if(replay == null){
            repl.runAndTerminate() //command("auto", "");
        }
        repl.terminate()
    }
}

fun main(args:Array<String>) = Main().main(args)
