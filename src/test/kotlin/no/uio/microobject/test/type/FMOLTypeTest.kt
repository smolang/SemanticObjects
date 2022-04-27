package no.uio.microobject.test.type

import kotlin.test.assertFalse
import io.kotest.core.test.config.enabledOrReasonIf

class FMOLTypeTest : MicroObjectTypeTest()  {
    init{
        "Simulate success 1".config(enabledOrReasonIf = fmuNeedsWindows) {
            val tC = checkMet("SuccessClass", "start", "test_fmu")
            assert(tC.report(false))
        }
        "Simulate success 2".config(enabledOrReasonIf = fmuNeedsWindows) {
            val tC = checkMet("SuccessClass", "assign", "test_fmu")
            assert(tC.report(false))
        }
        "Simulate success 3".config(enabledOrReasonIf = fmuNeedsWindows) {
            val tC = checkMet("SuccessClass", "portTest", "test_fmu")
            assert(tC.report(false))
        }
        for( i in 1..6 )
        "Simulate fail $i".config(enabledOrReasonIf = fmuNeedsWindows) {
            val tC = checkMet("FailClass", "fail$i", "test_fmu")
            assertFalse(tC.report(false))
        }
        for( i in 1..6 )
        "fields $i".config(enabledOrReasonIf = fmuNeedsWindows) {
            val tC = checkMet("SuccessClass", "fieldfail$i", "test_fmu")
            assertFalse(tC.report(false))
        }
        "extra fields".config(enabledOrReasonIf = fmuNeedsWindows) {
            val tC = checkMet("SuccessClass", "extra", "test_fmu")
            assert(tC.report(false))
        }
    }
}
