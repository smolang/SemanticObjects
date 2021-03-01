package microobject.main

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.deprecated
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import microobject.runtime.REPL
import java.io.File
import java.nio.file.Paths
import kotlin.system.exitProcess

data class Settings(val verbose : Boolean,      //Verbosity
                    val outpath : String,       //path of temporary outputs
                    val background : String,    //owl background knowledge
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
    fun prefixes() : String =
        """@prefix : <urn:> .
           @prefix smol: <${langPrefix}> .
           @prefix prog: <${progPrefix}>.
           @prefix domain: <${domainPrefix}>.
           @prefix run: <${runPrefix}> .""".trimIndent()
}

class Main : CliktCommand() {
    private val ninteractive by option("--non-interactive","-n",help="Does not enter the interactive shell, but executes the loaded file if no replay file is given.").flag()
    private val verbose      by option("--verbose","-v",help="Verbose output.").flag()
    private val tmp          by option("--tmp","-t",help="path to a directory used to store temporary files.").path().default(Paths.get("/tmp/mo"))
    private val apache       by option("--jena","-j",help="path to the bin/ directory of an apache jena installation (used for SHACL and SPARQL queries from files).").path().deprecated()
    private val replay       by option("--replay","-r",help="path to a file containing a series of shell commands.").path()
    private val load         by option("--load","-l",help="path to a .smol file which is loaded on startup.").path()
    private val back         by option("--back","-b",help="path to a .ttl file that contains OWL class definitions as background knowledge.").path()
    private val domainPrefix by option("--domain","-d",help="prefix for domain:.").default("http://github.com/edkamb/SemanticObjects/ontologies/default#")

    override fun run() {
        val pathJena = if( apache == null ) "" else apache.toString()
        org.apache.jena.query.ARQ.init()

        if(ninteractive && load == null){
            println("Error: Missing option \"--load\".")
            exitProcess(-1)
        }

        var backgr = ""
        if(back != null){
            val file = File(back.toString())
            if(file.exists()){
                backgr = file.readText()
            }else println("Could not find file for background knowledge: ${file.path}")
        }

        val repl = REPL(pathJena, Settings(verbose, tmp.toString(), backgr, domainPrefix))
        if(load != null){
            repl.command("read", load.toString())
        }
        if(replay != null){
            val str = replay.toString()
            File(str).forEachLine {
                if(!it.startsWith("#")) {
                    println("MO-auto> $it")
                    val splits = it.split(" ", limit = 2)
                    val left = if(splits.size == 1) "" else splits[1]
                    repl.command(splits.first(), left)
                }
            }
        }
        if(!ninteractive){
            println("Interactive shell started.")
            do {
                print("MO>")
                val next = readLine() ?: break
                val splits = next.split(" ", limit = 2)
                val left = if(splits.size == 1) "" else splits[1]
            } while (!repl.command(splits.first(), left))
        }else if(replay == null){
            repl.command("auto", "")
        }
        repl.terminate()
    }
}

fun main(args:Array<String>) = Main().main(args)
