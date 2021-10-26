package no.uio.microobject.test.execution

import no.uio.microobject.data.LiteralExpr
import no.uio.microobject.data.LocalVar
import no.uio.microobject.data.TRUEEXPR
import no.uio.microobject.test.MicroObjectTest
import no.uio.microobject.type.BaseType
import no.uio.microobject.type.ERRORTYPE
import no.uio.microobject.type.INTTYPE
import org.apache.jena.query.ResultSetFormatter
import org.apache.jena.rdf.model.impl.LiteralImpl
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class SMOLExecutionTest: MicroObjectTest() {
    init {
        "models override"{
            loadBackground("src/test/resources/models.owl")
            val (a,_) = initInterpreter("models", StringLoad.RES)
            executeUntilBreak(a)
            assertEquals(1, a.stack.size)
            assertEquals(LiteralExpr("2", INTTYPE), a.evalTopMost(LocalVar("l2", INTTYPE)))
            assertEquals(LiteralExpr("1", INTTYPE), a.evalTopMost(LocalVar("l1", INTTYPE)))
        }
        "overload"{
            loadBackground("examples/overload.back")
            val (a, _) = initInterpreter("overload", StringLoad.RES)
            executeUntilBreak(a)
            executeUntilBreak(a)
            assertEquals(1, a.stack.size)
            assertNotEquals(LiteralExpr("null", ERRORTYPE), a.evalTopMost(LocalVar("pre", BaseType("List"))))
            assertEquals(LiteralExpr("null", ERRORTYPE), a.evalTopMost(LocalVar("post", BaseType("List"))))
        }
        "persons"{
            loadBackground("examples/persons.back")
            val (a, _) = initInterpreter("persons", StringLoad.RES)
            executeUntilBreak(a)
            val res = a.query("SELECT ?man WHERE { ?man a domain:Man. }")
            // val res = a.query("SELECT * WHERE { ?a ?b ?c . }")
            assert(res!!.hasNext())
            println("\n" + ResultSetFormatter.asText(res))
        }
        "double"{
            val (a, _) = initInterpreter("double", StringLoad.RES)
            executeUntilBreak(a)
            executeUntilBreak(a)
            assertEquals(1, a.stack.size)
            assertEquals(TRUEEXPR, a.evalTopMost(LocalVar("val", BaseType("List"))))
        }
        "scene"{
            val (a, _) = initInterpreter("scene", StringLoad.RES)
            executeUntilBreak(a)
            assertEquals(0, a.stack.size)
            val res = a.query("SELECT DISTINCT ?obj ?name WHERE { ?sth prog:Rectangle_area_builtin_res ?obj. ?sth prog:Rectangle_name ?name }")
            assertNotNull(res)
            var i = 0
            while(res.hasNext()){
                val r = res.next()
                i++
                assertEquals("10", (r["obj"] as LiteralImpl).string)
                assertEquals("http://www.w3.org/2001/XMLSchema#integer", (r["obj"] as LiteralImpl).getDatatypeURI() )
            }
            assertEquals(1, i)
        }
    }
}
