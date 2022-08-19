package no.uio.microobject.test

import no.uio.microobject.test.type.MicroObjectTypeTest
import kotlin.test.assertFalse

class RegressionTests : MicroObjectTypeTest() {
    init{
        "Issue10" {
            assertFalse(checkMet("Hello", "say_hello", "bug_10").report(false))
        }
    }
}