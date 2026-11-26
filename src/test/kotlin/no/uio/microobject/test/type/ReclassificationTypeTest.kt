package no.uio.microobject.test.type

import io.kotest.matchers.shouldBe
import no.uio.microobject.test.MicroObjectTest
import no.uio.microobject.type.Severity

/**
 * Type-check regression tests for the declarative reclassification port.
 *
 * Unlike the execution tests, these run the full `TypeChecker.check()` (not just `collect()`), so the
 * classify/adapt checks, the state-instantiation checks and the adaptation-method checks are all exercised.
 */
class ReclassificationTypeTest : MicroObjectTest() {

    // A is the root of the adaptation hierarchy, C is the state marked for adaptation,
    // B is a plain subclass that may be instantiated normally.
    private val adaptationProgram = """
        class A()
        end

        class B extends A()
        end

        class C extends A()
        classifies "<smol:links> some <ast:A>";
        end

        class D()
        end

        main
            A a = new B();
            classify(a);
            adapt(a);
        end
    """.trimIndent()

    // Same hierarchy, but classify targets a class that is not part of any adaptation query.
    private val unrelatedProgram = """
        class A()
        end

        class B extends A()
        end

        class C extends A()
        classifies "<smol:links> some <ast:A>";
        end

        class D()
        end

        main
            D d = new D();
            D x = classify(d);
        end
    """.trimIndent()

    // classify/adapt with a statically root-typed argument must not be reported:
    // the root class is a valid match (the runtime object may be one of the states).
    private fun rootTypedClassifyAndAdaptAccepted() {
        val (tC, _) = initTc(adaptationProgram, StringLoad.PRG)
        tC.check()

        // Readable failure message: list the actual errors
        tC.error.filter { it.severity == Severity.ERROR } shouldBe emptyList()
        tC.report(true) shouldBe true
    }

    // classify on a class that belongs to no adaptation query must be an ERROR.
    private fun unrelatedClassClassifyRejected() {
        val (tC, _) = initTc(unrelatedProgram, StringLoad.PRG)
        tC.check()

        tC.report(true) shouldBe false
        tC.error.any { it.severity == Severity.ERROR && it.msg.contains("not in any adaptation query") } shouldBe true
    }

    // The reference's own example does `A a = new B();` where B carries a `classifies` block:
    // that must not be reported as an ERROR (the state-instantiation diagnostics are warnings).
    private fun referenceExampleTypeChecks() {
        loadBackground("src/test/resources/classification_example.ttl", "http://www.smolang.org/ex#")
        val (_, tC) = initInterpreter("classification", StringLoad.RES, hashMapOf("ast" to "http://www.smolang.org/ex#"))

        tC.check()

        // Readable failure message: list the actual errors (warnings are allowed)
        tC.error.filter { it.severity == Severity.ERROR } shouldBe emptyList()
        tC.report(true) shouldBe true

        // The diagnostic for `new B()` must still be produced, but as a WARNING
        tC.error.any {
            it.severity == Severity.WARNING && it.msg.contains("because it is a state marked for adaptation")
        } shouldBe true
    }

    init {
        "root-typed classify and adapt are accepted" {
            rootTypedClassifyAndAdaptAccepted()
        }
        "classify on an unrelated class is an error" {
            unrelatedClassClassifyRejected()
        }
        "the reference classification example type-checks without errors" {
            referenceExampleTypeChecks()
        }
    }
}
