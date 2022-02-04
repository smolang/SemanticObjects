package no.uio.microobject.test.triples

import no.uio.microobject.test.MicroObjectTest
import org.apache.jena.query.ResultSet
import org.apache.jena.rdf.model.impl.LiteralImpl
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TripleManagerTest: MicroObjectTest() {
    init {
        // Initialize interpreter
        val (interpreter,_) = initInterpreter("persons", StringLoad.RES)

        // Queries to use
        val qAll = "SELECT (count(*) as ?count) WHERE {?a ?b ?c.}"
        val qType = "SELECT (count(*) as ?count) WHERE {?a rdf:type ?c.}"
        val qDomain = "SELECT (count(*) as ?count) WHERE {?a rdfs:domain ?c.}"
        val qRange = "SELECT (count(*) as ?count) WHERE {?a rdfs:range ?c.}"
        val qDomainAndRange = "SELECT (count(*) as ?count) WHERE {?a rdfs:domain ?c1. ?a rdfs:range ?c2.}"
        val qDatatypeProperty = "SELECT (count(*) as ?count) WHERE {?a rdf:type owl:DatatypeProperty.}"
        val qObjectProperty = "SELECT (count(*) as ?count) WHERE {?a rdf:type owl:ObjectProperty.}"

        // Check that a count SPARQL query returns the correct count
        fun assertTripleCountQuery(query: String, correctCount: Int) {
            val res: ResultSet? = interpreter.query(query)
            assertNotNull(res)
            while (res.hasNext()) {
                val r = res.next()
                assertEquals(correctCount, (r["count"] as LiteralImpl).int)
            }
        }

        // Give a set of cases to this function and it will check that they all hold for the current state.
        // cases is a map from a list of settings to the correct count
        fun assertCasesOnState(cases: Map<List<Any>, Int>) {
            var i = 0
            for ((key, value) in cases) {
                i += 1
                "Triple Manager $i" {
                    interpreter.tripleManager.sources["heap"] = (key[0] == 1)
                    interpreter.tripleManager.sources["staticTable"] = (key[1] as Int == 1)
                    interpreter.tripleManager.sources["vocabularyFile"] = (key[2] as Int == 1)
                    interpreter.tripleManager.sources["externalOntology"] = (key[3] == 1)
                    interpreter.tripleManager.reasoner = key[4].toString()
                    for (b in arrayListOf(true, false)) {  // Make sure that we get the same result when guards are turned off
                        interpreter.tripleManager.guards["heap"] = b
                        interpreter.tripleManager.guards["staticTable"] = b
                        assertTripleCountQuery(key[5].toString(), value)
                    }
                }
            }
        }

        // Order of sources in the list: heap, staticTable, vocabularyFile, externalOntology
        val cases = mapOf<List<Any>, Int>(
            listOf(0,0,0,0,"off", qAll) to 0, // When no sources are included and reasoning is off, then no answers
            listOf(1,0,0,0,"off", qAll) to 3, // Only three triples related to entry object. No stepping yet
            listOf(0,1,0,0,"off", qAll) to 91,
            listOf(0,0,1,0,"off", qAll) to 183, // vocab.owl contains 183 triples.
            listOf(0,0,0,0,"off", qType) to 0, // When no sources are included and reasoning is off, then no answers
            listOf(0,0,1,0,"off", qType) to 63, // vocab.owl contains 183 triples using rdf:type
            listOf(0,0,1,0,"off", qDomain) to 39, // vocab.owl contains 39 triples using rdfs:domain
            listOf(0,0,1,0,"off", qRange) to 39, // vocab.owl contains 39 triples using rdfs:range
            listOf(0,0,1,0,"off", qDomainAndRange) to 39, // vocab.owl contains 39 triples with both domain and range.
            listOf(0,0,1,0,"off", qDatatypeProperty) to 17, // vocab.owl contains 17 datatype properties
            listOf(0,0,1,0,"off", qObjectProperty) to 26, // vocab.owl contains 26 object properties
        )

        assertCasesOnState(cases)
    }
}
