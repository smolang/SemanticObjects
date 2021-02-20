package microobject.data

//Internal Type Structure
interface Type {
    fun isFullyConcrete() : Boolean
    fun getPrimary() : SimpleType
    fun containsUnknown(types: Set<String>): Boolean
}

abstract class SimpleType : Type {
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

data class ComposedType(val name : Type, val params : List<Type>) : Type {
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