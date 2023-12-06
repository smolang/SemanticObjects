package no.uio.microobject.ast.stmt

import no.uio.microobject.ast.*
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.ast.expr.TRUEEXPR
import no.uio.microobject.runtime.EvalResult
import no.uio.microobject.runtime.Interpreter
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry
import no.uio.microobject.type.*
import org.apache.jena.datatypes.xsd.XSDDatatype

// For ontology-based reflexion
data class ConstructStmt(val target : Location, val query: Expression, val params : List<Expression>, val pos : Int = -1, val declares: Type?) :
    Statement {
    override fun toString(): String = "$target := access($query, ${params.joinToString(",")})"
    override fun getRDF(): String {
        var s = """
            prog:stmt${this.hashCode()} rdf:type smol:ConstructStatement.
            prog:stmt${this.hashCode()} smol:hasTarget prog:loc${target.hashCode()}.
            prog:stmt${this.hashCode()} smol:hasQuery prog:expr${query.hashCode()}.
            prog:stmt${this.hashCode()} smol:Line '$pos'^^xsd:integer.

        """.trimIndent()
        for (i in params.indices){
            s += "prog:stmt${this.hashCode()} smol:hasParameter [smol:hasParameterIndex $i ; smol:hasParameterValue prog:expr${params[i].hashCode()}; ].\n"
            s += params[i].getRDF()
        }
        // return s + target.getRDF()
        return s + target.getRDF() + query.getRDF()
        // '${literal.removePrefix("\"").removeSuffix("\"")}'
    }

    override fun eval(heapObj: Memory, stackFrame: StackEntry, interpreter: Interpreter): EvalResult {
        val str = interpreter.prepareQuery(query, params, stackFrame.store, interpreter.heap, stackFrame.obj)
        val results = interpreter.query(str.removePrefix("\"").removeSuffix("\""))
        var list = LiteralExpr("null")
        val targetType = target.getType()
        if (targetType !is ComposedType || targetType.getPrimary().getNameString() != "List" || targetType.params.first() !is BaseType)
            throw Exception("Could not perform construction from query results: unknown type $targetType")
        val className = (targetType.params.first() as BaseType).name
        if (results != null) {
            for (r in results) {
                val newListName = Names.getObjName("List")
                val newListMemory: Memory = mutableMapOf()

                val m = interpreter.staticInfo.fieldTable[className] ?: throw Exception("This class is unknown: $className")
                val newObjName = Names.getObjName(className)
                val newObjMemory: Memory = mutableMapOf()
                for(f in m) {
                    if (!r.varNames().asSequence().contains(f.name)) { //default values
                        when (f.type) {
                            STRINGTYPE -> newObjMemory[f.name] = LiteralExpr("", f.type)
                            BOOLEANTYPE -> newObjMemory[f.name] = LiteralExpr(TRUEEXPR.literal, f.type)
                            DOUBLETYPE -> newObjMemory[f.name] = LiteralExpr("0.0", f.type)
                            INTTYPE -> newObjMemory[f.name] = LiteralExpr("0", f.type)
                            else -> newObjMemory[f.name] = LiteralExpr("null", f.type)
                        }
                    } else {
                        if(r.get(f.name).isLiteral){
                            val extractedName = r.getLiteral(f.name).toString().removePrefix(interpreter.settings.runPrefix)
                            if (f.type == INTTYPE && r.getLiteral(f.name).asNode().literalDatatype == XSDDatatype.XSDinteger)
                                newObjMemory[f.name] = LiteralExpr(extractedName.split("^^")[0], INTTYPE)
                            else if (f.type == INTTYPE && (extractedName.matches("\\d+".toRegex()) || extractedName.matches("\\d+\\^\\^http://www.w3.org/2001/XMLSchema#integer".toRegex())))
                                newObjMemory[f.name] = LiteralExpr(extractedName.split("^^")[0], INTTYPE)
                            else if (f.type == DOUBLETYPE && r.getLiteral(f.name).asNode().literalDatatype == XSDDatatype.XSDdouble)
                                newObjMemory[f.name] = LiteralExpr(extractedName.split("^^")[0], DOUBLETYPE)
                            else if (f.type == DOUBLETYPE && (extractedName.matches("\\d+".toRegex()) || extractedName.matches("\\d+\\^\\^http://www.w3.org/2001/XMLSchema#double".toRegex())))
                                newObjMemory[f.name] = LiteralExpr(extractedName.split("^^")[0], DOUBLETYPE)
                            else if(f.type == STRINGTYPE)
                                newObjMemory[f.name] = LiteralExpr("\""+extractedName+"\"", f.type)
                            else
                                newObjMemory[f.name] = LiteralExpr(extractedName, f.type)
                        } else {

                            val obres = r.get(f.name)
                            val found = obres.toString().removePrefix(interpreter.settings.runPrefix)
                            val objNameCand = if(found.startsWith("\\\"")) found.replace("\\\"","\"") else found
                            for (ob in interpreter.heap.keys) {
                                if (ob.literal == objNameCand) {
                                    newObjMemory[f.name] = LiteralExpr(objNameCand, ob.tag)
                                    break
                                }
                            }
                        }
                    }
                }
                val rdfName = Names.getNodeName()

                if(interpreter.staticInfo.owldescr[className] != null) {
                    newObjMemory["__describe"] = LiteralExpr(
                        "$rdfName ${interpreter.staticInfo.owldescr[className]!!.removeSurrounding("\"")}",
                        STRINGTYPE
                    )
                    newObjMemory["__models"] = LiteralExpr(rdfName, STRINGTYPE)
                }
                interpreter.heap[newObjName] = newObjMemory
                newListMemory["content"] = newObjName
                newListMemory["next"] = list
                interpreter.heap[newListName] = newListMemory
                list = newListName
            }
        }
        return replaceStmt(AssignStmt(target, list, declares = declares), stackFrame)

    }
}