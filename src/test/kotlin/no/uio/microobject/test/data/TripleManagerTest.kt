package no.uio.microobject.test.data

import io.kotest.matchers.shouldBe
import no.uio.microobject.main.testModel
import no.uio.microobject.test.MicroObjectTest
import org.apache.jena.rdf.model.ModelFactory

class TripleManagerTest: MicroObjectTest() {
    private fun fusekiTest() {
        testModel = ModelFactory.createDefaultModel()
        testModel!!.read("src/test/resources/tree_shapes.ttl")
        val (interpreter,_) = initTripleStoreInterpreter("persons", StringLoad.RES)

        interpreter.tripleManager.getModel().containsAny(testModel!!) shouldBe true
    }

    init {
        "eval" {
            fusekiTest()
        }
    }
}