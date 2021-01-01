package microobject.data

import antlr.microobject.gen.WhileParser
import org.antlr.v4.runtime.ParserRuleContext
import java.lang.Exception
import javax.lang.model.type.NullType

//Error Messages
enum class Severity { HINT, WARNING, ERROR }
data class TypeError(val msg: String, val line: Int, val severity: Severity)



//Internal Type Structure
abstract class Type {
    abstract fun isFullyConcrete() : Boolean
    abstract fun getPrimary() : SimpleType
    abstract fun isAtomic() : Boolean
}
abstract class SimpleType : Type(){
    abstract fun getNameString() : String
    override fun getPrimary(): SimpleType = this
}
data class GenericType(val name : String) : SimpleType() {
    override fun getNameString() : String  = name
    override fun toString() : String  = name
    override fun isFullyConcrete() : Boolean = false
    override fun isAtomic() : Boolean = true
}
data class BaseType(val name : String, val atomic : Boolean = false) : SimpleType(){
    override fun getNameString() : String  = name
    override fun isFullyConcrete() : Boolean = true
    override fun toString() : String  = name
    override fun isAtomic() : Boolean = atomic
}
data class ComposedType(val name : Type, val params : List<Type>) : Type(){
    override fun getPrimary() : SimpleType {
        if(name is ComposedType) return name.getPrimary()
        return name as SimpleType
    }
    override fun toString() : String  = name.toString() + "<" + params.joinToString { it.toString() }+">"
    override fun isFullyConcrete() : Boolean = params.fold(name.isFullyConcrete(), { it,nx -> it && nx.isFullyConcrete()})
    override fun isAtomic() : Boolean = false
}

val INTTYPE = BaseType("Int", true)
val BOOLEANTYPE = BaseType("Boolean", true)
val STRINGTYPE = BaseType("String", true)
val OBJECTTYPE = BaseType("Object")
val NULLTYPE = BaseType("Null")
val ERRORTYPE = BaseType("ERROR", true)


class TypeChecker(private val ctx: WhileParser.ProgramContext) {
    private val builtins : Set<String> = setOf("Int", "Boolean", "Unit", "String", "Object")
    private val extends : MutableMap<String, String> = mutableMapOf(Pair("Int", "Object"), Pair("Boolean", "Object"), Pair("Unit", "Object"), Pair("String", "Object"))
    private val generics : MutableMap<String, List<String>> = mutableMapOf()
    private val fields : MutableMap<String, Map<String,Type>> = mutableMapOf()
    private val parameters : MutableMap<String, List<String>> = mutableMapOf()
    private val methods : MutableMap<String, List<WhileParser.Method_defContext>> = mutableMapOf()
    private val classes : MutableSet<String> = mutableSetOf("Int", "Boolean", "Unit", "String", "Object")
    private var error : List<TypeError> = listOf()

    fun report(silent : Boolean = false) : Boolean {
        var ret = false
        for( e in error ){
            if(e.severity == Severity.ERROR) ret = true
            if(!silent) println("Line ${e.line}, ${e.severity}: ${e.msg}")
        }
        return ret
    }
    private fun log(msg: String, node : ParserRuleContext?, severity: Severity = Severity.ERROR){
        error = error + TypeError(msg, node?.getStart()?.line ?: 0, severity)
    }



    private fun stringToType(text: String, createClass: String) : Type{
        return when {
            generics.getOrDefault(createClass, listOf()).contains(text) -> GenericType(text)
            text == INTTYPE.name -> INTTYPE
            text == BOOLEANTYPE.name -> BOOLEANTYPE
            text == STRINGTYPE.name -> STRINGTYPE
            else -> BaseType(text)
        }
    }


    private fun translateType(tCtx : WhileParser.TypeContext, className : String) : Type { //= stringToType(tCtx.text, className)
        return when(tCtx){
            is WhileParser.Simple_typeContext -> stringToType(tCtx.text, className) //if(generics.contains(tCtx.text)) GenericType(tCtx.text) else BaseType(tCtx.text)
            is WhileParser.Nested_typeContext -> {
                val lead = stringToType(tCtx.NAME().text, className) //if(generics.contains(tCtx.text)) GenericType(tCtx.text) else BaseType(tCtx.text)
                ComposedType(lead, tCtx.typelist().type().map { translateType(it, className) })
            }
            else -> throw Exception("Unknown type context: $tCtx") // making the type checker happy
        }
    }

    private fun collect(){
        //two passes for correct translation of types
        for(clCtx in ctx.class_def()){
            val name = clCtx.NAME(0).text
            classes.add(name)
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
    fun check() {
        collect()
        for (clCtx in ctx.class_def()) {
            val name = clCtx.NAME(0).text

            //Check extends: class must exist
            if (clCtx.NAME(1) != null) {
                val extName = clCtx.NAME(1).text
                if (!classes.contains(extName)) log("Class $name extends unknown class $extName. Do not explicitly extend Object.", clCtx)
            }


            //Check generics: no shadowing
            if(clCtx.namelist() != null){
                val collisions = clCtx.namelist().NAME().filter { classes.contains(it.text) || builtins.contains(it.text) }
                for(col in collisions){
                    log("Class $name has type parameter ${col.text} that shadows a known type.", clCtx)
                }
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
            if(clCtx.method_def() != null){
                for( mtCtx in clCtx.method_def()){
                    checkMet(mtCtx, name)
                }
            }
        }
    }

    private fun containsUnknown(paramType: Type, types: Set<String>): Boolean {
        if(paramType is GenericType) return false // If we can translate it into a generic type, we already checked
        if(paramType is BaseType) return !types.contains(paramType.name)
        val composType = paramType as ComposedType
        return composType.params.fold( containsUnknown(composType.name, types), {acc,nx -> acc && containsUnknown(nx, types)})
    }

    private fun checkMet(mtCtx: WhileParser.Method_defContext, className: String) {
        val name = mtCtx.NAME().text
        val gens = generics.getOrDefault(className, listOf()).map { GenericType(it) }
        val thisType = if(gens.isNotEmpty()) ComposedType(BaseType(className), gens) else BaseType(className)

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

        val initVars = if(mtCtx.paramList() != null) mtCtx.paramList().param().map { Pair(it.NAME().text, translateType(it.type(), className)) }.toMap().toMutableMap() else mutableMapOf()
        //Check statement
        val ret = checkStatement(mtCtx.statement(), false, initVars, translateType(mtCtx.type(), className), thisType, className)
        if(!ret)
            log("Method ${mtCtx.NAME().text} has a path without a final return statement.", mtCtx)

    }

    private fun checkStatement(ctx : WhileParser.StatementContext,
                               finished : Boolean,
                               vars : MutableMap<String, Type>,
                               metType : Type,
                               thisType : Type,
                               className: String) : Boolean{
        val inner = fields.getOrDefault(className, mapOf())
        when(ctx){
            is WhileParser.If_statementContext -> {
                val innerType = getType(ctx.expression(), inner, vars, thisType)
                if(innerType != ERRORTYPE && innerType != BOOLEANTYPE)
                    log("If statement expects a boolean in its guard, but parameter has type $innerType.",ctx)
                val left = checkStatement(ctx.statement(0), finished, vars, metType, thisType, className)
                val right = checkStatement(ctx.statement(1), finished, vars, metType, thisType, className)
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
                        if(lhs !is WhileParser.Var_expressionContext)
                            log("Variable declaration must declare a variable.", ctx)
                        val name = (lhs as WhileParser.Var_expressionContext).NAME().text
                        if(vars.keys.contains(name)) log("Variable $name declared twice.", ctx)
                        else                         vars[name] = translateType(ctx.type(), className)
                        translateType(ctx.type(), className)
                    } else getType(ctx.expression(0), inner, vars, thisType)
                val rhsType = getType(ctx.expression(1), inner, vars, thisType)
                if(lhsType != ERRORTYPE && rhsType != ERRORTYPE && !isAssignable(lhsType, rhsType))
                    log("Type $rhsType is not assignable to $lhsType", ctx)
            }
            is WhileParser.Call_statementContext -> {
                val lhsType =
                    when {
                        ctx.type() != null -> {
                            val lhs = ctx.expression(0)
                            if(lhs !is WhileParser.Var_expressionContext)
                                log("Variable declaration must declare a variable.", ctx)
                            val name = (lhs as WhileParser.Var_expressionContext).NAME().text
                            if(vars.keys.contains(name)) log("Variable $name declared twice.", ctx)
                            else                         vars[name] = translateType(ctx.type(), className)
                            translateType(ctx.type(), className)
                        }
                        ctx.target != null -> {
                            getType(ctx.expression(0), inner, vars, thisType)
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
                      val met = methods.getOrDefault(rhsType.getPrimary().getNameString(), listOf()).first { it.NAME().text == calledMet }
                      if(met.paramList() != null) {
                          if (ctx.expression().size - 1 - calleeIndex != met.paramList().param().size) {
                              log(
                                  "Mismatching number of parameter when calling $rhsType.$calledMet. Expected ${
                                      met.paramList().depth()
                                  }, got ${ctx.expression().size - 1 - calleeIndex}", ctx
                              )
                          } else {
                              for (i in calleeIndex + 1 until ctx.expression().size) {
                                  val match = i - calleeIndex - 1
                                  val targetType = translateType(met.paramList().param(match).type(), className)
                                  val realType = getType(ctx.expression(i), inner, vars, thisType)
                                  if (targetType != ERRORTYPE && realType != ERRORTYPE && !isAssignable(
                                          targetType,
                                          realType
                                      )
                                  )
                                      log("Type $realType is not assignable to $targetType", ctx)
                              }
                              if (lhsType != null) {
                                  val metRet = translateType(met.type(), className)
                                  if (!isAssignable(lhsType, metRet))
                                      log("Type $metRet is not assignable to $lhsType", ctx)
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
                        if (lhs !is WhileParser.Var_expressionContext)
                            log("Variable declaration must declare a variable.", ctx)
                        val name = (lhs as WhileParser.Var_expressionContext).NAME().text
                        if (vars.keys.contains(name)) log("Variable $name declared twice.", ctx)
                        else vars[name] = translateType(ctx.type(), className)
                        translateType(ctx.type(), className)
                    } else getType(ctx.expression(0), inner, vars, thisType)
                val createClass = ctx.NAME().text
                if (parameters.getOrDefault(createClass, listOf()).size == (ctx.expression().size - 1)){
                    for(i in 1 until ctx.expression().size){
                        val currentTarget = parameters.getOrDefault(createClass, listOf())
                        val targetType = fields.getOrDefault(createClass, mapOf()).getOrDefault(currentTarget, ERRORTYPE)
                        val realType = getType(ctx.expression(i), inner, vars, thisType)
                        if(targetType != ERRORTYPE && realType != ERRORTYPE && !isAssignable(targetType, realType))
                            log("Type $targetType is not assignable to $realType", ctx)
                    }
                } else log("Mismatching number of parameter when creating an $createClass instance. Expected ${ctx.expression().size-1}, got ${fields.getOrDefault(createClass, mapOf()).size}", ctx)

                var newType : Type = BaseType(createClass)

                if(ctx.namelist() != null)
                    newType = ComposedType(newType, ctx.namelist().NAME().map {
                        stringToType(it.text, createClass)
                    })

                if(lhsType != ERRORTYPE && !isAssignable(newType, lhsType) )
                    log("Type $createClass is not assignable to $lhsType", ctx)

            }
            is WhileParser.Sparql_statementContext -> {
                log("Type checking (C)SSA is not supported yet ", ctx, Severity.WARNING)
            }
            is WhileParser.Owl_statementContext -> {
                log("Type checking (C)SSA is not supported yet ", ctx, Severity.WARNING)
            }
            is WhileParser.Return_statementContext -> {
                val innerType = getType(ctx.expression(), inner, vars, thisType)
                if(innerType != ERRORTYPE && innerType != metType)
                    log("Type $innerType of return statement does not match method type ${metType}.",ctx)
                return true
            }
            is WhileParser.Output_statementContext -> {
                val innerType = getType(ctx.expression(), inner, vars, thisType)
                if(innerType != ERRORTYPE && innerType != STRINGTYPE)
                    log("Println statement expects a string, but parameter has type $innerType.", ctx)
            }
            is WhileParser.Skip_statmentContext -> { }
            is WhileParser.Debug_statementContext -> { }
            else -> {
                log("Statement $ctx cannot be type checked}",ctx)
            }
        }
        return false
    }


    private fun getType(eCtx : WhileParser.ExpressionContext,
                        fields : Map<String, Type>,
                        vars : Map<String, Type>,
                        thisType : Type) : Type{
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
                if(!fields.containsKey(name)) log("Field $name is not declared for $thisType.", eCtx)
                return fields.getOrDefault(name, ERRORTYPE)
            }
            is WhileParser.Nested_expressionContext -> {
                return getType(eCtx.expression(), fields, vars, thisType)
            }
            is WhileParser.Mult_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType)
                val t2 = getType(eCtx.expression(0), fields, vars, thisType)
                // do not throw an error twice
                if((t1 == INTTYPE || t1 == ERRORTYPE) && (t2 == INTTYPE|| t1 == ERRORTYPE)) return INTTYPE
                log("Malformed multiplication with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.Plus_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType)
                val t2 = getType(eCtx.expression(0), fields, vars, thisType)
                // do not throw an error twice
                if((t1 == INTTYPE || t1 == ERRORTYPE) && (t2 == INTTYPE|| t1 == ERRORTYPE)) return INTTYPE
                log("Malformed Addition with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.Minus_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType)
                val t2 = getType(eCtx.expression(0), fields, vars, thisType)
                // do not throw an error twice
                if((t1 == INTTYPE || t1 == ERRORTYPE) && (t2 == INTTYPE|| t1 == ERRORTYPE)) return INTTYPE
                log("Malformed subtraction with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.Neq_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType)
                val t2 = getType(eCtx.expression(0), fields, vars, thisType)
                // do not throw an error twice
                if((t1 == ERRORTYPE && t2 == ERRORTYPE) || (t2 == t1)) return BOOLEANTYPE
                log("Malformed comparison with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.Eq_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType)
                val t2 = getType(eCtx.expression(0), fields, vars, thisType)
                // do not throw an error twice
                if((t1 == ERRORTYPE && t2 == ERRORTYPE) || (t2 == t1)) return BOOLEANTYPE
                log("Malformed comparison with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.Leq_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType)
                val t2 = getType(eCtx.expression(0), fields, vars, thisType)
                // do not throw an error twice
                if((t1 == INTTYPE || t1 == ERRORTYPE) && (t2 == INTTYPE|| t1 == ERRORTYPE)) return BOOLEANTYPE
                log("Malformed comparison with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.Geq_expressionContext -> {
                val t1 = getType(eCtx.expression(0), fields, vars, thisType)
                val t2 = getType(eCtx.expression(0), fields, vars, thisType)
                // do not throw an error twice
                if((t1 == INTTYPE || t1 == ERRORTYPE) && (t2 == INTTYPE|| t1 == ERRORTYPE)) return BOOLEANTYPE
                log("Malformed comparison with subtypes $t1 and $t2", eCtx)
                return ERRORTYPE
            }
            is WhileParser.This_expressionContext -> return thisType
            is WhileParser.External_field_expressionContext -> {
                val t1 = getType(eCtx.expression(), fields, vars, thisType)
                if(t1 == ERRORTYPE) return ERRORTYPE
                if(t1 is GenericType) {
                    log("Access of fields of generic types is not supported.", eCtx)
                    return ERRORTYPE
                }
                val primary = t1.getPrimary()
                //if(t1 is ComposedType) {
                    if(!this.fields.containsKey(primary.getNameString())){
                        log("Cannot access fields of $primary.", eCtx)
                        return ERRORTYPE
                    }
                    if(!this.fields.getOrDefault(primary.getNameString(), mutableMapOf()).containsKey(eCtx.NAME().text)){
                        log("Field ${eCtx.NAME().text} is not declared for $primary.", eCtx)
                        return ERRORTYPE
                    }
                    return this.fields.getOrDefault(primary.getNameString(), mutableMapOf()).getOrDefault(eCtx.NAME().text, ERRORTYPE)
                //}
                //return ERRORTYPE
            }
            else -> {
                log("Expression $eCtx cannot be type checked.",eCtx)
                return ERRORTYPE
            }
        }
    }

    private fun isAssignable(lhs: Type, rhs : Type) : Boolean {
        if(lhs == ERRORTYPE || rhs == ERRORTYPE) return false   //errors are not assignable
        if(lhs.isAtomic() && rhs.isAtomic()) return lhs == rhs  //no need for complex typing if both types are atomic
        if((lhs.isAtomic() && !rhs.isAtomic()) || (!lhs.isAtomic() && rhs.isAtomic())) return false    //atomic and non-atomic types do not mix
        if(rhs == NULLTYPE) return true
        if(lhs is BaseType && rhs is BaseType){
            return isBelow(rhs, lhs)
        } else if (lhs is BaseType && rhs is ComposedType) {
            return isBelow(rhs.getPrimary(), lhs)
        } else if (lhs is ComposedType && rhs is BaseType) {
            return false
        } else { //if (lhs is ComposedType && rhs is ComposedType)
            val cLhs = lhs as ComposedType
            val cRhs = rhs as ComposedType
            if(cLhs.params.size != cRhs.params.size) return false

            var ret = isAssignable(cLhs.name, cRhs.name)
            for( i in cLhs.params.indices){
                if(!ret) break
                ret = ret && isAssignable(cLhs.params[i], cRhs.params[i])
            }
            return ret
        }
    }

    private fun isBelow(rhs: Type, lhs: Type): Boolean {
        if(rhs == lhs) return true
        if(rhs == NULLTYPE) return true
        if(rhs is GenericType || rhs is ComposedType || !rhs.isAtomic()) return false
        if(extends.containsKey(rhs)) return isBelow(BaseType(extends.getOrDefault(rhs, "Object")), lhs)
        return false
    }
}