package microobject.test.type

import kotlin.test.assertEquals
import kotlin.test.assertFalse


open class MOLTypeTest  : MicroObjectTypeTest() {
    init {
        "Assign Test Success"{
            assert(checkMet("Test", "assignSuccess1", "test_assign" ).report(false))
            assert(checkMet("Test", "assignSuccess2", "test_assign" ).report(false))
            assert(checkMet("TestGen", "success", "test_assign" ).report(false))
        }
        for(i in 1..7) {
            "Assign Test Fail $i"{
                val tC = checkMet("Test", "fail$i", "test_assign")
                assertFalse(tC.report(false))
                assertEquals(tC.error.size, 1)
            }
        }
        for(i in 1..4) {
            "Assign Test Gen Fail $i"{
                val tC = checkMet("TestGen", "fail$i", "test_assign")
                assertFalse(tC.report(false))
                assertEquals(tC.error.size, 1)
            }
        }


        "Visible check fail1"{
            val tC = checkMet("A", "m1", "visible")
            assertFalse(tC.report(false))
            assertEquals(tC.error.size, 4)
        }
        "Visible check fail2"{
            val tC = checkMet("B", "m2", "visible")
            assertFalse(tC.report(false))
            assertEquals(tC.error.size, 4)
        }
        "Visible check fail3"{
            val tC = checkMet("C", "m3", "visible")
            assertFalse(tC.report(false))
            assertEquals(tC.error.size, 4)
        }
        "Visible check fail4"{
            val tC = checkMet("D", "m4", "visible")
            assertFalse(tC.report(false))
            assertEquals(tC.error.size, 5)
        }

        "Call Test Success"{
            assert(checkMet("Test", "success", "test_call" ).report(false))
        }
        for(i in 1..7){
            "Call Test Fail $i"{
                val tC = checkMet("Test", "fail$i", "test_call" )
                assertFalse(tC.report(false))
                assertEquals(tC.error.size, 1)
            }
        }



        "Override Test Success"{
            assert(checkClass("Success1", "test_override" ).report(false))
            assert(checkClass("Success2", "test_override" ).report(false))
        }
        "Override Test Fail 1"{
            val tC = checkClass("Fail1", "test_override" )
            assert(tC.report(false)) //it is just a warning
            assertEquals(tC.error.size, 1)
        }
        "Override Test Fail 2"{
            val tC = checkClass("Fail2", "test_override" )
            assertFalse(tC.report(false))
            assertEquals(tC.error.size, 1)
        }
        "Override Test Fail 3"{
            val tC = checkClass("Fail3", "test_override" )
            assert(tC.report(false)) //it is just a warning
            assertEquals(tC.error.size, 1)
        }
        "Override Test Fail 4"{
            val tC = checkClass("Fail4", "test_override" )
            assertFalse(tC.report(false))
            assertEquals(tC.error.size, 2)
        }
        "Override Test Fail 5"{
            val tC = checkClass("Fail5", "test_override" )
            assert(tC.report(false)) //it is just a warning
            assertEquals(tC.error.size, 1)
        }
        "Override Test Fail 6"{
            val tC = checkClass("Fail6", "test_override" )
            assertFalse(tC.report(false))
            assertEquals(tC.error.size, 1)
        }
        "Override Test Fail 7"{
            val tC = checkClass("Fail7", "test_override" )
            assertFalse(tC.report(false))
            assertEquals(tC.error.size, 1)
        }
    }
}