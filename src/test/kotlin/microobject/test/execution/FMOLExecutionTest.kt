package microobject.test.execution

import microobject.data.LiteralExpr
import microobject.data.LocalVar
import microobject.test.MicroObjectTest
import microobject.type.DOUBLETYPE
import kotlin.test.assertEquals

class FMOLExecutionTest: MicroObjectTest() {
    init {
        "adder 1" {
            val stmt = """
                Cont[Double ra, Double rb; Double rc] fmu1 := simulate("src/test/resources/adder.fmu", ra := 1.0, rb := 1.0);
                Double res := fmu1.rc;
                breakpoint;
                print(res);
            """.trimIndent()
            val (a,t) = initInterpreter(stmt,StringLoad.STMT)
            assert(t.report())
            executeUntilBreak(a)
            assertEquals(LiteralExpr("2.0", DOUBLETYPE), a.evalTopMost(LocalVar("res", DOUBLETYPE)))
        }
    }
}