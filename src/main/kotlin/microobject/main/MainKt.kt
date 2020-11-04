package microobject.main

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import microobject.runtime.REPL
import java.io.File
import java.nio.file.Paths


class Main : CliktCommand() {
    private val ninteractive by option("--non-interactive","-n",help="Does not enter the interactive shell.").flag()
    private val verbose      by option("--verbose","-v",help="Verbose output.").flag()
    private val tmp          by option("--tmp","-t",help="path to a directory used to store temporary files.").path().default(Paths.get("/tmp/mo"))
    private val apache       by option("--jena","-j",help="path to the bin/ directory of an apache jena installation (used for SHACL and SPARQL queries).").path()
    private val replay       by option("--replay","-r",help="path to a file containing a series of shell commands.").path()
    private val load         by option("--load","-k",help="path to a .mo file which is loaded on startup.").path()

    override fun run() {
        val pathJena = if( apache == null ) "" else apache.toString()
        val repl = REPL(pathJena, tmp.toString(), verbose)
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

        }
    }
}

fun main(args:Array<String>) = Main().main(args)