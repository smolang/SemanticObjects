package microobject.data

import microobject.gen.WhileBaseVisitor
import microobject.gen.WhileParser.*
import microobject.runtime.FieldEntry
import microobject.runtime.MethodEntry
import microobject.runtime.StackEntry
import microobject.runtime.StaticTable

class Translate : WhileBaseVisitor<ProgramElement>() {

    override fun visitNamelist(ctx: NamelistContext?): ProgramElement {
        return visitChildren(ctx)
    }
    private val table : MutableMap<String, Pair<FieldEntry, Map<String,MethodEntry>>> = mutableMapOf()

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
                     StackEntry(visit(ctx.statement()) as Statement, mutableMapOf(), Names.getObjName("_Entry_")),
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

    override fun visitProgram(ctx: ProgramContext?): ProgramElement {
        return visitChildren(ctx)
    }

    override fun visitClass_def(ctx: Class_defContext?): ProgramElement {
        return visitChildren(ctx)
    }


    override fun visitMethod_def(ctx: Method_defContext?): ProgramElement {
        return visitChildren(ctx)
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
        return LiteralExpr(ctx!!.CONSTANT().text)
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