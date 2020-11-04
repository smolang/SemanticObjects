package microobject.main

import microobject.runtime.REPL
import java.io.File

fun main(args: Array<String>) {
    if(args.isNotEmpty()) {
        val repl = REPL(args[0])
        if (args.size == 2) {
            val str = args[1]
            File(str).forEachLine {
                if(!it.startsWith("#")) {
                    println("MO> $it")
                    val splits = it.split(" ", limit = 2)
                    val left = if(splits.size == 1) "" else splits[1]
                    repl.command(splits.first(), left)
                }
            }
        } else {
            do {
                print("MO>")
                val next = readLine() ?: break
                val splits = next.split(" ", limit = 2)
                val left = if(splits.size == 1) "" else splits[1]
            } while (!repl.command(splits.first(), left))
            println("Have a nice day")
        }
    }
}



