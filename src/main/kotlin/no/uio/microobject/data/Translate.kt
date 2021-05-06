@file:Suppress("UNNECESSARY_NOT_NULL_ASSERTION")

package no.uio.microobject.data

import no.uio.microobject.antlr.WhileBaseVisitor
import no.uio.microobject.antlr.WhileParser.*
import no.uio.microobject.runtime.*
import no.uio.microobject.type.*
import org.antlr.v4.runtime.RuleContext

/**
 * This class handles multiple tasks related to translating ANTLR structures to the internal representation
 *  - It translate statements and expression
 *  - It generates the class table
 */
class Translate : WhileBaseVisitor<ProgramElement>() {

    private val table : MutableMap<String, Pair<FieldEntry, Map<String,MethodEntry>>> = mutableMapOf()

    fun generateStatic(ctx: ProgramContext?) : Pair<StackEntry,StaticTable> {
        val roots : MutableSet<String> = mutableSetOf()
        val hierarchy : MutableMap<String, MutableSet<String>> = mutableMapOf()
        for(cl in ctx!!.class_def()){
            if(cl.superType != null){
                val superType =
                    TypeChecker.translateType(cl.superType, cl!!.className.text, mutableMapOf())
                var maps = hierarchy[superType.getPrimary().getNameString()]
                if(maps == null) maps = mutableSetOf()
                maps.add(cl!!.className.text)
                hierarchy[superType.getPrimary().getNameString()] = maps
            } else {
                roots += cl!!.className.text
            }
            val fields = if(cl.fieldDeclList() != null) {
                var res = listOf<FieldInfo>()
                if(cl.fieldDeclList().fieldDecl() != null) {
                    for (nm in cl.fieldDeclList().fieldDecl()) {
                        val cVisibility = if(nm.visibility == null) Visibility.PUBLIC else if(nm.visibility.PROTECTED() != null) Visibility.PROTECTED else Visibility.PRIVATE
                        val iVisibility = if(nm.infer == null) Visibility.PUBLIC else if(nm.infer.INFERPROTECTED() != null) Visibility.PROTECTED else Visibility.PRIVATE
                        res = res + FieldInfo(
                            nm.NAME().text,
                            TypeChecker.translateType(nm.type(), cl.className.text, mutableMapOf()),
                            cVisibility,
                            iVisibility,
                            BaseType(cl!!.className.text)
                        )
                    }
                }
                res
            } else {
                listOf()
            }

            val res = mutableMapOf<String, MethodEntry>()
            for(nm in cl.method_def()){ //Pair<Statement, List<String>>
                if(nm.abs == null && nm.statement() == null)
                    throw Exception("Non-abstract method with empty statement: ${nm.NAME().text}")
                if(nm.abs != null && nm.statement() != null )
                    throw Exception("Abstract method with non-empty statement: ${nm.NAME().text}")
                if(nm.abs == null) {
                    val stmt = visit(nm!!.statement()) as Statement
                    val params = if (nm.paramList() != null) paramListTranslate(nm.paramList()) else listOf()
                    res[nm.NAME().text] = Pair(stmt, params)
                }
                if(nm.abs != null) {
                    val params = if (nm.paramList() != null) paramListTranslate(nm.paramList()) else listOf()
                    res[nm.NAME().text] = Pair(SkipStmt(ctx!!.start.line), params)
                }
            }
            table[cl.className.text] = Pair(fields, res)
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

    private fun paramListTranslate(ctx: ParamListContext?) : List<String> {
        var res = listOf<String>()
        if(ctx!!.param() != null) {
            for (nm in ctx.param())
                res += nm.NAME().text
        }
        return res
    }

    override fun visitSuper_statement(ctx: Super_statementContext?): ProgramElement {
        var ll = emptyList<Expression>()
        var met : RuleContext? = ctx
        while(met != null && met !is Method_defContext){
            met = met.parent
        }
        val method = (met as Method_defContext).NAME().text

        if(ctx!!.target == null){
            for(i in 0 until ctx.expression().size)
                ll += visit(ctx.expression(i)) as Expression

            return SuperStmt(Names.getVarName(),
                method,
                ll,
                ctx!!.start.line
            )
        } else {
            for(i in 0 until ctx.expression().size)
                ll += visit(ctx.expression(i)) as Expression
            return SuperStmt(
                visit(ctx.target) as Location,
                method,
                ll,
                ctx!!.start.line
            )
        }
    }

    override fun visitCall_statement(ctx: Call_statementContext?): ProgramElement {
        var ll = emptyList<Expression>()

        if(ctx!!.target == null){
            for(i in 1 until ctx.expression().size)
                ll += visit(ctx.expression(i)) as Expression
            return CallStmt(Names.getVarName(),
                visit(ctx.expression(0)) as Location,
                ctx.NAME().text,
                ll,
                ctx!!.start.line
            )
        } else {
            for(i in 2 until ctx.expression().size)
                ll += visit(ctx.expression(i)) as Expression
            return CallStmt(
                visit(ctx.target) as Location,
                visit(ctx.expression(1)) as Location,
                ctx.NAME().text,
                ll,
                ctx!!.start.line
            )
        }
    }

    override fun visitCreate_statement(ctx: Create_statementContext?): ProgramElement {
        var ll = emptyList<Expression>()
        for(i in 1 until ctx!!.expression().size)
            ll += visit(ctx.expression(i)) as Expression
        val def = getClassDecl(ctx)
        val targetType =
            TypeChecker.translateType(ctx.newType, if(def != null) def!!.className.text else ERRORTYPE.name, mutableMapOf())
        return CreateStmt(visit(ctx.target) as Location,
                          targetType.getPrimary().getNameString(),
                          ll,
                          ctx!!.start.line
                         )
    }

    override fun visitSparql_statement(ctx: Sparql_statementContext?): ProgramElement {
        val target = visit(ctx!!.target) as Location
        if(ctx.declType != null) {
            val cDecl = getClassDecl(ctx)
            val className = if(cDecl == null) ERRORTYPE.name else cDecl.className.text
            val targetType =
                TypeChecker.translateType(ctx.declType, className, mutableMapOf())
            target.setType(targetType)
        }
        val query = visit(ctx!!.query) as Expression
        var ll = emptyList<Expression>()
        for(i in 2 until ctx!!.expression().size)
            ll += visit(ctx.expression(i)) as Expression
        return SparqlStmt(target, query, ll, ctx!!.start.line)
    }

    override fun visitConstruct_statement(ctx: Construct_statementContext?): ProgramElement {
        val target = visit(ctx!!.target) as Location
        if(ctx.declType != null) {
            val decl = getClassDecl(ctx)
            val className = if(decl == null) ERRORTYPE.name else decl!!.className.text
            val targetType =
                TypeChecker.translateType(ctx.declType, className, mutableMapOf())
            target.setType(targetType)
        }
        val query = visit(ctx!!.query) as Expression
        var ll = emptyList<Expression>()
        for(i in 2 until ctx!!.expression().size)
            ll += visit(ctx.expression(i)) as Expression
        return ConstructStmt(target, query, ll, ctx!!.start.line)
    }

    override fun visitOwl_statement(ctx: Owl_statementContext?): ProgramElement {
        val target = visit(ctx!!.target) as Location
        val query = visit(ctx!!.query) as Expression
        return OwlStmt(target, query, ctx!!.start.line)
    }

    override fun visitValidate_statement(ctx: Validate_statementContext?): ProgramElement {
        val target = visit(ctx!!.target) as Location
        val query = visit(ctx!!.query) as Expression
        return ValidateStmt(target, query, ctx!!.start.line)
    }

    override fun visitSimulate_statement(ctx: Simulate_statementContext?): ProgramElement {
        val path = ctx!!.path.text.removeSurrounding("\"")
        val target = visit(ctx!!.target) as Location

        var res = listOf<VarInit>()
        if(ctx!!.varInitList() != null) {
            for (nm in ctx!!.varInitList()!!.varInit())
                res += visit(nm) as VarInit
        }
        return SimulationStmt(target, path,  res)
    }

    override fun visitTick_statement(ctx: Tick_statementContext?): ProgramElement {
        return TickStmt(visit(ctx!!.fmu) as Expression, visit(ctx!!.time) as Expression )
    }

    override fun visitOutput_statement(ctx: Output_statementContext?): ProgramElement {
        return PrintStmt(visit(ctx!!.expression()) as Expression, ctx!!.start.line)
    }

    override fun visitDestroy_statement(ctx: Destroy_statementContext?): ProgramElement {
        return DestroyStmt(visit(ctx!!.expression()) as Expression, ctx!!.start.line)
    }

    override fun visitAssign_statement(ctx: Assign_statementContext?): ProgramElement {
        return AssignStmt(visit(ctx!!.expression(0)) as Location, visit(ctx.expression(1)) as Expression, ctx!!.start.line)
    }

    override fun visitSkip_statment(ctx: Skip_statmentContext?): ProgramElement {
        return SkipStmt(ctx!!.start.line)
    }

    override fun visitDebug_statement(ctx: Debug_statementContext?): ProgramElement {
        return DebugStmt(ctx!!.start.line)
    }

    override fun visitReturn_statement(ctx: Return_statementContext?): ProgramElement {
        return ReturnStmt(visit(ctx!!.expression()) as Expression, ctx!!.start.line)
    }

    override fun visitIf_statement(ctx: If_statementContext?): ProgramElement {
        val stm1 = visit(ctx!!.statement(0)) as Statement
        val stm2 = if(ctx.statement(1) != null) visit(ctx.statement(1)) as Statement else SkipStmt()
        val stmNext =  if(ctx.next != null) visit(ctx.next) as Statement else SkipStmt()
        val guard = visit(ctx.expression()) as Expression
        return appendStmt(IfStmt(guard, stm1, stm2, ctx!!.start.line), stmNext)
    }

    override fun visitWhile_statement(ctx: While_statementContext?): ProgramElement {
        val stm1 = visit(ctx!!.statement(0)) as Statement
        val stmNext =  if(ctx.next != null) visit(ctx.next) as Statement else SkipStmt()
        val guard = visit(ctx.expression()) as Expression
        return appendStmt(WhileStmt(guard, stm1, ctx!!.start.line), stmNext)
    }

    override fun visitSequence_statement(ctx: Sequence_statementContext?): ProgramElement {
        val stm1 = visit(ctx!!.statement(0)) as Statement
        val stm2 = visit(ctx.statement(1)) as Statement
        return appendStmt(stm1, stm2)
    }

    override fun visitPlus_expression(ctx: Plus_expressionContext?): ProgramElement {
        return ArithExpr(Operator.PLUS, listOf(visit(ctx!!.expression(0)) as Expression, visit(ctx.expression(1)) as Expression))
    }

    override fun visitMult_expression(ctx: Mult_expressionContext?): ProgramElement {
        return ArithExpr(Operator.MULT, listOf(visit(ctx!!.expression(0)) as Expression, visit(ctx.expression(1)) as Expression))
    }
    override fun visitDiv_expression(ctx: Div_expressionContext?): ProgramElement {
        return ArithExpr(Operator.DIV, listOf(visit(ctx!!.expression(0)) as Expression, visit(ctx.expression(1)) as Expression))
    }

    override fun visitMinus_expression(ctx: Minus_expressionContext?): ProgramElement {
        return ArithExpr(Operator.MINUS, listOf(visit(ctx!!.expression(0)) as Expression, visit(ctx.expression(1)) as Expression))
    }

    override fun visitGeq_expression(ctx: Geq_expressionContext?): ProgramElement {
        return ArithExpr(Operator.GEQ, listOf(visit(ctx!!.expression(0)) as Expression, visit(ctx.expression(1)) as Expression))
    }
    override fun visitLeq_expression(ctx: Leq_expressionContext?): ProgramElement {
        return ArithExpr(Operator.LEQ, listOf(visit(ctx!!.expression(0)) as Expression, visit(ctx.expression(1)) as Expression))
    }
    override fun visitGt_expression(ctx: Gt_expressionContext?): ProgramElement {
        return ArithExpr(Operator.GT, listOf(visit(ctx!!.expression(0)) as Expression, visit(ctx.expression(1)) as Expression))
    }
    override fun visitLt_expression(ctx: Lt_expressionContext?): ProgramElement {
        return ArithExpr(Operator.LT, listOf(visit(ctx!!.expression(0)) as Expression, visit(ctx.expression(1)) as Expression))
    }
    override fun visitAnd_expression(ctx: And_expressionContext?): ProgramElement {
        return ArithExpr(Operator.AND, listOf(visit(ctx!!.expression(0)) as Expression, visit(ctx.expression(1)) as Expression))
    }
    override fun visitOr_expression(ctx: Or_expressionContext?): ProgramElement {
        return ArithExpr(Operator.OR, listOf(visit(ctx!!.expression(0)) as Expression, visit(ctx.expression(1)) as Expression))
    }

    override fun visitEq_expression(ctx: Eq_expressionContext?): ProgramElement {
        return ArithExpr(Operator.EQ, listOf(visit(ctx!!.expression(0)) as Expression, visit(ctx.expression(1)) as Expression))
    }
    override fun visitNeq_expression(ctx: Neq_expressionContext?): ProgramElement {
        return ArithExpr(Operator.NEQ, listOf(visit(ctx!!.expression(0)) as Expression, visit(ctx.expression(1)) as Expression))
    }

    override fun visitNot_expression(ctx: Not_expressionContext?): ProgramElement {
        return ArithExpr(Operator.NOT, listOf(visit(ctx!!.expression()) as Expression))
    }

    override fun visitConst_expression(ctx: Const_expressionContext?): ProgramElement {
        val inner = ctx!!.CONSTANT()!!.text
        return if(inner.toIntOrNull() != null) LiteralExpr(inner, INTTYPE) else LiteralExpr(inner, ERRORTYPE)
    }
    override fun visitTrue_expression(ctx: True_expressionContext?): ProgramElement {
        return TRUEEXPR
    }
    override fun visitFalse_expression(ctx: False_expressionContext?): ProgramElement {
        return FALSEEXPR
    }

    override fun visitNull_expression(ctx: Null_expressionContext?): ProgramElement {
        return LiteralExpr("null")
    }

    override fun visitString_expression(ctx: String_expressionContext?): ProgramElement {
        val inner = ctx!!.STRING().text
        return LiteralExpr(inner, STRINGTYPE)
    }

    override fun visitDouble_expression(ctx: Double_expressionContext?): ProgramElement {
        val inner = ctx!!.FLOAT().text
        return LiteralExpr(inner, DOUBLETYPE)
    }

    override fun visitNested_expression(ctx: Nested_expressionContext?): ProgramElement {
        return visit(ctx!!.expression())
    }

    override fun visitVar_expression(ctx: Var_expressionContext?): ProgramElement {
        return LocalVar(ctx!!.NAME().text)
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

    override fun visitVarInit(ctx: VarInitContext?): ProgramElement {
        return VarInit(ctx!!.NAME().text, visit(ctx.expression()) as Expression)
    }

    private fun getClassDecl(ctx : RuleContext): Class_defContext? {
        if(ctx.parent == null) return null
        if(ctx.parent is Class_defContext) return ctx.parent as Class_defContext
        return getClassDecl(ctx.parent)
    }
}
