package no.uio.microobject.test.basic

import io.kotest.matchers.shouldBe
import no.uio.microobject.test.MicroObjectTest

class ExtraPrefixTest: MicroObjectTest() {
    fun prefixTest() {
        val (interpreter,_) = initInterpreter("persons", StringLoad.RES)

        interpreter.settings.prefixMap().containsKey("ast") shouldBe false
        interpreter.settings.addPrefixes(hashMapOf("ast" to "http://www.smolang.org/ast#"))
        interpreter.settings.prefixMap().containsKey("ast") shouldBe true
    }
    init {
        prefixTest()
    }
}