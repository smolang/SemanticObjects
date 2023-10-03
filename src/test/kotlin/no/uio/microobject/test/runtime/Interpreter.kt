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

    init {
        "eval" {
            evalTest()
        }
    }
}