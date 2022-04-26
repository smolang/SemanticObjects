package no.uio.microobject.main

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import org.jline.reader.LineReaderBuilder
import no.uio.microobject.runtime.REPL
import java.io.File
import java.nio.file.Paths
import kotlin.system.exitProcess

data class Settings(var verbose : Boolean,      //Verbosity
                    val materialize : Boolean,  //Materialize
                    val outpath : String,       //path of temporary outputs
                    val background : String,    //owl background knowledge
                    val domainPrefix : String,  //prefix used in the domain model (domain:)
                    val progPrefix : String = "https://github.com/Edkamb/SemanticObjects/Program#",    //prefix for the program (prog:)
                    val runPrefix : String  = "https://github.com/Edkamb/SemanticObjects/Run${System.currentTimeMillis()}#",    //prefix for this run (run:)
                    val langPrefix : String = "https://github.com/Edkamb/SemanticObjects#",
                    val extraPrefixes : HashMap<String, String>
                    ){
    var prefixMapCache: HashMap<String, String>? = null
    fun prefixMap() : HashMap<String, String> {
        if(prefixMapCache != null) return prefixMapCache as HashMap<String, String>
        prefixMapCache = hashMapOf(
            "domain" to domainPrefix,
            "smol" to langPrefix,
            "prog" to progPrefix,
            "run" to runPrefix,
            "owl" to "http://www.w3.org/2002/07/owl#",
            "rdf" to "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
            "rdfs" to "http://www.w3.org/2000/01/rdf-schema#",
            "xsd" to "http://www.w3.org/2001/XMLSchema#"
        )
        prefixMapCache!!.putAll(extraPrefixes)
        return prefixMapCache as HashMap<String, String>
    }

    fun replaceKnownPrefixes(string: String) : String{
        var res = string.replace("domain:", "$domainPrefix:")
            .replace("prog:", "$progPrefix:")
            .replace("run:", "$runPrefix:")
            .replace("smol:", "$langPrefix:")
        for( (k,v) in extraPrefixes) res = res.replace("$k:", "$v:")
        return res;
    }
    fun replaceKnownPrefixesNoColon(string: String) : String{ //For the HermiT parser, BEWARE: requires that the prefixes and in #
        var res= string.replace("domain:", domainPrefix)
            .replace("prog:", progPrefix)
            .replace("run:", runPrefix)
            .replace("smol:", langPrefix)
        for( (k,v) in extraPrefixes) res = res.replace("$k:", v)
        return res;
    }
    fun prefixes() : String {
        var res = """@prefix smol: <${langPrefix}> .
           @prefix prog: <${progPrefix}>.
           @prefix domain: <${domainPrefix}>.
           @prefix run: <${runPrefix}> .""".trimIndent()
        for( (k,v) in extraPrefixes) res = "$res\n@prefix $k: <$v>."
        return res + "\n";
    }

    fun getHeader() : String {
        return """
        @prefix owl: <http://www.w3.org/2002/07/owl#> .
        @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
        @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
        @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
        """.trimIndent()
    }
}

class Main : CliktCommand() {
    private val mainMode by option().switch(
        "--execute" to "execute", "-e" to "execute",
        "--load" to "repl",       "-l" to "repl",
    ).default("repl")

    private val back         by option("--back",      "-b",  help="path to a .ttl file that contains OWL class definitions as background knowledge.").path()
    private val domainPrefix by option("--domain",    "-d",  help="prefix for domain:.").default("https://github.com/Edkamb/SemanticObjects/ontologies/default#")
    private val input        by option("--input",     "-i",  help="path to a .smol file which is loaded on startup.").path()
    private val replay       by option("--replay",    "-r",  help="path to a file containing a series of shell commands.").path()
    private val tmp          by option("--tmp",       "-t",  help="path to a directory used to store temporary files.").path().default(Paths.get("/tmp/mo"))
    private val verbose      by option("--verbose",   "-v",  help="Verbose output.").flag()
    private val materialize  by option("--materialize", "-m",  help="Materialize triples and dump to file.").flag()
    private val extra        by option("--prefixes", "-p", help="Extra prefixes, given as a list PREFIX=URI").associate()

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

        if (input == null && mainMode != "repl"){
            println("Error: please specify an input .smol file using \"--input\".")
            exitProcess(-1)
        }

        val repl = REPL( Settings(verbose, materialize, tmp.toString(), backgr, domainPrefix, extraPrefixes=HashMap(extra)))
        if(input != null){
            repl.command("read", input.toString())
        }
        if(replay != null){
            val str = replay.toString()
            File(str).forEachLine {
                if(!it.startsWith("#") && it != "") {
                    println("MO-auto> $it")
                    val splits = it.split(" ", limit = 2)
                    val left = if(splits.size == 1) "" else splits[1]
                    repl.command(splits.first(), left)
                }
            }
        }
        if(mainMode == "repl"){
            println("Interactive shell started.")
            val reader = LineReaderBuilder.builder().build()
            do {
                val next = reader.readLine("MO> ") ?: break
                val splits = next.trim().split(" ", limit = 2)
                val left = if(splits.size == 1) "" else splits[1].trim()
            } while (!repl.command(splits.first(), left))
        }else if(replay == null){
            repl.runAndTerminate() //command("auto", "");
        }
        repl.terminate()
    }
}

fun main(args:Array<String>) = Main().main(args)
