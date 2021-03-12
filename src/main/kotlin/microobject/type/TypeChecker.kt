package microobject.type

import antlr.microobject.gen.WhileParser
import microobject.main.Settings
import microobject.runtime.FieldInfo
import microobject.runtime.StaticTable
import microobject.runtime.Visibility
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RuleContext
import org.javafmi.wrapper.Simulation
import java.nio.file.Files
import java.nio.file.Paths


/**
 *
 * This has for now the following quirks:
 *  - Generic arguments share a global namespace
 *  - Variables cannot declared twice in a method, even if the scopes do not overlap
 *  - Generic classes cannot be extended
 *
 */

class TypeChecker(private val ctx: WhileParser.ProgramContext, private val settings: Settings, private val staticTable: StaticTable) : TypeErrorLogger() {

    companion object{
        /**********************************************************************
        Handling type data structures
         ***********************************************************************/
        //translates a string text to a type, even accessed within class createClass (needed to determine generics)
        private fun stringToType(text: String, createClass: String, generics : MutableMap<String, List<String>>) : Type {
            return when {
                generics.getOrDefault(createClass, listOf()).contains(text) -> GenericType(text)
                text == INTTYPE.name -> INTTYPE
                text == BOOLEANTYPE.name -> BOOLEANTYPE
                text == STRINGTYPE.name -> STRINGTYPE
                text == DOUBLETYPE.name -> DOUBLETYPE
                else -> BaseType(text)
            }
        }

        //translates a type AST text to a type, even accessed within class createClass (needed to determine generics)
        fun translateType(ctx : WhileParser.TypeContext, className : String, generics : MutableMap<String, List<String>>) : Type {
            return when(ctx){
                is WhileParser.Simple_typeContext -> stringToType(ctx.text, className, generics)
                is WhileParser.Nested_typeContext -> {
                    val lead = stringToType(ctx.NAME().text, className, generics)
                    ComposedType(lead, ctx.typelist().type().map { translateType(it, className, generics) })
                }
                is WhileParser.Fmu_typeContext -> {
                    val ins = if(ctx.`in` != null) ctx.`in`.param().map { Pair(it.NAME().text, translateType(it.type(), className, generics)) }
                    else emptyList()
                    val outs = if(ctx.out != null) ctx.out.param().map { Pair(it.NAME().text, translateType(it.type(), className, generics)) }
                    else emptyList()
                    SimulatorType(ins,outs)
                }
                else -> throw Exception("Unknown type context: $ctx") // making the type checker happy
            }
        }
    }

    //Known classes
    private val classes : MutableSet<String> = mutableSetOf("Int", "Boolean", "Unit", "String", "Object", "Double")

    //Type hierarchy. Null is handled as a special case during the analysis itself.
    private val extends : MutableMap<String, String> = mutableMapOf(Pair("Double", "Object"), Pair("Int", "Object"), Pair("Boolean", "Object"), Pair("Unit", "Object"), Pair("String", "Object"))

    //List of declared generic type names per class
    private val generics : MutableMap<String, List<String>> = mutableMapOf()

    //List of declared fields (with type and visibility) per class
    private val fields : MutableMap<String, Map<String, FieldInfo>> = mutableMapOf()

    //List of declared parameters per class (TODO: remove)
    private val parameters : MutableMap<String, List<String>> = mutableMapOf()

    //List of declared methods per class
    private val methods : MutableMap<String, List<WhileParser.Method_defContext>> = mutableMapOf()

    //Easy access by mapping each class name to its definition
    private val recoverDef :  MutableMap<String, WhileParser.Class_defContext> = mutableMapOf()


    /**********************************************************************
    CSSA
     ***********************************************************************/
    private val queryCheckers = mutableListOf<QueryChecker>()

    override fun report(silent: Boolean): Boolean {
        return super.report(silent) && queryCheckers.fold(true, {acc, nx -> acc && nx.report(silent)})
    }


    /**********************************************************************
    Preprocessing
     ***********************************************************************/
    internal fun collect(){
        //two passes for correct translation of types
        for(clCtx in ctx.class_def()){
            val name = clCtx.NAME(0).text
            classes.add(name)
            recoverDef[name] = clCtx
        }
        for(clCtx in ctx.class_def()){
            val name = clCtx.NAME(0).text
            if(clCtx.NAME(1) != null) extends[name] = clCtx.NAME(1).text
            else                         extends[name] = OBJECTTYPE.name
            if(clCtx.namelist() != null)
                generics[name] = clCtx.namelist().NAME().map { it.text }
            if(clCtx.method_def() != null)
                methods[name] = clCtx.method_def()
            if(clCtx.fieldDeclList() == null) {
                fields[name] = mapOf()
            }else {
                val next = clCtx.fieldDeclList().fieldDecl().map {
                    val cVisibility =
                        if (it.visibility == null) Visibility.PUBLIC else if (it.visibility.PROTECTED() != null) Visibility.PROTECTED else Visibility.PRIVATE
                    val iVisibility =
                        if (it.infer == null) Visibility.PUBLIC else if(it.infer.INFERPROTECTED() != null) Visibility.PROTECTED else Visibility.PRIVATE
                    Pair(it.NAME().text, FieldInfo(it.NAME().text, translateType(it.type(), name, generics), cVisibility, iVisibility, BaseType(name)))
                }
                fields[name] = next.toMap()
            }
            parameters[name] = if(clCtx.fieldDeclList() == null) listOf() else clCtx.fieldDeclList().fieldDecl().map { it.NAME().text }
        }
    }

    /**********************************************************************
    Actual type checking
     ***********************************************************************/

    /* interface */
    fun check() {
        //preprocessing
        collect()

        //check classes
        for (clCtx in ctx.class_def()) checkClass(clCtx)

        //check main block
        checkStatement(ctx.statement(), false, mutableMapOf(), ERRORTYPE, ERRORTYPE, ERRORTYPE.name, false)
    }

    internal fun checkClass(clCtx : WhileParser.Class_defContext){
        val name = clCtx.NAME(0).text

        //Check extends: class must exist and not be generic
        if (clCtx.NAME(1) != null) {
            val extName = clCtx.NAME(1).text
            if (!classes.contains(extName)) log("Class $name extends unknown class $extName.", clCtx)
            else {
                if(recoverDef.containsKey(extName) && recoverDef[extName]!!.namelist() != null)
                    log("Class $name extends generic class $extName.", clCtx)
            }
        }

        //Check generics: no shadowing
        if(clCtx.namelist() != null){
            val collisions = clCtx.namelist().NAME().filter { classes.contains(it.text)  }
            for(col in collisions)
                log("Class $name has type parameter ${col.text} that shadows a known type.", clCtx)

            val globalColl = clCtx.namelist().NAME().filter { it -> generics.filter { it.key != name }.values.fold(listOf<String>(), { acc, nx -> acc + nx}).contains(it.text) }
            for(col in globalColl)
                log("Class $name has type parameter ${col.text} that shadows another type parameter (type parameters share a global namespace).", clCtx)
        }

        //Check fields
        if(clCtx.fieldDeclList() != null){
            for( param in clCtx.fieldDeclList().fieldDecl()){
                val paramName = param.NAME().text
                val paramType = translateType(param.type(), name, generics)
                if(param.infer != null)
                    log("Inference visibility is not supported yet.", param, Severity.WARNING)
                if(containsUnknown(paramType, classes))
                    log("Class $name has unknown type $paramType for field $paramName.", param)
            }
        }

        //Check methods
        if(clCtx.method_def() != null)
            for( mtCtx in clCtx.method_def() ) checkMet(mtCtx, name)


    }

    private fun checkOverride(mtCtx: WhileParser.Method_defContext, className: String){
        val name = mtCtx.NAME().text
        val upwards = recoverDef[className]!!.NAME(1).text
        val allMets = getMethods(upwards).filter { it.NAME().text == name }
        if (allMets.isEmpty()) {
            log(
                "Method $name is declared as overriding in $className, but no superclass of $className implements $name.",
                mtCtx,
                Severity.WARNING
            )
        }
        for (superMet in allMets) {
            if (translateType(superMet.type(), className, generics) != translateType(mtCtx.type(), className, generics))
                log(
                    "Method $name is declared as overriding in $className, but a superclass has a different return type: ${
                        translateType(
                            superMet.type(),
                            className
                            , generics)
                    }.", mtCtx
                )
            if(mtCtx.paramList() != null) {
                if (superMet.paramList() != null) {
                    if (superMet.paramList().param().size != mtCtx.paramList().param().size)
                        log(
                            "Method $name is declared as overriding in $className, but a superclass has a different number of parameters: ${
                                superMet.paramList().param().size
                            }.", mtCtx
                        )
                    else {
                        for (i in mtCtx.paramList().param().indices) {
                            val myName = mtCtx.paramList().param(i).NAME().text
                            val otherName = superMet.paramList().param(i).NAME().text
                            val myType = translateType(mtCtx.paramList().param(i).type(), className, generics)
                            val otherType = translateType(superMet.paramList().param(i).type(), className, generics)
                            if (myName != otherName)
                                log(
                                    "Method $name is declared as overriding in $className, but parameter $i ($myType $myName) has a different name in a superclass of $className: $otherName.",
                                    mtCtx,
                                    Severity.WARNING
                                )
                            if (myType != otherType)
                                log(
                                    "Method $name is declared as overriding in $className, but parameter $i ($myType $myName) has a different type in a superclass of $className: $otherType.",
                                    mtCtx
                                )
                        }
                    }
                } else log("Method $name is declared as overriding in $className, but a superclass has a different number of parameters.", mtCtx)
            } else if(superMet.paramList() != null)
                log("Method $name is declared as overriding in $className, but a superclass has a different number of parameters.", mtCtx)
        }
    }

    internal fun checkMet(mtCtx: WhileParser.Method_defContext, className: String) {
        val name = mtCtx.NAME().text
        val gens = generics.getOrDefault(className, listOf()).map { GenericType(it) }
        val thisType = if(gens.isNotEmpty()) ComposedType(BaseType(className), gens) else BaseType(className)

        //Check rule annotation
        if(mtCtx.builtinrule != null && mtCtx.paramList() != null)
            log("rule-method $className.$name has non-empty parameter list.", mtCtx)

        //Check return type: must be known
        if(containsUnknown(translateType(mtCtx.type(), className, generics), classes))
            log("Method $className.$name has unknown return type ${mtCtx.type()}.", mtCtx.type())

        //Check parameters: no shadowing, types must be known
        if(mtCtx.paramList() != null){
            for(param in mtCtx.paramList().param()){
                val paramType = translateType(param.type(), className, generics)
                val paramName = param.NAME().text
                if(containsUnknown(paramType, classes))
                    log("Method $className.$name has unknown parameter type $paramType for parameter $paramName.", mtCtx.type())

                if(fields[className]!!.containsKey(paramName))
                    log("Method $className.$name has parameter $paramName that shadows a field.", mtCtx)
            }
        }

        //Check overriding
        if(mtCtx.overriding != null){
            if(recoverDef[className]!!.NAME(1) != null) {
                checkOverride(mtCtx, className)
            } else {
                log("Method $name is declared as overriding in $className, but $className does not extend any other class.", mtCtx, Severity.WARNING)
            }
       }
        if(mtCtx.overriding == null && recoverDef[className]!!.NAME(1) != null){
            val upwards = recoverDef[className]!!.NAME(1).text
            val allMets = getMethods(upwards).filter { it.NAME().text == name }
            if(allMets.isNotEmpty()){
                log("Method $name is not declared as overriding in $className, but a superclass of $className implements $name.", mtCtx, Severity.WARNING)
            }
        }

        //Check statement
        val initVars = if(mtCtx.paramList() != null) mtCtx.paramList().param().map { Pair(it.NAME().text, translateType(it.type(), className, generics)) }.toMap().toMutableMap() else mutableMapOf()
        val ret = checkStatement(mtCtx.statement(), false, initVars, translateType(mtCtx.type(), className, generics), thisType, className, mtCtx.builtinrule != null)

        if(!ret) log("Method ${mtCtx.NAME().text} has a path without a final return statement.", mtCtx)

        //check queries
        queryCheckers.forEach { it.type(staticTable) }
    }

    // This cannot be done with extension methods because they cannot override StatementContext.checkStatement()
    // TODO: move this to an antlr visitor
    private fun checkStatement(ctx : WhileParser.StatementContext,
                               finished : Boolean,
                               vars : MutableMap<String, Type>,
                               metType : Type, //return type
                               thisType : Type,
                               className: String,
                               inRule : Boolean ) : Boolean {
        val inner : Map<String, FieldInfo> = getFields(className)

        when(ctx){
            is WhileParser.If_statementContext -> {
                val innerType = getType(ctx.expression(), inner, vars, thisType, inRule)
                if(innerType != ERRORTYPE && innerType != BOOLEANTYPE)
                    log("If statement expects a boolean in its guard, but parameter has type $innerType.",ctx)
                val left = checkStatement(ctx.statement(0), finished, vars, metType, thisType, className, inRule)
                val right = if(ctx.ELSE() != null) checkStatement(ctx.statement(1), finished, vars, metType, thisType, className, inRule) else true
                return if(ctx.next != null){
                    checkStatement(ctx.next,  (left && right) || finished, vars, metType, thisType, className, inRule)
                } else (left && right) || finished
            }
            is WhileParser.While_statementContext -> {
                val innerType = getType(ctx.expression(), inner, vars, thisType, inRule)
                if(innerType != ERRORTYPE && innerType != BOOLEANTYPE)
                    log("While statement expects a boolean in its guard, but parameter has type $innerType.",ctx)
                checkStatement(ctx.statement(0), finished, vars, metType, thisType, className, inRule)
                if(ctx.next != null)
                    return checkStatement(ctx.next,  finished, vars, metType, thisType, className, inRule)
            }
            is WhileParser.Sequence_statementContext -> {
                val first = checkStatement(ctx.statement(0), finished, vars, metType, thisType, className, inRule)
                return checkStatement(ctx.statement(1), first, vars, metType, thisType, className, inRule)
            }
            is WhileParser.Assign_statementContext -> {
                val lhsType =
                    if(ctx.type() != null){
                        val lhs = ctx.expression(0)
                        if(lhs !is WhileParser.Var_expressionContext){
                            log("Variable declaration must declare a variable.", ctx)
                        } else {
                            val name = lhs.NAME().text
                            if (vars.keys.contains(name)) log("Variable $name declared twice.", ctx)
                            else vars[name] = translateType(ctx.type(), className, generics)
                        }
                        translateType(ctx.type(), className, generics)
                    } else getType(ctx.expression(0), inner, vars, thisType, false)
                val rhsType = getType(ctx.expression(1), inner, vars, thisType, inRule)
                if(lhsType != ERRORTYPE && rhsType != ERRORTYPE && !lhsType.isAssignable(rhsType, extends))
                    log("Type $rhsType is not assignable to $lhsType", ctx)
            }
            is WhileParser.Super_statementContext -> {
                val lhsType =
                    when {
                        ctx.type() != null -> {
                            val lhs = ctx.expression(0)
                            if (lhs !is WhileParser.Var_expressionContext) {
                                log("Variable declaration must declare a variable.", ctx)
                            } else {
                                val name = lhs.NAME().text
                                if (vars.keys.contains(name)) log("Variable $name declared twice.", ctx)
                                else vars[name] = translateType(ctx.type(), className, generics)
                            }
                            translateType(ctx.type(), className, generics)
                        }
                        ctx.target != null -> {
                            getType(ctx.expression(0), inner, vars, thisType, inRule)
                        }
                        else -> {
                            null
                        }
                    }


                if (lhsType != null && !lhsType.isAssignable(metType, extends))
                    log("Return value of super call cannot be assigned to type.", ctx)


                var metCtx: RuleContext? = ctx
                while (metCtx != null && metCtx !is WhileParser.Method_defContext) {
                    metCtx = metCtx.parent
                }
                val methodName = (metCtx as WhileParser.Method_defContext).NAME().text

                val calleeIndex = if (ctx.target == null) 0 else 1
                val met = methods.getOrDefault(className, listOf()).first { it.NAME().text == methodName }
                if (ctx.expression() != null) {
                    val callParams: List<Type> = getParameterTypes(met, className)
                    if (ctx.expression().size - calleeIndex != callParams.size) {
                        log(
                            "Mismatching number of parameters when calling super in $methodName. Expected ${
                                callParams.size
                            }, got ${ctx.expression().size  - calleeIndex}", ctx
                        )
                    } else {
                        for (i in calleeIndex until ctx.expression().size) {
                            val match = i - calleeIndex
                            val targetType = callParams[match] //of method decl
                            val realType =
                                getType(ctx.expression(i), inner, vars, thisType, inRule)                    //of call
                            if (targetType != ERRORTYPE && realType != ERRORTYPE && !realType.isAssignable(targetType, extends))
                                log("Type $realType is not assignable to $targetType.", ctx)

                        }
                    }
                }
            }
            is WhileParser.Call_statementContext -> {
                val lhsType =
                    when {
                        ctx.type() != null -> {
                            val lhs = ctx.expression(0)
                            if(lhs !is WhileParser.Var_expressionContext){
                                log("Variable declaration must declare a variable.", ctx)
                            } else {
                                val name = lhs.NAME().text
                                if (vars.keys.contains(name)) log("Variable $name declared twice.", ctx)
                                else vars[name] = translateType(ctx.type(), className, generics)
                            }
                            translateType(ctx.type(), className, generics)
                        }
                        ctx.target != null -> {
                            getType(ctx.expression(0), inner, vars, thisType, false)
                        }
                        else -> {
                            null
                        }
                    }

                val calleeIndex = if(ctx.target == null) 0 else 1
                val rhsType = getType(ctx.expression(calleeIndex), inner, vars, thisType, inRule, inRule)
                if(rhsType.getPrimary() !is BaseType || !methods.containsKey(rhsType.getPrimary().getNameString())){
                    log("Call on type $rhsType not possible: type $rhsType is not a class.", ctx)
                } else {
                  val calledMet = ctx.NAME().text
                  if(!methods.getOrDefault(rhsType.getPrimary().getNameString(), listOf()).map { it.NAME().text }.contains(calledMet)){
                      log("Call on type $rhsType not possible: method $calledMet not found.", ctx)
                  } else {
                      val otherClassName = rhsType.getPrimary().getNameString()
                      val met = methods.getOrDefault(otherClassName, listOf()).first { it.NAME().text == calledMet }
                      if(met.builtinrule == null && inRule)
                          log("Method $otherClassName.$calledMet is not a rule-method, but accessed from one.", ctx)
                      if(met.visibility != null && met.visibility.PRIVATE() != null && BaseType(otherClassName) != thisType)
                          log("Method $otherClassName.$calledMet is declared private, but accessed from $thisType.", ctx)
                      if(met.visibility != null && met.visibility.PROTECTED() != null && !BaseType(otherClassName).isAssignable(thisType, extends))
                          log("Method $otherClassName.$calledMet is declared protected, but accessed from $thisType.", ctx)
                      if(met.paramList() != null) {
                          val callParams : List<Type> = getParameterTypes(met, otherClassName)
                          if (ctx.expression().size - 1 - calleeIndex != callParams.size) {
                              log(
                                  "Mismatching number of parameters when calling $rhsType.$calledMet. Expected ${
                                      callParams.size
                                  }, got ${ctx.expression().size - 1 - calleeIndex}", ctx
                              )
                          } else {
                              for (i in calleeIndex + 1 until ctx.expression().size) {
                                  val match = i - calleeIndex - 1
                                  val targetType = callParams[match] //of method decl
                                  val realType = getType(ctx.expression(i), inner, vars, thisType, inRule)                    //of call
                                  val finalType = instantiateGenerics(targetType, rhsType, otherClassName, generics.getOrDefault(className, listOf()))
                                  if (targetType != ERRORTYPE && realType != ERRORTYPE && !realType.isAssignable(finalType, extends))
                                      log("Type $realType is not assignable to $targetType.", ctx)
                              }
                              if (lhsType != null) { //result type
                                  val metRet = translateType(met.type(), otherClassName, generics) // type as declared
                                  val finalType = instantiateGenerics(metRet, rhsType, otherClassName, generics.getOrDefault(className, listOf()))
                                  if (!lhsType.isAssignable(finalType, extends)) {
                                      log(
                                          "Type $finalType is not assignable to $lhsType.",
                                          ctx
                                      )
                                  }
                              }
                          }
                      }
                  }
                }
            }
            is WhileParser.Create_statementContext -> {
                val lhsType =
                    if (ctx.type() != null) {
                        val lhs = ctx.expression(0)
                        if(lhs !is WhileParser.Var_expressionContext){
                            log("Variable declaration must declare a variable.", ctx)
                        } else {
                            val name = lhs.NAME().text
                            if (vars.keys.contains(name)) log("Variable $name declared twice.", ctx)
                            else vars[name] = translateType(ctx.type(), className, generics)
                        }
                        translateType(ctx.type(), className, generics)
                    } else getType(ctx.expression(0), inner, vars, thisType, false)
                val createClass = ctx.NAME().text
                val createDecl = recoverDef[createClass]
                var newType : Type = BaseType(createClass)

                if(ctx.namelist() != null)
                    newType = ComposedType(newType, ctx.namelist().NAME().map {
                        stringToType(it.text, className, generics)
                    })

                if(createDecl?.namelist() != null){
                    if(ctx.namelist() == null ){
                        log("Generic parameters for $createClass missing.", ctx)
                    }else if(createDecl.namelist().NAME().size != ctx.namelist().NAME().size){
                        log("Number of generic parameters for $createClass is wrong.", ctx)
                    }
                }

                val creationParameters = getParameterTypes(createClass)
                if (creationParameters.size == (ctx.expression().size - 1)){
                    for(i in 1 until ctx.expression().size){
                        val targetType = creationParameters[i-1]
                        val finalType = instantiateGenerics(targetType, newType, createClass, generics.getOrDefault(className, listOf()))
                        val realType = getType(ctx.expression(i), inner, vars, thisType, inRule)
                        if(targetType != ERRORTYPE && realType != ERRORTYPE && !finalType.isAssignable(realType, extends)) {
                            log("Type $realType is not assignable to $finalType", ctx)
                        }
                    }
                } else {
                    log("Mismatching number of parameters when creating an $createClass instance. Expected ${creationParameters.size}, got ${ctx.expression().size-1}", ctx)
                }



                if(lhsType != ERRORTYPE && !lhsType.isAssignable(newType, extends) ) {
                    log("Type $createClass is not assignable to $lhsType", ctx)
                }

            }
            is WhileParser.Sparql_statementContext -> {
                var expType : Type? = null
                if(ctx.declType != null){
                    val lhs = ctx.expression(0)
                    if(lhs !is WhileParser.Var_expressionContext){
                        log("Variable declaration must declare a variable.", ctx)
                    } else {
                        val name = lhs.NAME().text
                        if (vars.keys.contains(name)) log("Variable $name declared twice.", ctx)
                        else {
                            expType = translateType(ctx.type(), className, generics)
                            vars[name] = expType
                        }
                    }
                }else{
                    expType = getType(ctx.target, inner, vars, thisType, false)
                }
                if(expType != null) {
                    val qc = QueryChecker(settings, ctx.query.text.removeSurrounding("\""), expType, ctx, "obj")
                    queryCheckers.add(qc)
                }
            }
            is WhileParser.Construct_statementContext -> {
                var expType : Type? = null
                if(ctx.declType != null){
                    val lhs = ctx.expression(0)
                    if(lhs !is WhileParser.Var_expressionContext){
                        log("Variable declaration must declare a variable.", ctx)
                    } else {
                        val name = lhs.NAME().text
                        if (vars.keys.contains(name)) log("Variable $name declared twice.", ctx)
                        else {
                            expType = translateType(ctx.type(), className, generics)
                            vars[name] = expType
                        }
                    }
                }else{
                    expType = getType(ctx.target, inner, vars, thisType, false)
                }
                if(expType != null && fields.containsKey(expType.getPrimary().toString())) {
                    val fieldName = fields[expType.getPrimary().toString()]
                    for(f in fieldName!!.entries) {
                        val qc = QueryChecker(settings, ctx.query.text.removeSurrounding("\""), f.value.type, ctx, f.key)
                        queryCheckers.add(qc)
                    }
                }
            }
            is WhileParser.Owl_statementContext -> {
                log("Type checking this form of (C)SSA is not supported yet ", ctx, Severity.WARNING)
                if(ctx.declType != null){
                    val lhs = ctx.expression(0)
                    if(lhs !is WhileParser.Var_expressionContext){
                        log("Variable declaration must declare a variable.", ctx)
                    } else {
                        val name = lhs.NAME().text
                        if (vars.keys.contains(name)) log("Variable $name declared twice.", ctx)
                        else vars[name] = translateType(ctx.type(), className, generics)
                    }
                }
            }
            is WhileParser.Return_statementContext -> {
                val innerType = getType(ctx.expression(), inner, vars, thisType, inRule)
                if(innerType != ERRORTYPE && innerType != metType && !metType.isAssignable(innerType, extends))
                    log("Type $innerType of return statement does not match method type ${metType}.",ctx)
                return true
            }
            is WhileParser.Output_statementContext -> {
                //For now, we print everything
                /*val innerType = getType(ctx.expression(), inner, vars, thisType)
                if(innerType != microobject.data.getERRORTYPE && innerType != microobject.data.getSTRINGTYPE)
                    log("Println statement expects a string, but parameter has type $innerType.", ctx)*/
            }
            is WhileParser.Skip_statmentContext -> { }
            is WhileParser.Debug_statementContext -> { }
            is WhileParser.Simulate_statementContext -> {
                val path = ctx.path.text.removeSurrounding("\"")
                if (!Files.exists(Paths.get(path))) {
                    log("Could not find file for FMU $path, statement cannot be type checked", ctx)
                } else{
                    val sim = Simulation(path)
                    val inits = if(ctx.varInitList() != null)
                                ctx.varInitList().varInit().map { Pair(it.NAME().text,getType(it.expression(), inner, vars, thisType, inRule)) }.toMap()
                                else
                                 emptyMap()

                    var ins = listOf<Pair<String,Type>>()
                    var outs = listOf<Pair<String,Type>>()

                    for(mVar in sim.modelDescription.modelVariables){
                        if(mVar.causality == "input" || mVar.causality == "state"){
                            if(!mVar.hasStartValue() && !inits.containsKey(mVar.name))
                                log("Simulation fails to initialize variable ${mVar.name}: no initial value given", ctx)
                        }
                        if((mVar.causality == "output" || mVar.initial == "calculated") && inits.containsKey(mVar.name)) {
                            log("Cannot initialize output or/and calculated variable ${mVar.name}",ctx)
                        }

                        if(mVar.causality == "input") ins = ins + Pair(mVar.name, getSimType(mVar.typeName))
                        if(mVar.causality == "output") outs = outs + Pair(mVar.name, getSimType(mVar.typeName))
                    }
                    val simType = SimulatorType(ins, outs)

                    if(ctx.type() != null) {
                        val declType = translateType(ctx.type(), className, generics)
                        if(!declType.isAssignable(simType, extends))
                            log("Type $simType is not assignable to $declType", ctx)
                        if (ctx.target !is WhileParser.Var_expressionContext) {
                            log("Variable declaration must declare a variable.", ctx)
                        } else {
                            val name = ((ctx.target) as WhileParser.Var_expressionContext).NAME().text
                            if (vars.keys.contains(name)) log("Variable $name declared twice.", ctx)
                            else vars[name] = translateType(ctx.type(), className, generics)
                        }
                    }else{
                        val declType = getType(ctx.target, inner, vars, thisType, false)
                        if(!declType.isAssignable(simType, extends))
                            log("Type $simType is not assignable to $declType", ctx)
                    }

                }
            }
            is WhileParser.Tick_statementContext -> {
                val fmuType = getType(ctx.fmu, inner, vars, thisType, inRule)
                val tickType = getType(ctx.time, inner, vars, thisType, inRule)
                if(fmuType !is SimulatorType)
                    log("Tick statement expects a FMU as first parameter, but got $fmuType }.",ctx)
                if(tickType != DOUBLETYPE)
                    log("Tick statement expects a Double as second parameter, but got $tickType }.",ctx)
            }
            else -> {
                log("Statements with class ${ctx.javaClass} cannot be type checked",ctx)
            }
        }
        return false
    }

    private fun getSimType(typeName: String): Type =
        when(typeName) {
            "Integer" -> INTTYPE
            "Real" -> DOUBLETYPE
            "String" -> STRINGTYPE
            "Boolean" -> BOOLEANTYPE
            else -> ERRORTYPE
        }


    // This cannot be done with extension methods because they cannot override ExpressionContext.getType()
    private fun getType(eCtx : WhileParser.ExpressionContext,
                        fields : Map<String, FieldInfo>,
                        vars : Map<String, Type>,
                        thisType : Type,
                        inRule : Boolean,
                        read: Boolean = true,
    ) : Type {
        when(eCtx){
            is WhileParser.Const_expressionContext -> return INTTYPE
            is WhileParser.Double_expressionContext -> return DOUBLETYPE
            is WhileParser.String_expressionContext -> return STRINGTYPE
            is WhileParser.False_expressionContext -> return BOOLEANTYPE
            is WhileParser.True_expressionContext -> return BOOLEANTYPE
            is WhileParser.Null_expressionContext -> return NULLTYPE
            is WhileParser.Var_expressionContext -> {
                val name = eCtx.NAME().text
                if(!vars.containsKey(name)) log("Variable $name is not declared.", eCtx)
                return vars.getOrDefault(name, ERRORTYPE)
            }
            is WhileParser.Field_expressionContext -> {
                val name = eCtx.NAME().text
                if(!fields.containsKey(name))
                    log("Field $name is not declared for $thisType.", eCtx)
                return fields.getOrDefault(name, FieldInfo(eCtx.NAME().text, ERRORTYPE, Visibility.PUBLIC, Visibility.PUBLIC, thisType)).type
            }
            is WhileParser.Nested_expressionContext -> {
                return getType(eCtx.expression(), fields, vars, thisType, inRule)
            }
            is WhileParser.Mult_expressionContext -> { // The following has a lot of code duplication and could be improved by changing the grammar
                val t1 = getType(eCtx.expression(0), fields, vars, thisType, inRule)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType, inRule)
                return typeForNumericalFunction(t1, t2, "*", eCtx)
            }
            is WhileParser.Plus_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType, inRule)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType, inRule)
                return typeForNumericalFunction(t1, t2, "+", eCtx)
            }
            is WhileParser.Minus_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType, inRule)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType, inRule)
                return typeForNumericalFunction(t1, t2, "-", eCtx)
            }
            is WhileParser.Div_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType, inRule)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType, inRule)
                return typeForNumericalFunction(t1, t2, "/", eCtx)
            }
            is WhileParser.Neq_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType, inRule)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType, inRule)
                if((t1 == ERRORTYPE && t2 == ERRORTYPE) || (t2 == t1)) return BOOLEANTYPE
                if(t1.isAssignable(t2, extends) || t2.isAssignable(t1, extends)) return BOOLEANTYPE
                log("Malformed comparison <> with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.Eq_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType, inRule)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType, inRule)
                if((t1 == ERRORTYPE && t2 == ERRORTYPE) || (t2 == t1)) return BOOLEANTYPE
                if(t1.isAssignable(t2, extends) || t2.isAssignable(t1, extends)) return BOOLEANTYPE
                log("Malformed comparison = with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.And_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType, inRule)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType, inRule)
                if((t1 == ERRORTYPE && t2 == ERRORTYPE) || (t2 == BOOLEANTYPE && t1 == BOOLEANTYPE)) return BOOLEANTYPE
                if(t1.isAssignable(t2, extends) || t2.isAssignable(t1, extends)) return BOOLEANTYPE
                log("Malformed comparison && with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.Or_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType, inRule)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType, inRule)
                if((t1 == ERRORTYPE && t2 == ERRORTYPE) || (t2 == BOOLEANTYPE && t1 == BOOLEANTYPE)) return BOOLEANTYPE
                if(t1.isAssignable(t2, extends) || t2.isAssignable(t1, extends)) return BOOLEANTYPE
                log("Malformed comparison || with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.Not_expressionContext -> {
                val t1 = getType(eCtx.expression(), fields, vars, thisType, inRule)
                if(t1 == ERRORTYPE || t1 == BOOLEANTYPE) return BOOLEANTYPE
                log("Malformed negation ! with subtypes $t1", eCtx)
                return ERRORTYPE
            }
            is WhileParser.Leq_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType, inRule)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType, inRule)
                return typeForNumericalRelation(t1, t2, "<=", eCtx)
            }
            is WhileParser.Geq_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType, inRule)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType, inRule)
                return typeForNumericalRelation(t1, t2, ">=", eCtx)
            }
            is WhileParser.Lt_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType, inRule)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType, inRule)
                return typeForNumericalRelation(t1, t2, "<", eCtx)
            }
            is WhileParser.Gt_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType, inRule)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType, inRule)
                return typeForNumericalRelation(t1, t2, ">", eCtx)
            }
            is WhileParser.This_expressionContext -> return thisType
            is WhileParser.External_field_expressionContext -> { // This must resolve the generics
                val t1 = getType(eCtx.expression(), fields, vars, thisType, inRule)
                if(t1 == ERRORTYPE) return ERRORTYPE
                if(t1 is SimulatorType){
                    return if(read){
                        val inVar = t1.outVar.firstOrNull { it.first == eCtx.NAME().text }
                        if(inVar == null)
                            log("Trying to read from a field that is not an outport: ${eCtx.NAME().text}", eCtx)
                        inVar?.second ?: ERRORTYPE
                    } else {
                        val inVar = t1.inVar.firstOrNull { it.first == eCtx.NAME().text }
                        if(inVar == null)
                            log("Trying to write into a field that is not an inport: ${eCtx.NAME().text}", eCtx)
                        inVar?.second ?: ERRORTYPE
                    }
                }
                if(t1 is GenericType) {
                    log("Access of fields of generic types is not supported.", eCtx)
                    return ERRORTYPE
                }
                val primary = t1.getPrimary()
                val primName = primary.getNameString()
                if(!this.fields.containsKey(primName)){
                    log("Cannot access fields of $primary.", eCtx)
                    return ERRORTYPE
                }
                val otherFields = getFields(primName)
                if(!otherFields.containsKey(eCtx.NAME().text)){
                    log("Field ${eCtx.NAME().text} is not declared for $primary.", eCtx)
                    return ERRORTYPE
                } else {
                    if(otherFields[eCtx.NAME().text]!!.computationVisibility == Visibility.PRIVATE){
                        if(thisType != otherFields[eCtx.NAME().text]!!.declaredIn)
                            log("Field ${otherFields[eCtx.NAME().text]!!.declaredIn}.${eCtx.NAME().text} is declared private, but accessed from $thisType.", eCtx)
                    }
                    if(otherFields[eCtx.NAME().text]!!.computationVisibility == Visibility.PROTECTED){
                        if(!otherFields[eCtx.NAME().text]!!.declaredIn.isAssignable(thisType, extends))
                            log("Field ${otherFields[eCtx.NAME().text]!!.declaredIn}.${eCtx.NAME().text} is declared protected, but accessed from $thisType.", eCtx)
                    }

                    if(inRule && otherFields[eCtx.NAME().text]!!.computationVisibility == Visibility.PRIVATE && thisType != otherFields[eCtx.NAME().text]!!.declaredIn)
                        log("Inferprivate field $eCtx.NAME().text accessed in rule-method.", eCtx)
                    if(inRule && otherFields[eCtx.NAME().text]!!.computationVisibility == Visibility.PROTECTED && !otherFields[eCtx.NAME().text]!!.declaredIn.isAssignable(thisType, extends))
                        log("Inferprotected field $eCtx.NAME().text accessed in rule-method.", eCtx)
                }
                val fieldType = this.fields.getOrDefault(primary.getNameString(), mutableMapOf()).getOrDefault(eCtx.NAME().text,
                    FieldInfo(eCtx.NAME().text, ERRORTYPE, Visibility.PUBLIC, Visibility.PUBLIC, thisType)
                )
                return instantiateGenerics(fieldType.type, t1, primName, generics.getOrDefault(thisType.getPrimary().getNameString(), listOf()))
            }
            else -> {
                log("Expression $eCtx cannot be type checked.",eCtx)
                return ERRORTYPE
            }
        }
    }


    private fun typeForNumericalFunction(t1 : Type, t2 : Type, symbol:String, ctx: ParserRuleContext) : Type {
        if(t1 == INTTYPE && t2 == INTTYPE) return INTTYPE
        if(t1 == DOUBLETYPE && t2 == DOUBLETYPE) return DOUBLETYPE
        if(t1 == ERRORTYPE && (t2 == INTTYPE || t2 == DOUBLETYPE)) return t2
        if(t2 == ERRORTYPE && (t1 == INTTYPE || t1 == DOUBLETYPE)) return t2
        log("Malformed operator $symbol with subtypes $t1 and $t2", ctx)
        return ERRORTYPE
    }

    private fun typeForNumericalRelation(t1 : Type, t2 : Type, symbol:String, ctx: ParserRuleContext) : Type {
        if((t1 == INTTYPE || t1 == DOUBLETYPE || t1 == ERRORTYPE) && (t2 == INTTYPE || t2 == DOUBLETYPE || t2 == ERRORTYPE)) return BOOLEANTYPE
        if(    (INTTYPE.isAssignable(t1, extends) || DOUBLETYPE.isAssignable(t1, extends))
            && (INTTYPE.isAssignable(t2, extends) || DOUBLETYPE.isAssignable(t2, extends))) return BOOLEANTYPE
        log("Malformed comparison $symbol with subtypes $t1 and $t2", ctx)
        return ERRORTYPE
    }

    /**********************************************************************
    Helper to handle types
     ***********************************************************************/
    private fun getParameterTypes(met: WhileParser.Method_defContext, otherClassName: String): List<Type> =
        if(met.paramList() == null) listOf() else met.paramList().param().map { translateType(it.type(), otherClassName, generics) }


    private fun getParameterTypes(className: String): List<Type> {
        val types : List<Type> = fields.getOrDefault(className, mapOf()).map { it.value.type }
        if(extends.containsKey(className)){
            val supertype = extends.getOrDefault(className, ERRORTYPE.name)
            val moreTypes = getParameterTypes(supertype)
            return moreTypes + types
        }
        return types
    }


    private fun getMethods(className: String): List<WhileParser.Method_defContext> {
        val ret : List<WhileParser.Method_defContext> = methods.getOrDefault(className, listOf())
        if(extends.containsKey(className)){
            val supertype = extends.getOrDefault(className, ERRORTYPE.name)
            val more = getMethods(supertype)
            return more + ret
        }
        return ret
    }

    /* check whether @type contains an unknown (structural) subtype  */
    private fun containsUnknown(type: Type, types: Set<String>): Boolean =
        when(type){
        is GenericType -> false // If we can translate it into a generic type, we already checked
        is SimulatorType -> false
        is BaseType -> !types.contains(type.name)
        else -> {
            val composType = type as ComposedType
            composType.params.fold(
                containsUnknown(composType.name, types),
                { acc, nx -> acc || containsUnknown(nx, types) })
        }
    }

    private fun getFields(className: String): Map<String, FieldInfo> {
        val f = fields.getOrDefault(className, mapOf())
        if(extends.containsKey(className)){
            val supertype = extends.getOrDefault(className, ERRORTYPE.name)
            val moreFields = getFields(supertype)
            return moreFields + f
        }
        return f
    }
    /**********************************************************************
    Helper to handle generics
     ***********************************************************************/

    /*
        schablone is the type of the part of className that we focus on
        concrete  is the type of the relevant className instance
        className is the class whose definitions binds the generics
        gens      are the generics of the focused class
     */
    private fun instantiateGenerics(schablone: Type, concrete: Type, className: String, gens : List<String>): Type {
        //get definition of class that binds generics
        val otherClass = recoverDef[className]

        //get its type
        val otherAbstractType =
            if(otherClass!!.namelist() == null) BaseType(className)
            else ComposedType(BaseType(className), otherClass.namelist().NAME().map { GenericType(it.text) })

        //match type of class with the concrete instance
        val matching = match(otherAbstractType, concrete)

        //apply retrieved matching to abstract part
        return applyMatching(schablone, matching, gens)
    }

    /* apply unifier */
    private fun applyMatching(metRet: Type, matching: Map<GenericType, Type>, gens : List<String>) : Type {
        when(metRet){
            is GenericType -> {
                return when {
                    matching.containsKey(metRet) -> matching.getOrDefault(metRet, ERRORTYPE)
                    gens.contains(metRet.name) -> metRet
                    else -> {
                        log("Applying the matching on $metRet failed.", null)
                        ERRORTYPE
                    }
                }
            }
            is BaseType -> return metRet
            is SimulatorType -> return metRet
            is ComposedType -> {
                return ComposedType(
                    applyMatching(metRet.name, matching, gens),
                    metRet.params.map { applyMatching(it, matching, gens) })
            }
            else -> return ERRORTYPE
        }
    }

    /* compute unifier */
    private fun match(abstractType: Type, concreteType: Type): Map<GenericType, Type> {
        when(abstractType){
            is GenericType -> return mapOf(Pair(abstractType, concreteType))
            is BaseType -> return mapOf()
            is ComposedType -> {
                if(concreteType !is ComposedType){
                    log("Matching $abstractType with $concreteType failed.", null)
                } else {
                    var matching = match(abstractType.name, concreteType.name)
                    if(abstractType.params.size != concreteType.params.size){
                        log("Matching $abstractType with $concreteType failed.", null)
                    } else {
                        for (i in abstractType.params.indices){
                            val next = match(abstractType.params[i], concreteType.params[i])
                            for(pair in next){
                                if(matching.containsKey(pair.key)){
                                    log("Matching $abstractType with $concreteType failed.", null)
                                }else matching = matching + Pair(pair.key, pair.value)
                            }
                        }
                        return matching
                    }
                }
                return mapOf()
            }
            else -> return mapOf()
        }
    }
}