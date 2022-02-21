package no.uio.microobject.test.triples

import no.uio.microobject.test.MicroObjectTest
import org.semanticweb.HermiT.Reasoner
import org.semanticweb.owlapi.reasoner.OWLReasoner
import kotlin.test.assertEquals

class OWLQueryTest: MicroObjectTest() {
    init {
        val (interpreter,_) = initInterpreter("persons", StringLoad.RES)

        val q1 = "<smol:Class>"
        val q2 = "<prog:List>"
        val q3 = "<prog:Person>"
        val q4 = "<prog:Marriage>"
        executeUntilBreak(interpreter)

        "OWL query 1" {
            val s = interpreter.owlQuery(q1)
            assertEquals(s.count(), 5)
        }
        "OWL query 2" {
            val s = interpreter.owlQuery(q2)
            assertEquals(s.count(), 0)
        }
        "OWL query 3" {
            val s = interpreter.owlQuery(q3)
            assertEquals(s.count(), 2) // No steps, so no persons yet
        }
        "OWL query 4" {
            val s = interpreter.owlQuery(q4)
            assertEquals(s.count(), 1) // No steps, so no marriages yet.
        }

        "OWL consistency" {
            val ontology = interpreter!!.tripleManager.getOntology()
            val reasoner : OWLReasoner = Reasoner.ReasonerFactory().createReasoner(ontology)
            assertEquals(reasoner.isConsistent, true)
        }
    }
}