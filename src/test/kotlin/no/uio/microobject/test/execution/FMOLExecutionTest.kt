package no.uio.microobject.test.execution

import no.uio.microobject.data.LiteralExpr
import no.uio.microobject.data.LocalVar
import no.uio.microobject.test.MicroObjectTest
import no.uio.microobject.type.DOUBLETYPE
import no.uio.microobject.type.INTTYPE
import kotlin.test.assertEquals

class FMOLExecutionTest: MicroObjectTest() {
    init {
        "adder 1" {
            val stmt = """
                Cont[In Double ra, In Double rb, Out Double rc] fmu1 := simulate("src/test/resources/adder.fmu", ra := 1.0, rb := 1.0);
                Double res := fmu1.rc;
                breakpoint;
                print(res);
            """.trimIndent()
            val (a,t) = initInterpreter(stmt,StringLoad.STMT)
            assert(t.report())
            executeUntilBreak(a)
            assertEquals(LiteralExpr("2.0", DOUBLETYPE), a.evalTopMost(LocalVar("res", DOUBLETYPE)))
        }
        "linear 1" {
            val stmt = """
                Cont[Out Int outPort] fmu1 := simulate("src/test/resources/linear.fmu", inPort := 1);
                fmu1.tick(1.0);
                Int res := fmu1.outPort;
                breakpoint;
                print(outPort);
            """.trimIndent()
            val (a,t) = initInterpreter(stmt,StringLoad.STMT)
            assert(t.report())
            executeUntilBreak(a)
            assertEquals(LiteralExpr("1", INTTYPE), a.evalTopMost(LocalVar("res", DOUBLETYPE)))
        }
    }
}
