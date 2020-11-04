package microobject.main

import microobject.runtime.REPL
import java.io.File

fun main(args: Array<String>) {
    if(args.size > 0) {
        val repl = REPL(args[0])
        if (args.size == 2) {
            val str = args[1]
            File(str).forEachLine {
                if(!it.startsWith("#")) {
                    println("MO> $it")
                    val splits = it.split(" ", limit = 2)
                    repl.command(splits.first(), splits.subList(1, splits.size))
                }
            }
        } else {
            val repl = REPL(args[0])
            do {
                print("MO>")
                val next = readLine() ?: break
                val splits = next.split(" ", limit = 2)
            } while (!repl.command(splits.first(), splits.subList(1, splits.size)))
            println("Have a nice day")
        }
    }
}



