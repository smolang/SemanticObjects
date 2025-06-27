package no.uio.microobject.test.type

import no.uio.microobject.type.Severity

class SMOLTypeNoPunTest : MicroObjectTypeTest() {
    init{
        "Query check success 1"{
            val tC = checkMet("D", "m1", "type_query_new_lift", false)
            assert(tC.report(false))
            assert(tC.queryCheckers.all { it.error.none { it -> it.severity == Severity.ERROR } })
        }
        "Query check success 2"{
            val tC = checkMet("D", "m2", "type_query_new_lift", false)
            assert(tC.report(false))
            assert(tC.queryCheckers.all { it.error.none { it -> it.severity == Severity.ERROR } })
        }
        "Query check fail"{
            val tC = checkMet("D", "m3", "type_query_new_lift", false)
            assert(!tC.report(false))
            assert(!tC.queryCheckers.all { it.error.none { it -> it.severity == Severity.ERROR } })
        }
    }
}
