package no.uio.microobject.test.type

import kotlin.test.assertEquals
import kotlin.test.assertFalse


open class MOLTypeTest  : MicroObjectTypeTest() {
    init {
        "Assign Test Success"{
            assert(checkClass("C","types").report(false))
            assert(checkClass("C","poly").report(false))
            assert(checkClass("D","poly").report(false))
            assert(checkMet("Test", "assignSuccess1", "test_assign" ).report(false))
            assert(checkMet("Test", "assignSuccess2", "test_assign" ).report(false))
            assert(checkMet("TestGen", "success", "test_assign" ).report(false))
        }
        "OP Test Success"{
            assert(checkMet("Test", "opsuccess", "test_assign" ).report(false))
        }
        "inner success"{
            assert(checkClass("D", "innerField2" ).report(false))
        }
        "inner fail"{
            assertFalse(checkClass("C", "innerField2" ).report(false))
        }
        "conversion success"{
            assert(checkMet("Conv", "success", "conversion" ).report(false))
        }
        "conversion fail"{
            assertFalse(checkMet("Conv", "fail", "conversion" ).report(false))
        }
        "OP Test Fail"{
            assertFalse(checkMet("Test", "opfail", "test_assign" ).report(false))
        }
        for(i in 1..7) {
            "Assign Test Fail $i"{
                val tC = checkMet("Test", "fail$i", "test_assign")
                println(tC.report(false))
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
            assertEquals(tC.error.size, 2)
        }
        "Override Test Fail 3"{
            val tC = checkClass("Fail3", "test_override" )
            assert(tC.report(false)) //it is just a warning
            assertEquals(tC.error.size, 1)
        }
        "Override Test Fail 4"{
            val tC = checkClass("Fail4", "test_override" )
            assertFalse(tC.report(false))
            assertEquals(tC.error.size, 1)
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

        "Generic inheritance fail 1"{
            val tC = checkClass("D", "test_generic" )
            assertFalse(tC.report(false))
            assertEquals(tC.error.size, 3)
        }
        "Generic inheritance fail 2"{
            val tC = checkClass("E", "test_generic" )
            assertFalse(tC.report(false))
            assertEquals(tC.error.size, 1)
        }
        "Generic inheritance fail 3"{
            val tC = checkClass("F", "test_generic" )
            assertFalse(tC.report(false))
            assertEquals(tC.error.size, 1)
        }
        "Generic inheritance fail 4"{
            val tC = checkClass("H", "test_generic" )
            assertFalse(tC.report(false))
            assertEquals(tC.error.size, 1)
        }
        "Generic inheritance fail 5"{
            val tC = checkClass("I", "test_generic" )
            assertFalse(tC.report(false))
            assertEquals(tC.error.size, 1)
        }
        "Generic inheritance fail 6"{
            val tC = checkClass("B", "test_generic" )
            assertFalse(tC.report(false))
            assertEquals(tC.error.size, 1)
        }
        "Generic fail 7"{
            val tC = checkClass("J", "test_generic" )
            assertFalse(tC.report(false))
            assertEquals(tC.error.size, 1)
        }
        "Generic inheritance succ 1"{
            val tC = checkClass("A", "test_generic" )
            assert(tC.report(false))
        }
        "Generic inheritance succ 2"{
            val tC = checkClass("G", "test_generic" )
            assert(tC.report(false))
        }
        "String concat"{
            assert(checkMet("C", "success", "strings" ).report(false))
            assertFalse(checkMet("C", "fail1", "strings" ).report(false))
            assertFalse(checkMet("C", "fail2", "strings" ).report(false))
            assertFalse(checkMet("C", "fail3", "strings" ).report(false))
            assertFalse(checkMet("C", "fail4", "strings" ).report(false))
        }
    }
}
