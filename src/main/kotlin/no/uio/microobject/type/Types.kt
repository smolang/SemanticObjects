package no.uio.microobject.type

//Internal Type Structure
abstract class Type {
    companion object{
        fun isAtomic(type : Type) : Boolean{
            return type == INTTYPE || type == BOOLEANTYPE || type == STRINGTYPE || type == DOUBLETYPE
        }
    }
    abstract fun isFullyConcrete() : Boolean
    abstract fun getPrimary() : SimpleType
    abstract fun containsUnknown(types: Set<String>): Boolean
    open fun isAssignable(rhs : Type, extends : MutableMap<String, Type>) : Boolean {
        if (this == ERRORTYPE || rhs == ERRORTYPE) return false   //errors are not assignable
        if (rhs == NULLTYPE) return true
        if (this == rhs) return true  //no need for complex typing
        return false
    }
    fun isBelow(t2: Type, extends : MutableMap<String, Type>) : Boolean {
        if(this == t2) return true
        if(this == NULLTYPE) return true
        if(this is GenericType || this is ComposedType) return false
        if(extends.containsKey(this.getPrimary().getNameString())) return extends.getOrDefault(this.getPrimary().getNameString(), OBJECTTYPE).isBelow(t2, extends)
        return false
    }
}

abstract class SimpleType : Type() {
    abstract fun getNameString() : String
    override fun getPrimary(): SimpleType = this
}

data class SimulatorType(val inVar : List<Pair<String, Type>>, val outVar : List<Pair<String, Type>>) : SimpleType() {
    override fun toString() : String  = "Cont[ ${inVar.joinToString(",") { "In " + it.first + ":" + it.second }}, ${outVar.joinToString(",") { "Out " + it.first + ":" + it.second }}]"
    override fun getNameString() : String  = "Cont"
    override fun isFullyConcrete() : Boolean =
        inVar.map { it.second }.any { it.isFullyConcrete() } && outVar.map { it.second }.any { it.isFullyConcrete() }
    override fun containsUnknown(types: Set<String>): Boolean =
        inVar.map { it.second }.any { it.containsUnknown(types) } || outVar.map { it.second }.any { it.containsUnknown(types) }
    override fun isAssignable(rhs : Type, extends : MutableMap<String, Type>) : Boolean  =
        super.isAssignable(rhs, extends) || (rhs is SimulatorType && rhs.inVar.containsAll(this.inVar) && rhs.outVar.containsAll(this.outVar))
}

data class GenericType(val name : String) : SimpleType() {
    override fun getNameString(): String = name
    override fun toString(): String = name
    override fun isFullyConcrete(): Boolean = false
    override fun containsUnknown(types: Set<String>): Boolean = !types.contains(name)
    override fun isAssignable(rhs: Type, extends: MutableMap<String, Type>): Boolean = super.isAssignable(rhs, extends)
}
data class BaseType(val name : String) : SimpleType(){
    override fun getNameString() : String  = name
    override fun isFullyConcrete() : Boolean = true
    override fun toString() : String  = name
    override fun containsUnknown(types: Set<String>): Boolean = false //contract
    override fun isAssignable(rhs : Type, extends : MutableMap<String, Type>) : Boolean =
        super.isAssignable(rhs, extends) || (rhs is BaseType && rhs.isBelow(this, extends))
}

data class ComposedType(val name : Type, val params : List<Type>) : Type() {
    override fun getPrimary() : SimpleType {
        if(name is ComposedType) return name.getPrimary()
        return name as SimpleType
    }
    override fun toString() : String  = name.toString() + "<" + params.joinToString { it.toString() }+">"
    override fun isFullyConcrete() : Boolean = params.fold(name.isFullyConcrete()) { it, nx -> it && nx.isFullyConcrete() }
    override fun containsUnknown(types: Set<String>): Boolean = params.fold( name.containsUnknown(types)) { acc, nx ->
        acc || nx.containsUnknown(
            types
        )
    }

    override fun isAssignable(rhs : Type, extends : MutableMap<String, Type>) : Boolean {
        if(super.isAssignable(rhs, extends)) return true
        if(rhs !is ComposedType) {
            val rhsSuper = extends[rhs.getPrimary().getNameString()]
            return if (rhsSuper == null) false
            else {
                this.isAssignable(rhsSuper, extends)
            }
        }
        if (this.params.size != rhs.params.size) return false

        var ret = this.name.isAssignable(rhs.name, extends)
        for (i in this.params.indices) {
            if (!ret) break
            ret = ret && this.params[i].isAssignable(rhs.params[i], extends)
        }
        return ret
    }
}


val INTTYPE = BaseType("Int")
val BOOLEANTYPE = BaseType("Boolean")
val STRINGTYPE = BaseType("String")
val DOUBLETYPE = BaseType("Double")
val OBJECTTYPE = BaseType("Object")
val NULLTYPE = BaseType("Null")
val ERRORTYPE = BaseType("ERROR")
val UNITTYPE = BaseType("Unit")
