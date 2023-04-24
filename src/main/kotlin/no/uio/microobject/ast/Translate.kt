@file:Suppress("UNNECESSARY_NOT_NULL_ASSERTION")

package no.uio.microobject.ast

import no.uio.microobject.antlr.WhileBaseVisitor
import no.uio.microobject.antlr.WhileParser.*
import no.uio.microobject.ast.expr.*
import no.uio.microobject.ast.stmt.*
import no.uio.microobject.runtime.*
import no.uio.microobject.type.*
import org.antlr.v4.runtime.RuleContext

/**
 * This class handles multiple tasks related to translating ANTLR structures to the internal representation
 *  - It translate statements and expression
 *  - It generates the class table
 */
class Translate : WhileBaseVisitor<ProgramElement>() {

    private val table : MutableMap<String, Pair<FieldEntry, Map<String,MethodInfo>>> = mutableMapOf()
    private val owldescr : MutableMap<String, String> = mutableMapOf()


    private fun translateModels(ctx : Models_blockContext) : Pair<List<Pair<Expression, String>>, String>{
        if(ctx is Simple_models_blockContext)
            return Pair(emptyList(), ctx.owldescription.text)
        if(ctx is Complex_models_blockContext) {
            val expr = visit(ctx.expression()) as Expression
            val str = ctx.owldescription.text
            val tail = translateModels(ctx.models_block())
            return Pair(tail.first + Pair(expr,str), tail.second)
        }
        throw Exception("Unknown models clause: $ctx") //making the type checker happy
    }

    fun generateStatic(ctx: ProgramContext?) : Pair<StackEntry,StaticTable> {
        val roots : MutableSet<String> = mutableSetOf()
        val hierarchy : MutableMap<String, MutableSet<String>> = mutableMapOf()
        var modelsTable : Map<String, List<ModelsEntry>> = emptyMap()
        var hidden : Set<String> = setOf()
        for(cl in ctx!!.class_def()){
            val modelsList =
            if(cl.models_block() != null){
                val models = translateModels(cl.models_block())
                owldescr[cl!!.className.text] = models.second
                models.first
            } else emptyList()
            modelsTable = modelsTable + Pair(cl.className.text, modelsList)
            if(cl.hidden != null) hidden = hidden + cl.className.text

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
                        val cVisibility = if(nm.HIDE() != null) Visibility.HIDE else Visibility.DEFAULT
                        res = res + FieldInfo(
                            nm.NAME().text,
                            TypeChecker.translateType(nm.type(), cl.className.text, mutableMapOf()),
                            cVisibility,
                            BaseType(cl!!.className.text),
                            nm.domain != null
                        )
                    }
                }
                res
            } else {
                listOf()
            }

            val res = mutableMapOf<String, MethodInfo>()
            for(nm in cl.method_def()){ //Pair<Statement, List<String>>
                if(nm.abs == null && nm.statement() == null)
                    throw Exception("Non-abstract method with empty statement: ${nm.NAME().text}")
                if(nm.abs != null && nm.statement() != null )
                    throw Exception("Abstract method with non-empty statement: ${nm.NAME().text}")
                if(nm.abs == null) {
                    var stmt = visit(nm!!.statement()) as Statement
                    val last = stmt.getLast()
                    val metType = TypeChecker.translateType(nm.type(), cl.NAME().text, mutableMapOf())
                    if(metType == UNITTYPE && last !is ReturnStmt){//just to be sure
                        stmt = appendStmt(stmt, ReturnStmt(UNITEXPR))
                    }
                    val params = if (nm.paramList() != null) paramListTranslate(nm.paramList()) else listOf()
                    res[nm.NAME().text] = MethodInfo(stmt, params, nm.builtinrule != null, nm.domainrule != null, cl.className.text, metType)
                }
                if(nm.abs != null) {
                    val metType = TypeChecker.translateType(nm.type(), cl.NAME().text, mutableMapOf())
                    val params = if (nm.paramList() != null) paramListTranslate(nm.paramList()) else listOf()
                    res[nm.NAME().text] = MethodInfo(SkipStmt(ctx!!.start.line), params, nm.builtinrule != null, nm.domainrule != null, cl.className.text, metType)
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
        val fieldTable : MutableMap<String, FieldEntry> = mutableMapOf()
        val methodTable : MutableMap<String, Map<String, MethodInfo>> = mutableMapOf()
        for(entry in table){
            fieldTable +=  Pair(entry.key, entry.value.first)
            methodTable +=  Pair(entry.key, entry.value.second)
        }

        return Pair(
                     StackEntry(visit(ctx.statement()) as Statement, mutableMapOf(), Names.getObjName("_Entry_"), Names.getStackId()),
                     StaticTable(fieldTable, methodTable, hierarchy, modelsTable, hidden)
                   )
    }

    private fun paramListTranslate(ctx: ParamListContext?) : List<String> {
        val res = mutableListOf<String>()
        if(ctx!!.param() != null) {
            for (nm in ctx.param())
                res += nm.NAME().text
        }
        return res
    }

    override fun visitSuper_statement(ctx: Super_statementContext?): ProgramElement {
        val ll = emptyList<Expression>().toMutableList().toMutableList()
        val def = getClassDecl(ctx as RuleContext)
        val declares = if(ctx!!.declType == null) null else
            TypeChecker.translateType(ctx.declType, if(def != null) def!!.className.text else ERRORTYPE.name, mutableMapOf())
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
                ctx!!.start.line,
                declares
            )
        } else {
            for(i in 0 until ctx.expression().size)
                ll += visit(ctx.expression(i)) as Expression
            return SuperStmt(
                visit(ctx.target) as Location,
                method,
                ll,
                ctx!!.start.line,
                declares
            )
        }
    }

    override fun visitCall_statement(ctx: Call_statementContext?): ProgramElement {
        val ll = emptyList<Expression>().toMutableList().toMutableList()
        val def = getClassDecl(ctx as RuleContext)
        val declares = if(ctx!!.declType == null) null else
            TypeChecker.translateType(ctx.declType, if(def != null) def!!.className.text else ERRORTYPE.name, mutableMapOf())
        if(ctx!!.target == null){
            for(i in 1 until ctx.expression().size)
                ll += visit(ctx.expression(i)) as Expression
            return CallStmt(Names.getVarName(),
                visit(ctx.expression(0)) as Location,
                ctx.NAME().text,
                ll,
                ctx!!.start.line,
                declares
            )
        } else {
            for(i in 2 until ctx.expression().size)
                ll += visit(ctx.expression(i)) as Expression
            return CallStmt(
                visit(ctx.target) as Location,
                visit(ctx.expression(1)) as Location,
                ctx.NAME().text,
                ll,
                ctx!!.start.line,
                declares
            )
        }
    }

    override fun visitCreate_statement(ctx: Create_statementContext?): ProgramElement {
        val ll = emptyList<Expression>().toMutableList()
        for(i in 1 until ctx!!.expression().size) {
            if(ctx.expression(i) != ctx.owldescription)
                ll += visit(ctx.expression(i)) as Expression
        }
        val def = getClassDecl(ctx)
        val targetType =
            TypeChecker.translateType(ctx.newType, if(def != null) def!!.className.text else ERRORTYPE.name, mutableMapOf())
        val myModeling = if(ctx.owldescription != null) visit(ctx.owldescription) as LiteralExpr else null
        val classModeling = if(owldescr.containsKey(targetType.getPrimary().getNameString())) owldescr!![targetType.getPrimary().getNameString()]
            ?.let { LiteralExpr(it, STRINGTYPE) } else  null

        var modeling = if(myModeling == null && classModeling == null) null
        else if ( myModeling == null ) classModeling
        else if ( classModeling == null ) myModeling
        else LiteralExpr("\"${myModeling.literal.removeSurrounding("\"")} ${classModeling.literal.removeSurrounding("\"")}\"", STRINGTYPE)
        if(modeling != null) {
            println("replacing ${modeling.literal}")
        }

        return CreateStmt(visit(ctx.target) as Location,
                          targetType.getPrimary().getNameString(),
                          ll,
                          ctx!!.start.line,
                          targetType,
                          modeling )
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
        val ll = emptyList<Expression>().toMutableList()
        for(i in 2 until ctx!!.expression().size)
            ll += visit(ctx.expression(i)) as Expression
        val mode = if(ctx.modeexpression() == null || ctx.modeexpression() is Sparql_modeContext) SparqlMode else InfluxDBMode((ctx.modeexpression() as Influx_modeContext).STRING().text)
        return AccessStmt(target, query, ll, ctx!!.start.line, mode, target.getType())
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
        val ll = emptyList<Expression>().toMutableList()
        for(i in 2 until ctx!!.expression().size)
            ll += visit(ctx.expression(i)) as Expression
        return ConstructStmt(target, query, ll, ctx!!.start.line, target.getType())
    }

    override fun visitOwl_statement(ctx: Owl_statementContext?): ProgramElement {
        val target = visit(ctx!!.target) as Location
        val query = visit(ctx!!.query) as Expression
        val def = getClassDecl(ctx as RuleContext)
        val declares = if(ctx!!.declType == null) null else
            TypeChecker.translateType(ctx.declType, if(def != null) def!!.className.text else ERRORTYPE.name, mutableMapOf())
        return OwlStmt(target, query, ctx!!.start.line, declares)
    }

    override fun visitValidate_statement(ctx: Validate_statementContext?): ProgramElement {
        val target = visit(ctx!!.target) as Location
        val query = visit(ctx!!.query) as Expression
        val def = getClassDecl(ctx as RuleContext)
        val declares = if(ctx!!.declType == null) null else
            TypeChecker.translateType(ctx.declType, if(def != null) def!!.className.text else ERRORTYPE.name, mutableMapOf())
        return ValidateStmt(target, query, ctx!!.start.line, declares)
    }

    override fun visitSimulate_statement(ctx: Simulate_statementContext?): ProgramElement {
        val path = ctx!!.path.text.removeSurrounding("\"")
        val target = visit(ctx!!.target) as Location

        val res = mutableListOf<VarInit>()
        if(ctx!!.varInitList() != null) {
            for (nm in ctx!!.varInitList()!!.varInit())
                res += visit(nm) as VarInit
        }
        val def = getClassDecl(ctx as RuleContext)
        val declares = if(ctx!!.declType == null) null else
            TypeChecker.translateType(ctx.declType, if(def != null) def!!.className.text else ERRORTYPE.name, mutableMapOf())
        return SimulationStmt(target, path,  res, ctx!!.start.line, declares)
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
        val def = getClassDecl(ctx as RuleContext)
        val declares = if(ctx!!.declType == null) null else
            TypeChecker.translateType(ctx.declType, if(def != null) def!!.className.text else ERRORTYPE.name, mutableMapOf())
        return AssignStmt(visit(ctx!!.expression(0)) as Location, visit(ctx.expression(1)) as Expression, ctx!!.start.line, declares)
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
        val stm1 = visit(ctx!!.thenS) as Statement
        val stm2 = if(ctx.elseE != null) visit(ctx.elseE) as Statement else SkipStmt()
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
    override fun visitMod_expression(ctx: Mod_expressionContext?): ProgramElement {
        return ArithExpr(Operator.MOD, listOf(visit(ctx!!.expression(0)) as Expression, visit(ctx.expression(1)) as Expression))
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

    override fun visitInteger_expression(ctx: Integer_expressionContext?): ProgramElement {
        val inner = ctx!!.INTEGER()!!.text
        return if(inner.toIntOrNull() != null) LiteralExpr(inner, INTTYPE) else LiteralExpr(inner, ERRORTYPE)
    }
    override fun visitTrue_expression(ctx: True_expressionContext?): ProgramElement {
        return TRUEEXPR
    }
    override fun visitFalse_expression(ctx: False_expressionContext?): ProgramElement {
        return FALSEEXPR
    }
    override fun visitUnit_expression(ctx: Unit_expressionContext?): ProgramElement {
        return UNITEXPR
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
        if(ctx!!.expression() is This_expressionContext)
            return OwnVar(ctx.NAME().text)
        return OthersVar(visit(ctx!!.expression()) as Expression, ctx.NAME().text)
    }
    override fun visitFmu_field_expression(ctx: Fmu_field_expressionContext?): ProgramElement {
        return OthersVar(visit(ctx!!.expression()) as Expression, ctx.STRING().text.removeSurrounding("\""))
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
