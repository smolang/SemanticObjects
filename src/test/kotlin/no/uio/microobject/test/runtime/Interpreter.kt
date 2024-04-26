package no.uio.microobject.test.runtime

import io.kotest.matchers.shouldBe
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.test.MicroObjectTest
import no.uio.microobject.type.*

class Interpreter : MicroObjectTest() {
    private fun evalTest() {
        val (interpreter,_) = initInterpreter("persons", StringLoad.RES)
        val expr = LiteralExpr("50.0")
        val result = interpreter.eval(expr, interpreter.stack.peek())
        result shouldBe LiteralExpr("50.0")
    }

    private fun getObjectNamesTest() {
        val (interpreter,_) = initInterpreter("persons", StringLoad.RES)

        executeUntilBreak(interpreter)
        val marriage = interpreter.getObjectNames("Marriage")
        val person = interpreter.getObjectNames("Person")

        marriage.size shouldBe 1
        person.size shouldBe 2
    }

    private fun evalParamsTest() {
        val (interpreter, _) = initInterpreter("eval-params", StringLoad.RES)
        executeUntilBreak(interpreter)

        interpreter.evalCall(interpreter.getObjectNames("A")[0].toString(), "A", "setX", mapOf("newX" to LiteralExpr("1", BaseType("int"))))
        val queryRes = interpreter.query("SELECT ?x WHERE { ?obj a prog:A ; prog:A_x ?x }")
        queryRes!!.hasNext() shouldBe true

        val x = queryRes.next().get("x")
        x.toString() shouldBe interpreter.settings.runPrefix + "1"
    }

    init {
        "eval" {
            evalTest()
            getObjectNamesTest()
            evalParamsTest()
        }
    }
}