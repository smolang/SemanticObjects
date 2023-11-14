package no.uio.microobject.test.data

import io.kotest.matchers.shouldBe
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.ast.expr.LocalVar
import no.uio.microobject.main.testModel
import no.uio.microobject.test.MicroObjectTest
import no.uio.microobject.type.DOUBLETYPE
import org.apache.jena.fuseki.main.FusekiServer
import org.apache.jena.query.DatasetFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.ResultSet
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdfconnection.RDFConnectionFactory

class TripleManagerTest: MicroObjectTest() {
    private fun fusekiTest() {
        testModel = ModelFactory.createDefaultModel()
        testModel!!.read("src/test/resources/tree_shapes.ttl")
        val (interpreter,_) = initTripleStoreInterpreter("persons", StringLoad.RES)

        interpreter.tripleManager.getModel().containsAny(testModel!!) shouldBe true
    }

    private fun fusekiServerTest() {

        val model = ModelFactory.createDefaultModel()
        model.read("src/test/resources/tree_shapes.ttl")

        val ds = DatasetFactory.createTxnMem()
        ds.defaultModel.add(model)

        // Execute the Fuseki triplestore with model
        val fusekiServer =  FusekiServer.create().add("/ds", ds).build() ;
        fusekiServer.start() ;

        val (interpreter,_) = initTripleStoreInterpreter("persons", StringLoad.RES)
        fusekiServer.stop()
        interpreter.tripleManager.getModel().containsAny(model) shouldBe true
    }

    init {
        "triple store".config(enabledOrReasonIf = tripleStoreToTest) {
            val model = ModelFactory.createDefaultModel()
            model.read("src/test/resources/tree_shapes.ttl")
            val (interpreter,_) = initTripleStoreInterpreter("persons", StringLoad.RES)
            interpreter.tripleManager.getModel().containsAny(model) shouldBe true

            // The triple store is initialised with this url, but we'll also test that we can get something
            val url = "http://localhost:3030/ds"
            val conn = RDFConnectionFactory.connect(url)

            val query = QueryFactory.create("SELECT * WHERE { ?s ?p ?o } LIMIT 1")
            val qexec = conn.query(query)
            val result: ResultSet = qexec.execSelect()

            result.hasNext() shouldBe true
        }
        "eval" {
            fusekiTest()
            fusekiServerTest()
        }
    }
}