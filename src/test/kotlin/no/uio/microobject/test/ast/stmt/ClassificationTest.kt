package no.uio.microobject.test.ast.stmt

import io.kotest.matchers.shouldBe
import no.uio.microobject.ast.expr.LocalVar
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

    // Regression test for the declared target form: `A x = classify(a);`
    private fun classifyDeclaredTest() {
        loadBackground("src/test/resources/classification_example.ttl", "http://www.smolang.org/ex#")
        val (interpreter, _) = initInterpreter("classification_declared", StringLoad.RES, hashMapOf("ast" to "http://www.smolang.org/ex#"))

        executeUntilBreak(interpreter)

        // The classification yields a new C object that is assigned to x
        val x = interpreter.evalTopMost(LocalVar("x"))
        x.tag shouldBe BaseType("C")
        interpreter.heap.containsKey(x) shouldBe true

        // The source object a is untouched and still a B object
        val a = interpreter.evalTopMost(LocalVar("a"))
        a.tag shouldBe BaseType("B")
        interpreter.heap.containsKey(a) shouldBe true
    }

    // A retrieves query that matches no row is a normal outcome: no NPE, the object stays unchanged
    private fun emptyRetrievesTest() {
        loadBackground("src/test/resources/classification_example.ttl", "http://www.smolang.org/ex#")
        val (interpreter, _) = initInterpreter("empty_retrieves", StringLoad.RES, hashMapOf("ast" to "http://www.smolang.org/ex#"))

        executeUntilBreak(interpreter)

        val a = interpreter.evalTopMost(LocalVar("a"))
        a.tag shouldBe BaseType("B")
        interpreter.heap.containsKey(a) shouldBe true
    }

    init {
        "eval" {
            classifyTest()
        }
        "eval declared target" {
            classifyDeclaredTest()
        }
        "eval empty retrieves" {
            emptyRetrievesTest()
        }
    }
}
