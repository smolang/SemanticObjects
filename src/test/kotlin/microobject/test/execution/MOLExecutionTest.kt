package microobject.test.execution

import microobject.data.LiteralExpr
import microobject.data.LocalVar
import microobject.test.MicroObjectTest
import microobject.type.INTTYPE
import kotlin.test.assertEquals

class MOLExecutionTest : MicroObjectTest() {
    init {
        "Add"{
            val v = LocalVar("v", INTTYPE)
            val (a,_) = initInterpreter(" Int v := 1 + 2; v := v + 2; breakpoint;",StringLoad.STMT)
            a.makeStep();
            assertEquals(1, a.stack.size)
            assert(a.stack.peek().store.containsKey("v"))
            assertEquals(LiteralExpr("3", INTTYPE), a.stack.peek().store["v"])
            assertEquals(LiteralExpr("3", INTTYPE), a.evalTopMost(v))
            a.makeStep();
            assertEquals(1, a.stack.size)
            assert(a.stack.peek().store.containsKey("v"))
            assertEquals(LiteralExpr("5", INTTYPE), a.stack.peek().store["v"])
            assertEquals(LiteralExpr("5", INTTYPE), a.evalTopMost(v))
        }
    }
}