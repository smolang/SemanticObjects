package microobject.test.type

import kotlin.test.assertFalse

class FMOLTypeTest : MicroObjectTypeTest()  {
    init{
        "Query simulate success 1"{
            val tC = checkMet("SuccessClass", "start", "test_fmu")
            assert(tC.report(false))
        }
        "Query simulate success 2"{
            val tC = checkMet("SuccessClass", "assign", "test_fmu")
            assert(tC.report(false))
        }
        for( i in 1..5)
        "Query simulate fail $i"{
            val tC = checkMet("FailClass", "fail$i", "test_fmu")
            assertFalse(tC.report(false))
        }
    }
}