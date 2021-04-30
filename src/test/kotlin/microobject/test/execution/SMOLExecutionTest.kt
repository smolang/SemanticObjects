package microobject.test.execution

import microobject.data.LiteralExpr
import microobject.data.LocalVar
import microobject.data.TRUEEXPR
import microobject.test.MicroObjectTest
import microobject.type.BaseType
import microobject.type.ERRORTYPE
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SMOLExecutionTest: MicroObjectTest() {
    init {
        "overload"{
            loadBackground("examples/overload.back")
            val (a, _) = initInterpreter("overload", StringLoad.RES)
            executeUntilBreak(a)
            executeUntilBreak(a)
            assertEquals(1, a.stack.size)
            assertNotEquals(LiteralExpr("null", ERRORTYPE), a.evalTopMost(LocalVar("pre", BaseType("List"))))
            assertEquals(LiteralExpr("null", ERRORTYPE), a.evalTopMost(LocalVar("post", BaseType("List"))))
        }
        "double"{
            val (a, _) = initInterpreter("double", StringLoad.RES)
            executeUntilBreak(a)
            executeUntilBreak(a)
            assertEquals(1, a.stack.size)
            assertEquals(TRUEEXPR, a.evalTopMost(LocalVar("val", BaseType("List"))))
        }
    }
}