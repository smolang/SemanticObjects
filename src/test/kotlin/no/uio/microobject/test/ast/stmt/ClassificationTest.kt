package no.uio.microobject.test.ast.stmt

import io.kotest.matchers.shouldBe
import no.uio.microobject.test.MicroObjectTest
import no.uio.microobject.type.BaseType

class ClassificationTest : MicroObjectTest() {
    private fun classifyTest() {
        loadBackground("src/test/resources/classification_example.ttl", "http://www.smolang.org/ex#")
        val (interpreter, _) = initInterpreter("classification", StringLoad.RES, hashMapOf("ast" to "http://www.smolang.org/ex#"))

        executeUntilBreak(interpreter)
        val keys = interpreter.heap.keys

        // Check if the tag of one of the keys (speficially it will be the run obj) is C
        keys.any { it.tag == BaseType("C") } shouldBe true
    }

    init {
        "eval" {
            classifyTest()
        }
    }
}