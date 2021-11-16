package no.uio.microobject.test.execution

import no.uio.microobject.test.MicroObjectTest
import java.lang.Exception
import kotlin.test.assertFailsWith

class LMOLExecutionTest : MicroObjectTest()  {

    init {
        "Parse"{
            val (a, _) = initInterpreter("retrieve", StringLoad.RES)
            assertFailsWith(Exception::class,"Implement me :)") {
                executeUntilBreak(a)
            }
        }
    }
}