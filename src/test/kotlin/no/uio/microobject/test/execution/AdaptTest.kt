package no.uio.microobject.test.execution

import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.ast.expr.LocalVar
import no.uio.microobject.test.MicroObjectTest
import no.uio.microobject.type.INTTYPE
import kotlin.test.assertEquals

class AdaptTest : MicroObjectTest()  {

    init {
        "adapt"{

            loadBackground("src/test/resources/selfadapt/greenhouse.ttl","urn:")
            val (a,_) = initInterpreter(listOf("selfadapt/Greenhouse_data","selfadapt/Greenhouse_health","selfadapt/Greenhouse_plants",
                                               "selfadapt/Greenhouse_pots","selfadapt/Greenhouse_pumps","selfadapt/GreenHouse"))
            executeUntilBreak(a)
            assertEquals(1, a.stack.size)
            assertEquals(LiteralExpr("3", INTTYPE), a.evalTopMost(LocalVar("l1", INTTYPE)))
            assertEquals(LiteralExpr("4", INTTYPE), a.evalTopMost(LocalVar("l2", INTTYPE)))
            assertEquals(LiteralExpr("4", INTTYPE), a.evalTopMost(LocalVar("l3", INTTYPE)))
            assertEquals(LiteralExpr("4", INTTYPE), a.evalTopMost(LocalVar("l4", INTTYPE)))
        }
    }
}