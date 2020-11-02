package microobject.main

import microobject.runtime.REPL
import java.io.File

fun main(args: Array<String>) {
    val autoMode = true

    if(autoMode) {
        val repl = REPL()
        val str = "/home/edkam/src/keyma/MicroObjects/test.mi"
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



