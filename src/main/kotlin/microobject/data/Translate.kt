package microobject.data

import antlr.microobject.gen.WhileBaseVisitor
import antlr.microobject.gen.WhileParser.*
import microobject.runtime.*
import org.apache.jena.graph.Node
import org.apache.jena.graph.NodeFactory
import org.apache.jena.graph.Triple
import org.apache.jena.reasoner.rulesys.BuiltinRegistry
import org.apache.jena.reasoner.rulesys.RuleContext
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin
import java.util.*


class Translate : WhileBaseVisitor<ProgramElement>() {

    private val table : MutableMap<String, Pair<FieldEntry, Map<String,MethodEntry>>> = mutableMapOf()
    var i = 0

    fun generateBuiltins(ctx: ProgramContext?, staticTable: StaticTable, back: String, interpreterBridge: InterpreterBridge) : String{
        var num = 0
        var retString = "["
        for(cl in ctx!!.class_def()){
            for(nm in cl.method_def()) {
                if(nm.builtinrule != null){
                    println("Generating builtin functor and rule for ${nm.NAME()}...")

                    val builtin = object : BaseBuiltin() {
                        override fun getName(): String {
                            return "${cl.NAME(0)}_${nm.NAME()}_builtin"
                        }

                        override fun getArgLength(): Int {
                            return 1
                        }

                        override fun headAction(args: Array<out Node>?, length: Int, context: RuleContext?) {
                            val thisVar = getArg(0, args, context)
                            val ipr = interpreterBridge.interpreter
                                ?: throw Exception("Builtin functor cannot be expanded if the interpreter is unknown.")

                            val myIpr = ipr.coreCopy()

                            val classStmt =
                                myIpr.staticInfo.methodTable[cl.NAME(0).text
                                    ?: throw Exception("Error during builtin generation")]
                                    ?: throw Exception("Error during builtin generation")
                            val met = classStmt[nm.NAME().text] ?: throw Exception("Error during builtin generation")
                            val mem: Memory = mutableMapOf()
                            val obj = LiteralExpr(
                                thisVar.toString().removePrefix("urn:"),
                                cl.NAME(0).text
                            )
                            mem["this"] = obj
                            val se = StackEntry(met.first, mem, obj, Names.getStackId())
                            myIpr.stack.push(se)

                            while (true) {
                                if (myIpr.stack.peek().active is ReturnStmt) {
                                    val resStmt = myIpr.stack.peek().active as ReturnStmt
                                    val res = resStmt.value
                                    val ret = myIpr.evalTopMost(res).literal
                                    val str = if (ret.toIntOrNull() == null) ret else "urn:$ret"
                                    val resNode = NodeFactory.createURI(str)
                                    val connectInNode = NodeFactory.createURI("urn:${name}_res")
                                    val triple = Triple.create(thisVar, connectInNode, resNode)
                                    context!!.add(triple)
                                    break
                                }
                                myIpr.makeStep()
                            }
                        }
                    }
                    BuiltinRegistry.theRegistry.register(builtin)
                    var ruleString = "rule${num++}:"
                    val headString = "${builtin.name}(?this)"
                    val thisString = "(?this urn:MOinstanceOf urn:${cl.NAME(0)})"
                    ruleString = " $ruleString $thisString -> $headString "
                    retString += ruleString
                }
            }
        }
        val str = if(retString != "[") "$retString]" else ""
        println("rules: $str")
        return str
    }

    fun generateStatic(ctx: ProgramContext?) : Pair<StackEntry,StaticTable> {
        val roots : MutableSet<String> = mutableSetOf()
        val hierarchy : MutableMap<String, MutableSet<String>> = mutableMapOf()
        for(cl in ctx!!.class_def()){
            if(cl.NAME(1) != null){
                var maps = hierarchy[cl.NAME(1).text]
                if(maps == null) maps = mutableSetOf()
                maps.add(cl!!.NAME(0).text)
                hierarchy[cl.NAME(1).text] = maps
            } else {
                roots += cl!!.NAME(0).text
            }
            val fields = if(cl.namelist() != null) nameListTranslate(cl.namelist()) else listOf()

            val res = mutableMapOf<String, MethodEntry>()
            for(nm in cl.method_def()){ //Pair<Statement, List<String>>
                val stmt = visit(nm!!.statement()) as Statement
                val params = if(nm.namelist() != null) nameListTranslate(nm.namelist()) else listOf()
                res[nm.NAME().text] = Pair(stmt, params)
            }
            table[cl.NAME(0).text] = Pair(fields, res)
        }

        while(roots.isNotEmpty()){
            val r = roots.first()
            roots.remove(r)
            val below = hierarchy[r] ?: continue
            for( str in below ){
                roots.add(str)
                val entryUp = table[r]
                val entryLow = table[str]
                table[str] = Pair(entryUp!!.first + entryLow!!.first, entryUp.second + entryLow.second)
            }
        }

        //refactor this first
        var fieldTable : Map<String,FieldEntry> = emptyMap()
        var methodTable : Map<String,Map<String,MethodEntry>> = emptyMap()
        for(entry in table){
            fieldTable = fieldTable + Pair(entry.key, entry.value.first)
            methodTable += Pair(entry.key, entry.value.second)
        }

        return Pair(
                     StackEntry(visit(ctx.statement()) as Statement, mutableMapOf(), Names.getObjName("_Entry_"), Names.getStackId()),
                     StaticTable(fieldTable, methodTable, hierarchy)
                   )
    }

    private fun nameListTranslate(ctx: NamelistContext?) : List<String> {
        var res = listOf<String>()
        if(ctx!!.NAME() != null) {
            for (nm in ctx.NAME())
                res += nm.text
        }
        return res
    }


    override fun visitCall_statement(ctx: Call_statementContext?): ProgramElement {
        var ll = emptyList<Expression>()

        if(ctx!!.target == null){
            for(i in 1 until ctx.expression().size)
                ll += visit(ctx.expression(i)) as Expression
            return CallStmt(Names.getVarName(),
                visit(ctx.expression(0)) as Location,
                ctx.NAME().text,
                ll)
        } else {
            for(i in 2 until ctx.expression().size)
                ll += visit(ctx.expression(i)) as Expression
            return CallStmt(
                visit(ctx.target) as Location,
                visit(ctx.expression(1)) as Location,
                ctx.NAME().text,
                ll
            )
        }
    }

    override fun visitCreate_statement(ctx: Create_statementContext?): ProgramElement {
        var ll = emptyList<Expression>()
        for(i in 1 until ctx!!.expression().size)
            ll += visit(ctx.expression(i)) as Expression
        return CreateStmt(visit(ctx.target) as Location,
                          ctx.NAME().text,
                          ll)
    }

    override fun visitSparql_statement(ctx: Sparql_statementContext?): ProgramElement {
        val target = visit(ctx!!.target) as Location
        val query = visit(ctx!!.query) as Expression
        var ll = emptyList<Expression>()
        for(i in 2 until ctx!!.expression().size)
            ll += visit(ctx.expression(i)) as Expression
        return SparqlStmt(target, query, ll)
    }

    override fun visitOwl_statement(ctx: Owl_statementContext?): ProgramElement {
        val target = visit(ctx!!.target) as Location
        val query = visit(ctx!!.query) as Expression
        return OwlStmt(target, query)

    }

    override fun visitOutput_statement(ctx: Output_statementContext?): ProgramElement {
        return PrintStmt(visit(ctx!!.expression()) as Expression)
    }

    override fun visitAssign_statement(ctx: Assign_statementContext?): ProgramElement {
        return AssignStmt(visit(ctx!!.expression(0)) as Location, visit(ctx.expression(1)) as Expression)
    }

    override fun visitSkip_statment(ctx: Skip_statmentContext?): ProgramElement {
        return SkipStmt
    }

    override fun visitDebug_statement(ctx: Debug_statementContext?): ProgramElement {
        return DebugStmt
    }

    override fun visitReturn_statement(ctx: Return_statementContext?): ProgramElement {
        return ReturnStmt(visit(ctx!!.expression()) as Expression)
    }

    override fun visitIf_statement(ctx: If_statementContext?): ProgramElement {
        val stm1 = visit(ctx!!.statement(0)) as Statement
        val stm2 = if(ctx.statement(1) != null) visit(ctx.statement(1)) as Statement else SkipStmt
        val stmNext =  if(ctx.next != null) visit(ctx.next) as Statement else SkipStmt
        val guard = visit(ctx.expression()) as Expression
        return appendStmt(IfStmt(guard, stm1, stm2), stmNext)
    }

    override fun visitWhile_statement(ctx: While_statementContext?): ProgramElement {
        val stm1 = visit(ctx!!.statement(0)) as Statement
        val stmNext =  if(ctx.next != null) visit(ctx.next) as Statement else SkipStmt
        val guard = visit(ctx.expression()) as Expression
        return appendStmt(WhileStmt(guard, stm1), stmNext)
    }

    override fun visitSequence_statement(ctx: Sequence_statementContext?): ProgramElement {
        val stm1 = visit(ctx!!.statement(0)) as Statement
        val stm2 = visit(ctx.statement(1)) as Statement
        return appendStmt(stm1, stm2)
    }

    override fun visitPlus_expression(ctx: Plus_expressionContext?): ProgramElement {
        return ArithExpr(Operator.PLUS, listOf(visit(ctx!!.expression(0)) as Expression, visit(ctx.expression(1)) as Expression))
    }

    override fun visitMinus_expression(ctx: Minus_expressionContext?): ProgramElement {
        return ArithExpr(Operator.PLUS, listOf(visit(ctx!!.expression(0)) as Expression, visit(ctx.expression(1)) as Expression))
    }

    override fun visitGeq_expression(ctx: Geq_expressionContext?): ProgramElement {
        return ArithExpr(Operator.GEQ, listOf(visit(ctx!!.expression(0)) as Expression, visit(ctx.expression(1)) as Expression))
    }

    override fun visitEq_expression(ctx: Eq_expressionContext?): ProgramElement {
        return ArithExpr(Operator.EQ, listOf(visit(ctx!!.expression(0)) as Expression, visit(ctx.expression(1)) as Expression))
    }
    override fun visitNeq_expression(ctx: Neq_expressionContext?): ProgramElement {
        return ArithExpr(Operator.NEQ, listOf(visit(ctx!!.expression(0)) as Expression, visit(ctx.expression(1)) as Expression))
    }

    override fun visitConst_expression(ctx: Const_expressionContext?): ProgramElement {
        val inner = ctx!!.CONSTANT()!!.text
        return if(inner.toIntOrNull() != null) LiteralExpr(inner, "integer") else LiteralExpr(inner, "boolean")
    }

    override fun visitString_expression(ctx: String_expressionContext?): ProgramElement {
        val inner = ctx!!.STRING().text
        return LiteralExpr(inner, "string")
    }

    override fun visitNested_expression(ctx: Nested_expressionContext?): ProgramElement {
        return visit(ctx!!.expression())
    }

    override fun visitVar_expression(ctx: Var_expressionContext?): ProgramElement {
        if(ctx!!.NAME().text == "null") return LiteralExpr("null")
        return LocalVar(ctx.NAME().text)
    }

    override fun visitField_expression(ctx: Field_expressionContext?): ProgramElement {
        return OwnVar(ctx!!.NAME().text)
    }

    override fun visitExternal_field_expression(ctx: External_field_expressionContext?): ProgramElement {
        return OthersVar(visit(ctx!!.expression()) as Expression, ctx.NAME().text)
    }
    override fun visitThis_expression(ctx: This_expressionContext?): ProgramElement {
        return LocalVar("this")
    }
}