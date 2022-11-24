package no.uio.microobject.test.execution

import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.ast.expr.LocalVar
import no.uio.microobject.ast.expr.TRUEEXPR
import no.uio.microobject.test.MicroObjectTest
import no.uio.microobject.type.BaseType
import no.uio.microobject.type.ERRORTYPE
import no.uio.microobject.type.INTTYPE
import org.apache.jena.rdf.model.impl.LiteralImpl
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class SMOLExecutionTest: MicroObjectTest() {
    init {
        "models override"{
            loadBackground("src/test/resources/models.owl","urn:")
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
            assert(res!!.hasNext())
            val res2 = a.query("SELECT * WHERE { ?person a prog:Person. ?person prog:Person_name ?name. ?person prog:Person_birthYear ?birthYear. ?person prog:Person_height ?height. ?person prog:Person_ownsCar ?ownsCar. FILTER (?name='Bernie') }")
            assertNotNull(res2)
            // Checking that types are correct
            val r = res2.next()
            assertEquals("Bernie", (r["name"] as LiteralImpl).string)
            assertEquals("http://www.w3.org/2001/XMLSchema#string", (r["name"] as LiteralImpl).datatypeURI)
            assertEquals("1952", (r["birthYear"] as LiteralImpl).string)
            assertEquals("http://www.w3.org/2001/XMLSchema#integer", (r["birthYear"] as LiteralImpl).datatypeURI)
            assertEquals("1.83", (r["height"] as LiteralImpl).string)
            assertEquals("http://www.w3.org/2001/XMLSchema#double", (r["height"] as LiteralImpl).datatypeURI)
            assertEquals("false", (r["ownsCar"] as LiteralImpl).string)
            assertEquals("http://www.w3.org/2001/XMLSchema#boolean", (r["ownsCar"] as LiteralImpl).datatypeURI)
            // println("\n" + ResultSetFormatter.asText(res2))
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
                assertEquals("10", (r["obj"] as LiteralImpl).string)
                assertEquals("http://www.w3.org/2001/XMLSchema#integer", (r["obj"] as LiteralImpl).datatypeURI)
                i++
            }
            assertEquals(1, i)
        }
    }
}
