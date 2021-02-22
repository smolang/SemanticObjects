package microobject.type

import antlr.microobject.gen.WhileParser
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RuleContext
import org.javafmi.wrapper.Simulation
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Paths

//Error Messages
enum class Severity { WARNING, ERROR }
data class TypeError(val msg: String, val line: Int, val severity: Severity)


/**
 *
 * This has for now the following quirks:
 *  - Generic arguments share a global namespace
 *  - Variables cannot declared twice in a method, even if the scopes do not overlap
 *  - Generic classes cannot be extended
 *
 */

class TypeChecker(private val ctx: WhileParser.ProgramContext) {

    //Known classes
    private val classes : MutableSet<String> = mutableSetOf("Int", "Boolean", "Unit", "String", "Object")

    //Type hierarchy. Null is handled as a special case during the analysis itself.
    private val extends : MutableMap<String, String> = mutableMapOf(Pair("Int", "Object"), Pair("Boolean", "Object"), Pair("Unit", "Object"), Pair("String", "Object"))

    //List of declared generic type names per class
    private val generics : MutableMap<String, List<String>> = mutableMapOf()

    //List of declared fields (with type) per class
    private val fields : MutableMap<String, Map<String, Type>> = mutableMapOf()

    //List of declared parameters per class (TODO: remove)
    private val parameters : MutableMap<String, List<String>> = mutableMapOf()

    //List of declared methods per class
    private val methods : MutableMap<String, List<WhileParser.Method_defContext>> = mutableMapOf()

    //Easy access by mapping each class name to its definition
    private val recoverDef :  MutableMap<String, WhileParser.Class_defContext> = mutableMapOf()

    /**********************************************************************
    Error handling
     ***********************************************************************/

    //Final output: collected errors
    internal var error : List<TypeError> = listOf()

    /* interface: prints all errors and returns whether one of them is an error */
    fun report(silent : Boolean = false) : Boolean {
        var ret = true
        for( e in error ){
            if(e.severity == Severity.ERROR) ret = false
            if(!silent) println("Line ${e.line}, ${e.severity}: ${e.msg}")
        }
        return ret
    }

    /* adds new error/warning */
    private fun log(msg: String, node : ParserRuleContext?, severity: Severity = Severity.ERROR){
        error = error + TypeError(msg, node?.getStart()?.line ?: 0, severity)
    }

    /**********************************************************************
    Handling type data structures
    ***********************************************************************/
    //translates a string text to a type, even accessed within class createClass (needed to determine generics)
    private fun stringToType(text: String, createClass: String) : Type {
        return when {
            generics.getOrDefault(createClass, listOf()).contains(text) -> GenericType(text)
            text == INTTYPE.name -> INTTYPE
            text == BOOLEANTYPE.name -> BOOLEANTYPE
            text == STRINGTYPE.name -> STRINGTYPE
            else -> BaseType(text)
        }
    }


    //translates a type AST text to a type, even accessed within class createClass (needed to determine generics)
    private fun translateType(ctx : WhileParser.TypeContext, className : String) : Type {
        return when(ctx){
            is WhileParser.Simple_typeContext -> stringToType(ctx.text, className)
            is WhileParser.Nested_typeContext -> {
                val lead = stringToType(ctx.NAME().text, className)
                ComposedType(lead, ctx.typelist().type().map { translateType(it, className) })
            }
            is WhileParser.Fmu_typeContext -> {
                val ins = if(ctx.`in` != null) ctx.`in`.param().map { Pair(it.NAME().text, translateType(it.type(), className)) }
                          else emptyList()
                val outs = if(ctx.out != null) ctx.out.param().map { Pair(it.NAME().text, translateType(it.type(), className)) }
                           else emptyList()
                SimulatorType(ins,outs)
            }
            else -> throw Exception("Unknown type context: $ctx") // making the type checker happy
        }
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
            fields[name] = if(clCtx.paramList() == null) mapOf() else clCtx.paramList().param().map { Pair(it.NAME().text, translateType(it.type(), name)) }.toMap()
            parameters[name] = if(clCtx.paramList() == null) listOf() else clCtx.paramList().param().map { it.NAME().text }
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
        checkStatement(ctx.statement(), false, mutableMapOf(), ERRORTYPE, ERRORTYPE, ERRORTYPE.name)
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
        if(clCtx.paramList() != null){
            for( param in clCtx.paramList().param()){
                val paramName = param.NAME().text
                val paramType = translateType(param.type(), name)
                if(containsUnknown(paramType, classes))
                    log("Class $name has unknown type $paramType for field $paramName.",param)
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
            if (translateType(superMet.type(), className) != translateType(mtCtx.type(), className))
                log(
                    "Method $name is declared as overriding in $className, but a superclass has a different return type: ${
                        translateType(
                            superMet.type(),
                            className
                        )
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
                            val myType = translateType(mtCtx.paramList().param(i).type(), className)
                            val otherType = translateType(superMet.paramList().param(i).type(), className)
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
        if(containsUnknown(translateType(mtCtx.type(), className), classes))
            log("Method $className.$name has unknown return type ${mtCtx.type()}.", mtCtx.type())

        //Check parameters: no shadowing, types must be known
        if(mtCtx.paramList() != null){
            for(param in mtCtx.paramList().param()){
                val paramType = translateType(param.type(), className)
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
        val initVars = if(mtCtx.paramList() != null) mtCtx.paramList().param().map { Pair(it.NAME().text, translateType(it.type(), className)) }.toMap().toMutableMap() else mutableMapOf()
        val ret = checkStatement(mtCtx.statement(), false, initVars, translateType(mtCtx.type(), className), thisType, className)

        if(!ret) log("Method ${mtCtx.NAME().text} has a path without a final return statement.", mtCtx)
    }

    // This cannot be done with extension methods because they cannot override StatementContext.checkStatement()
    // TODO: move this to an antlr visitor
    private fun checkStatement(ctx : WhileParser.StatementContext,
                               finished : Boolean,
                               vars : MutableMap<String, Type>,
                               metType : Type, //return type
                               thisType : Type,
                               className: String) : Boolean{
        val inner : Map<String, Type> = getFields(className)

        when(ctx){
            is WhileParser.If_statementContext -> {
                val innerType = getType(ctx.expression(), inner, vars, thisType)
                if(innerType != ERRORTYPE && innerType != BOOLEANTYPE)
                    log("If statement expects a boolean in its guard, but parameter has type $innerType.",ctx)
                val left = checkStatement(ctx.statement(0), finished, vars, metType, thisType, className)
                val right = if(ctx.ELSE() != null) checkStatement(ctx.statement(1), finished, vars, metType, thisType, className) else true
                return if(ctx.next != null){
                    checkStatement(ctx.next,  (left && right) || finished, vars, metType, thisType, className)
                } else (left && right) || finished
            }
            is WhileParser.While_statementContext -> {
                val innerType = getType(ctx.expression(), inner, vars, thisType)
                if(innerType != ERRORTYPE && innerType != BOOLEANTYPE)
                    log("While statement expects a boolean in its guard, but parameter has type $innerType.",ctx)
                checkStatement(ctx.statement(0), finished, vars, metType, thisType, className)
                if(ctx.next != null)
                    return checkStatement(ctx.next,  finished, vars, metType, thisType, className)
            }
            is WhileParser.Sequence_statementContext -> {
                val first = checkStatement(ctx.statement(0), finished, vars, metType, thisType, className)
                return checkStatement(ctx.statement(1), first, vars, metType, thisType, className)
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
                            else vars[name] = translateType(ctx.type(), className)
                        }
                        translateType(ctx.type(), className)
                    } else getType(ctx.expression(0), inner, vars, thisType, false)
                val rhsType = getType(ctx.expression(1), inner, vars, thisType)
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
                                else vars[name] = translateType(ctx.type(), className)
                            }
                            translateType(ctx.type(), className)
                        }
                        ctx.target != null -> {
                            getType(ctx.expression(0), inner, vars, thisType)
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
                                getType(ctx.expression(i), inner, vars, thisType)                    //of call
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
                                else vars[name] = translateType(ctx.type(), className)
                            }
                            translateType(ctx.type(), className)
                        }
                        ctx.target != null -> {
                            getType(ctx.expression(0), inner, vars, thisType, false)
                        }
                        else -> {
                            null
                        }
                    }

                val calleeIndex = if(ctx.target == null) 0 else 1
                val rhsType = getType(ctx.expression(calleeIndex), inner, vars, thisType)
                if(rhsType.getPrimary() !is BaseType || !methods.containsKey(rhsType.getPrimary().getNameString())){
                    log("Call on type $rhsType not possible: type $rhsType is not a class.", ctx)
                } else {
                  val calledMet = ctx.NAME().text
                  if(!methods.getOrDefault(rhsType.getPrimary().getNameString(), listOf()).map { it.NAME().text }.contains(calledMet)){
                      log("Call on type $rhsType not possible: method $calledMet not found.", ctx)
                  } else {
                      val otherClassName = rhsType.getPrimary().getNameString()
                      val met = methods.getOrDefault(otherClassName, listOf()).first { it.NAME().text == calledMet }
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
                                  val realType = getType(ctx.expression(i), inner, vars, thisType)                    //of call
                                  val finalType = instantiateGenerics(targetType, rhsType, otherClassName, generics.getOrDefault(className, listOf()))
                                  if (targetType != ERRORTYPE && realType != ERRORTYPE && !realType.isAssignable(finalType, extends))
                                      log("Type $realType is not assignable to $targetType.", ctx)
                              }
                              if (lhsType != null) { //result type
                                  val metRet = translateType(met.type(), otherClassName) // type as declared
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
                            else vars[name] = translateType(ctx.type(), className)
                        }
                        translateType(ctx.type(), className)
                    } else getType(ctx.expression(0), inner, vars, thisType, false)
                val createClass = ctx.NAME().text
                val createDecl = recoverDef[createClass]
                var newType : Type = BaseType(createClass)

                if(ctx.namelist() != null)
                    newType = ComposedType(newType, ctx.namelist().NAME().map {
                        stringToType(it.text, className)
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
                        val realType = getType(ctx.expression(i), inner, vars, thisType)
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
                log("Type checking (C)SSA is not supported yet ", ctx, Severity.WARNING)
                if(ctx.declType != null){
                    val lhs = ctx.expression(0)
                    if(lhs !is WhileParser.Var_expressionContext){
                        log("Variable declaration must declare a variable.", ctx)
                    } else {
                        val name = lhs.NAME().text
                        if (vars.keys.contains(name)) log("Variable $name declared twice.", ctx)
                        else vars[name] = translateType(ctx.type(), className)
                    }
                }
            }
            is WhileParser.Owl_statementContext -> {
                log("Type checking (C)SSA is not supported yet ", ctx, Severity.WARNING)
                if(ctx.declType != null){
                    val lhs = ctx.expression(0)
                    if(lhs !is WhileParser.Var_expressionContext){
                        log("Variable declaration must declare a variable.", ctx)
                    } else {
                        val name = lhs.NAME().text
                        if (vars.keys.contains(name)) log("Variable $name declared twice.", ctx)
                        else vars[name] = translateType(ctx.type(), className)
                    }
                }
            }
            is WhileParser.Return_statementContext -> {
                val innerType = getType(ctx.expression(), inner, vars, thisType)
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
                                ctx.varInitList().varInit().map { Pair(it.NAME().text,getType(it.expression(), inner, vars, thisType)) }.toMap()
                                else
                                 emptyMap()

                    var ins = listOf<Pair<String,Type>>()
                    var outs = listOf<Pair<String,Type>>()

                    for(mVar in sim.modelDescription.modelVariables){
                        if(mVar.causality == "input" || mVar.causality == "state"){
                            if(!mVar.hasStartValue() && !inits.containsKey(mVar.name))
                                log("Simulation fails to initialize variable ${mVar.name}: no initial value given", ctx)
                            if(inits.containsKey(mVar.name)) {
                                if (mVar.typeName != "Integer" && mVar.typeName != "Boolean")
                                    log("Simulation fails to initialize variable ${mVar.name}: only Integer variables are supported",ctx)
                            }
                            if(mVar.causality == "input")
                                ins = ins + Pair(mVar.name, getSimType(mVar.typeName))
                        }
                        if((mVar.causality == "output" || mVar.initial == "calculated") && inits.containsKey(mVar.name)) {
                            log("Cannot initialize output or/and calculated variable ${mVar.name}",ctx)
                        }

                        if(mVar.causality == "input") ins = ins + Pair(mVar.name, getSimType(mVar.typeName))
                        if(mVar.causality == "output") outs = outs + Pair(mVar.name, getSimType(mVar.typeName))
                    }
                    val simType = SimulatorType(ins, outs)

                    if(ctx.type() != null) {
                        val declType = translateType(ctx.type(), className)
                        if(!declType.isAssignable(simType, extends))
                            log("Type $simType is not assignable to $declType", ctx)
                        if (ctx.target !is WhileParser.Var_expressionContext) {
                            log("Variable declaration must declare a variable.", ctx)
                        } else {
                            val name = ((ctx.target) as WhileParser.Var_expressionContext).NAME().text
                            if (vars.keys.contains(name)) log("Variable $name declared twice.", ctx)
                            else vars[name] = translateType(ctx.type(), className)
                        }
                    }else{
                        val declType = getType(ctx.target, inner, vars, thisType, false)
                        if(!declType.isAssignable(simType, extends))
                            log("Type $simType is not assignable to $declType", ctx)
                    }

                }
            }
            is WhileParser.Tick_statementContext -> {
                val fmuType = getType(ctx.fmu, inner, vars, thisType)
                val tickType = getType(ctx.time, inner, vars, thisType)
                if(fmuType !is SimulatorType)
                    log("Tick statement expects a FMU as first parameter, but got $fmuType }.",ctx)
                if(tickType != INTTYPE)
                    log("Tick statement expects an integer as second parameter, but got $tickType }.",ctx)
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
            "Real" -> ERRORTYPE
            "String" -> STRINGTYPE
            "Boolean" -> BOOLEANTYPE
            else -> ERRORTYPE
        }


    // This cannot be done with extension methods because they cannot override ExpressionContext.getType()
    private fun getType(eCtx : WhileParser.ExpressionContext,
                        fields : Map<String, Type>,
                        vars : Map<String, Type>,
                        thisType : Type,
                        read: Boolean = true
    ) : Type {
        when(eCtx){
            is WhileParser.Const_expressionContext -> return INTTYPE
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
                return fields.getOrDefault(name, ERRORTYPE)
            }
            is WhileParser.Nested_expressionContext -> {
                return getType(eCtx.expression(), fields, vars, thisType)
            }
            is WhileParser.Mult_expressionContext -> { // The following has a lot of code duplication and could be improved by changing the grammar
                val t1 = getType(eCtx.expression(0), fields, vars, thisType)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType)
                if((t1 == INTTYPE || t1 == ERRORTYPE) && (t2 == INTTYPE || t1 == ERRORTYPE)) return INTTYPE
                if(INTTYPE.isAssignable(t1, extends) && INTTYPE.isAssignable(t2, extends)) return INTTYPE
                log("Malformed multiplication with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.Plus_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType)
                if((t1 == INTTYPE || t1 == ERRORTYPE) && (t2 == INTTYPE || t1 == ERRORTYPE)) return INTTYPE
                if(INTTYPE.isAssignable(t1, extends) && INTTYPE.isAssignable(t2, extends)) return INTTYPE
                log("Malformed addition with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.Minus_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType)
                // do not throw an error twice
                if((t1 == INTTYPE || t1 == ERRORTYPE) && (t2 == INTTYPE || t1 == ERRORTYPE)) return INTTYPE
                if(INTTYPE.isAssignable(t1, extends) && INTTYPE.isAssignable(t2, extends)) return INTTYPE
                log("Malformed subtraction with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.Neq_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType)
                if((t1 == ERRORTYPE && t2 == ERRORTYPE) || (t2 == t1)) return BOOLEANTYPE
                if(t1.isAssignable(t2, extends) || t2.isAssignable(t1, extends)) return BOOLEANTYPE
                log("Malformed comparison <> with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.Eq_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType)
                if((t1 == ERRORTYPE && t2 == ERRORTYPE) || (t2 == t1)) return BOOLEANTYPE
                if(t1.isAssignable(t2, extends) || t2.isAssignable(t1, extends)) return BOOLEANTYPE
                log("Malformed comparison = with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.And_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType)
                if((t1 == ERRORTYPE && t2 == ERRORTYPE) || (t2 == BOOLEANTYPE && t1 == BOOLEANTYPE)) return BOOLEANTYPE
                if(t1.isAssignable(t2, extends) || t2.isAssignable(t1, extends)) return BOOLEANTYPE
                log("Malformed comparison && with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.Or_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType)
                if((t1 == ERRORTYPE && t2 == ERRORTYPE) || (t2 == BOOLEANTYPE && t1 == BOOLEANTYPE)) return BOOLEANTYPE
                if(t1.isAssignable(t2, extends) || t2.isAssignable(t1, extends)) return BOOLEANTYPE
                log("Malformed comparison || with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.Not_expressionContext -> {
                val t1 = getType(eCtx.expression(), fields, vars, thisType)
                if(t1 == ERRORTYPE || t1 == BOOLEANTYPE) return BOOLEANTYPE
                log("Malformed negation ! with subtypes $t1", eCtx)
                return ERRORTYPE
            }
            is WhileParser.Leq_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType)
                if((t1 == INTTYPE || t1 == ERRORTYPE) && (t2 == INTTYPE || t1 == ERRORTYPE)) return BOOLEANTYPE
                if(INTTYPE.isAssignable(t1, extends) && INTTYPE.isAssignable(t2, extends)) return BOOLEANTYPE
                log("Malformed comparison <= with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.Geq_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType)
                if((t1 == INTTYPE || t1 == ERRORTYPE) && (t2 == INTTYPE || t1 == ERRORTYPE)) return BOOLEANTYPE
                if(INTTYPE.isAssignable(t1, extends) && INTTYPE.isAssignable(t2, extends)) return BOOLEANTYPE
                log("Malformed comparison >= with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.Lt_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType)
                if((t1 == INTTYPE || t1 == ERRORTYPE) && (t2 == INTTYPE || t1 == ERRORTYPE)) return BOOLEANTYPE
                if(INTTYPE.isAssignable(t1, extends) && INTTYPE.isAssignable(t2, extends)) return BOOLEANTYPE
                log("Malformed comparison <= with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.Gt_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType)
                val t2 = getType(eCtx.expression(1), fields, vars, thisType)
                if((t1 == INTTYPE || t1 == ERRORTYPE) && (t2 == INTTYPE || t1 == ERRORTYPE)) return BOOLEANTYPE
                if(INTTYPE.isAssignable(t1, extends) && INTTYPE.isAssignable(t2, extends)) return BOOLEANTYPE
                log("Malformed comparison >= with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.This_expressionContext -> return thisType
            is WhileParser.External_field_expressionContext -> { // This must resolve the generics
                val t1 = getType(eCtx.expression(), fields, vars, thisType)
                if(t1 == ERRORTYPE) return ERRORTYPE
                if(t1 is SimulatorType){
                    return if(read){
                        val inVar = t1.outVar.firstOrNull { it.first == eCtx.NAME().text }
                        inVar?.second ?: ERRORTYPE
                    } else {
                        val inVar = t1.inVar.firstOrNull { it.first == eCtx.NAME().text }
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
                }
                val fieldType = this.fields.getOrDefault(primary.getNameString(), mutableMapOf()).getOrDefault(eCtx.NAME().text,
                    ERRORTYPE
                )
                return instantiateGenerics(fieldType, t1, primName, generics.getOrDefault(thisType.getPrimary().getNameString(), listOf()))
            }
            else -> {
                log("Expression $eCtx cannot be type checked.",eCtx)
                return ERRORTYPE
            }
        }
    }



    /**********************************************************************
    Helper to handle types
     ***********************************************************************/
    private fun getParameterTypes(met: WhileParser.Method_defContext, otherClassName: String): List<Type> =
        if(met.paramList() == null) listOf() else met.paramList().param().map { translateType(it.type(), otherClassName) }


    private fun getParameterTypes(className: String): List<Type> {
        val types : List<Type> = fields.getOrDefault(className, mapOf()).map { it.value }
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
    private fun containsUnknown(type: Type, types: Set<String>): Boolean {
        if(type is GenericType) return false // If we can translate it into a generic type, we already checked
        if(type is BaseType) return !types.contains(type.name)
        val composType = type as ComposedType
        return composType.params.fold( containsUnknown(composType.name, types), {acc,nx -> acc || containsUnknown(nx, types)})
    }

    private fun getFields(className: String): Map<String, Type> {
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