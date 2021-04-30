package microobject.test.type

import kotlin.test.assertFalse

class FMOLTypeTest : MicroObjectTypeTest()  {
    init{
        "Simulate success 1"{
            val tC = checkMet("SuccessClass", "start", "test_fmu")
            assert(tC.report(false))
        }
        "Simulate success 2"{
            val tC = checkMet("SuccessClass", "assign", "test_fmu")
            assert(tC.report(false))
        }
        for( i in 1..5)
        "Simulate fail $i"{
            val tC = checkMet("FailClass", "fail$i", "test_fmu")
            assertFalse(tC.report(false))
        }
        for( i in 1..5)
        "fields $i"{
            val tC = checkMet("SuccessClass", "fieldfail$i", "test_fmu")
            assertFalse(tC.report(false))
        }
        "extra fields"{
            val tC = checkMet("SuccessClass", "extra", "test_fmu")
            assert(tC.report(false))
        }
    }
}