package no.uio.microobject.test.execution

import no.uio.microobject.data.LiteralExpr
import no.uio.microobject.data.LocalVar
import no.uio.microobject.test.MicroObjectTest
import no.uio.microobject.type.INTTYPE
import kotlin.test.assertEquals

class LMOLExecutionTest : MicroObjectTest()  {

    init {
        "Parse"{
            val (a, _) = initInterpreter("retrieve", StringLoad.RES)
        }
    }
}