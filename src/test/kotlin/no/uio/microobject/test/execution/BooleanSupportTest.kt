package no.uio.microobject.test.execution

import io.kotest.matchers.shouldBe
import no.uio.microobject.ast.expr.FALSEEXPR
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.ast.expr.LocalVar
import no.uio.microobject.ast.expr.OthersVar
import no.uio.microobject.ast.expr.TRUEEXPR
import no.uio.microobject.test.MicroObjectTest
import no.uio.microobject.type.STRINGTYPE

class BooleanSupportTest : MicroObjectTest() {

    private fun booleanToStringTest() {
        val (a, _) = initInterpreter("String s = booleanToString(True); breakpoint; print(s);", StringLoad.STMT)
        executeUntilBreak(a)
        a.evalTopMost(LocalVar("s", STRINGTYPE)) shouldBe LiteralExpr("True", STRINGTYPE)
    }

    private fun booleanAccessTest() {
        val (a, _) = initInterpreter("boolean_access", StringLoad.RES)
        executeUntilBreak(a)

        // The access query with FILTER (?v = false) must yield the flag created with False
        a.evalTopMost(OthersVar(LocalVar("lf"), "content")) shouldBe FALSEEXPR
        // The access query with FILTER (?v = true) must yield the flag created with True
        a.evalTopMost(OthersVar(LocalVar("lt"), "content")) shouldBe TRUEEXPR
    }

    init {
        "booleanToString" {
            booleanToStringTest()
        }
        "boolean access" {
            booleanAccessTest()
        }
    }
}
