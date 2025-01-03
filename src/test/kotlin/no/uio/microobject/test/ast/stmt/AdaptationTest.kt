package no.uio.microobject.test.ast.stmt

import io.kotest.matchers.shouldBe
import no.uio.microobject.antlr.WhileLexer
import no.uio.microobject.antlr.WhileParser
import no.uio.microobject.test.MicroObjectTest
import no.uio.microobject.type.BaseType
import no.uio.microobject.type.TypeChecker
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.semanticweb.HermiT.Reasoner
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.reasoner.OWLReasoner
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdaptationTest : MicroObjectTest() {
    private fun checkConsistencyKB() {
        val ttlFilePath = "src/test/resources/reclassification/tree.ttl"
        val ttlFile = File(ttlFilePath)

        val manager = OWLManager.createOWLOntologyManager()

        try {
            val ontology: OWLOntology = manager.loadOntologyFromOntologyDocument(ttlFile)
            val reasonerFactory = Reasoner.ReasonerFactory()
            val reasoner: OWLReasoner = reasonerFactory.createReasoner(ontology)

            assertTrue(reasoner.isConsistent)
            reasoner.dispose()
        } catch (e: Exception) {
            throw AssertionError("Error while checking consistency of the ontology: $e")
        }
    }

    private fun adaptTest() {
        loadBackground("src/test/resources/classification_example.ttl", "http://www.smolang.org/ex#")
        val (interpreter, _) = initInterpreter("reclassification", StringLoad.RES, hashMapOf("ast" to "http://www.smolang.org/ex#"))

        executeUntilBreak(interpreter)
        val keys = interpreter.heap.keys

        // Check if the tag of one of the keys (spefically it will be the run obj) is B
        keys.any { it.tag == BaseType("B") } shouldBe true
    }

    private fun adaptTreeClassifyTest() {
        loadBackground("src/test/resources/reclassification/tree.ttl","http://www.smolang.org/tree#")
        val (interpreter, _) = initInterpreter("reclassification/Tree", StringLoad.RES, hashMapOf("ast" to "http://www.smolang.org/tree#"))

        executeUntilBreak(interpreter)
        val keys = interpreter.heap.keys

        keys.any { it.tag == BaseType("SeedlingTree") } shouldBe true
    }

    private fun adaptTreeRetrieveTest() {
        loadBackground("src/test/resources/reclassification/tree.ttl","http://www.smolang.org/tree#")
        val (interpreter, _) = initInterpreter("reclassification/Tree", StringLoad.RES, hashMapOf("ast" to "http://www.smolang.org/tree#"))

        executeUntilBreak(interpreter)
        val keys = interpreter.heap.keys

        keys.any { it.tag == BaseType("SeedlingTree") && interpreter.heap[it]?.get("oxygen") != null } shouldBe true
    }

    private fun wellFormednessSuccess() {
        loadBackground("src/test/resources/reclassification/tree.ttl","http://www.smolang.org/tree#")
        val (interpreter, _) = initInterpreter("reclassification/Tree", StringLoad.RES, hashMapOf("ast" to "http://www.smolang.org/tree#"))

        val stdLib = this::class.java.classLoader.getResource("StdLib.smol").readText() + "\n\n"
        val lexer = WhileLexer(CharStreams.fromString(stdLib))
        val tokens = CommonTokenStream(lexer)
        val parser = WhileParser(tokens)
        val tree = parser.program()
        val tC = TypeChecker(tree, interpreter.settings, interpreter.tripleManager)

        assertEquals(true, tC.checkAdaptationConsistency(interpreter))
    }

    private fun wellFormednessFail() {
        loadBackground("src/test/resources/reclassification/tree.ttl","http://www.smolang.org/tree#")
        val (interpreter, _) = initInterpreter("reclassification/Tree_union", StringLoad.RES, hashMapOf("ast" to "http://www.smolang.org/tree#"))

        val stdLib = this::class.java.classLoader.getResource("StdLib.smol").readText() + "\n\n"
        val lexer = WhileLexer(CharStreams.fromString(stdLib))
        val tokens = CommonTokenStream(lexer)
        val parser = WhileParser(tokens)
        val tree = parser.program()
        val tC = TypeChecker(tree, interpreter.settings, interpreter.tripleManager)

        assertEquals(false, tC.checkAdaptationConsistency(interpreter))
    }

    init {
        "eval" {
            checkConsistencyKB()
            adaptTest()
            adaptTreeClassifyTest()
            adaptTreeRetrieveTest()
            wellFormednessSuccess()
            wellFormednessFail()
        }
    }
}