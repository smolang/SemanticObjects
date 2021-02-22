package microobject.type

//Internal Type Structure
abstract class Type {
    abstract fun isFullyConcrete() : Boolean
    abstract fun getPrimary() : SimpleType
    abstract fun containsUnknown(types: Set<String>): Boolean
    fun isAssignable(rhs : Type, extends : MutableMap<String, String>) : Boolean { //TODO: finish refactoring
        if (this == ERRORTYPE || rhs == ERRORTYPE) return false   //errors are not assignable
        if (rhs == NULLTYPE) return true
        if (this == rhs) return true  //no need for complex typing
        if (this is BaseType && rhs is BaseType) {
            return rhs.isBelow(this, extends)
        } else if (this is GenericType && rhs is GenericType) {
            return this == rhs
        } else if (this is SimulatorType && rhs is SimulatorType) {
            return rhs.inVar.containsAll(this.inVar) && rhs.outVar.containsAll(this.outVar)
        } else if (this.javaClass != rhs.javaClass) {
            return false
        }  else { //if (lhs is ComposedType && rhs is ComposedType)
            val cLhs = this as ComposedType
            val cRhs = rhs as ComposedType
            if(cLhs.params.size != cRhs.params.size) return false

            var ret = cLhs.name.isAssignable(cRhs.name, extends)
            for( i in cLhs.params.indices){
                if(!ret) break
                ret = ret && cLhs.params[i].isAssignable(cRhs.params[i], extends)
            }
            return ret
        }
    }
    fun isBelow(t2: Type, extends : MutableMap<String, String>) : Boolean {
        if(this == t2) return true
        if(this == NULLTYPE) return true
        if(this is GenericType || this is ComposedType) return false
        if(extends.containsKey(this.getPrimary().getNameString())) return BaseType(extends.getOrDefault(this.getPrimary().getNameString(), "Object")).isBelow(t2, extends)
        return false
    }
}

abstract class SimpleType : Type() {
    abstract fun getNameString() : String
    override fun getPrimary(): SimpleType = this
}

data class SimulatorType(val inVar : List<Pair<String, Type>>, val outVar : List<Pair<String, Type>>) : SimpleType() {
    override fun toString() : String  = "DE[ ${inVar.joinToString(",") { it.first + ":" + it.second }}; ${outVar.joinToString(",") { it.first + ":" + it.second }}]"
    override fun getNameString() : String  = "DE"
    override fun isFullyConcrete() : Boolean =
        inVar.map { it.second }.any { it.isFullyConcrete() } && outVar.map { it.second }.any { it.isFullyConcrete() }
    override fun containsUnknown(types: Set<String>): Boolean =
        inVar.map { it.second }.any { it.containsUnknown(types) } || outVar.map { it.second }.any { it.containsUnknown(types) }
}

data class GenericType(val name : String) : SimpleType() {
    override fun getNameString() : String  = name
    override fun toString() : String  = name
    override fun isFullyConcrete() : Boolean = false
    override fun containsUnknown(types: Set<String>): Boolean = !types.contains(name)
}

data class BaseType(val name : String) : SimpleType(){
    override fun getNameString() : String  = name
    override fun isFullyConcrete() : Boolean = true
    override fun toString() : String  = name
    override fun containsUnknown(types: Set<String>): Boolean = false //contract
}

data class ComposedType(val name : Type, val params : List<Type>) : Type() {
    override fun getPrimary() : SimpleType {
        if(name is ComposedType) return name.getPrimary()
        return name as SimpleType
    }
    override fun toString() : String  = name.toString() + "<" + params.joinToString { it.toString() }+">"
    override fun isFullyConcrete() : Boolean = params.fold(name.isFullyConcrete(), { it,nx -> it && nx.isFullyConcrete()})
    override fun containsUnknown(types: Set<String>): Boolean = params.fold( name.containsUnknown(types), {acc,nx -> acc || nx.containsUnknown(types)})
}

val INTTYPE = BaseType("Int")
val BOOLEANTYPE = BaseType("Boolean")
val STRINGTYPE = BaseType("String")
val OBJECTTYPE = BaseType("Object")
val NULLTYPE = BaseType("Null")
val ERRORTYPE = BaseType("ERROR")
val UNITTYPE = BaseType("Unit")