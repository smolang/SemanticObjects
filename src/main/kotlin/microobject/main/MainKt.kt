package microobject.main

import microobject.runtime.REPL
import java.io.File

fun main(args: Array<String>) {
    val repl = REPL()
    if(args.size == 1){
        val str = args[0]
        File(str).forEachLine {
            val splits = it.split(" ", limit = 2)
            repl.command(splits.first(), splits.subList(1, splits.size))
        }
    } else {
        val repl = REPL()
        do {
            print(">")
            val next = readLine() ?: break
            val splits = next.split(" ", limit = 2)
        } while (!repl.command(splits.first(), splits.subList(1, splits.size)))
        println("Have a nice day")
    }
}



