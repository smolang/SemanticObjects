package no.uio.microobject.test.type

import no.uio.microobject.type.Severity
import no.uio.microobject.type.TypeChecker
import kotlin.test.assertFalse

class SMOLTypeTest : MicroObjectTypeTest() {
    init{
        for(i in 1..5)
            "Query check success $i"{
                val tC = checkMet("Test", "mSuccess$i", "type_query") as TypeChecker
                assert(tC.report(false))
                assert(tC.queryCheckers.all { it.error.none { it.severity == Severity.ERROR } })
            }

        "Query check fail 1"{
            val tC = checkMet("Test", "mFail1", "type_query")
            assertFalse(tC.report(false))
        }

        "Query check fail 2"{
            val tC = checkMet("Test", "mFail2", "type_query")
            assertFalse(tC.report(false))
        }

        "Query check fail 3"{
            val tC = checkMet("Test", "mFail3", "type_query")
            assert(tC.report(false)) //%parameters are a warning now
            assert(!tC.queryCheckers.all { it.error.isEmpty() })
        }
        "Query rule success"{
            val tC = checkMet("F", "getI", "type_query")
            assert(tC.report(false))
        }

        "Query rule fail"{
            val tC = checkMet("F", "errorGet", "type_query")
            assertFalse(tC.report(false))
        }
    }
}
