package no.uio.microobject.ast.expr

import no.uio.microobject.ast.Expression
import no.uio.microobject.ast.ProgramElement
import no.uio.microobject.runtime.GlobalMemory
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.SimulationMemory
import no.uio.microobject.type.BOOLEANTYPE
import no.uio.microobject.type.DOUBLETYPE
import no.uio.microobject.type.INTTYPE
import no.uio.microobject.type.STRINGTYPE
import kotlin.math.roundToInt

data class ConversionExpr(val c : Conversion, val inner : Expression) : Expression {
    override fun eval(stack: Memory, heap: GlobalMemory, simMemory: SimulationMemory, obj: LiteralExpr): LiteralExpr {
        val i = inner.eval(stack, heap, simMemory, obj);
        when (c){
            Conversion.DOUBLETOINT ->
                if (i.tag == DOUBLETYPE){
                    return LiteralExpr(i.literal.toDouble().roundToInt().toString(), INTTYPE)
                } else {
                    throw Exception("Conversion.DOUBLETOINT requires a Double value as its parameter")
                }
            Conversion.DOUBLETOSTRING ->
                if (i.tag == DOUBLETYPE){
                    return LiteralExpr(i.literal, STRINGTYPE)
                } else {
                    throw Exception("Conversion.DOUBLETOSTRING requires a Double value as its parameter")
                }
            Conversion.INTTOSTRING ->
                if (i.tag == INTTYPE){
                    return LiteralExpr(i.literal, STRINGTYPE)
                } else {
                    throw Exception("Conversion.INTTOSTRING requires an Int value as its parameter")
                }
            Conversion.INTTODOUBLE ->
                if (i.tag == INTTYPE){
                    return LiteralExpr(i.literal.toInt().toDouble().toString(), DOUBLETYPE)
                } else {
                    throw Exception("Conversion.INTTODOUBLE requires an Int value as its parameter")
                }
            Conversion.BOOLEANTOSTRING ->
                if (i.tag == BOOLEANTYPE){
                    return LiteralExpr(i.literal, STRINGTYPE)
                } else {
                    throw Exception("Conversion.BOOLEANTOSTRING requires a Boolean value as its parameter")
                }
        }
    }

    override fun getRDF(): String {
        return inner.getRDF()  //TODO: once the AST lifting is being fixed, this must be included
    }

}

enum class Conversion : ProgramElement{
    INTTOSTRING {
        override fun getRDF(): String {
            TODO("Not yet implemented")
        }
    },
    INTTODOUBLE{
        override fun getRDF(): String {
            TODO("Not yet implemented")
        }
    },
    DOUBLETOINT{
        override fun getRDF(): String {
            TODO("Not yet implemented")
        }
    },
    DOUBLETOSTRING{
        override fun getRDF(): String {
            TODO("Not yet implemented")
        }
    },
    BOOLEANTOSTRING{
        override fun getRDF(): String {
            TODO("Not yet implemented")
        }
    },
}