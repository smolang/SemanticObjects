package no.uio.microobject.runtime

import no.uio.microobject.data.Expression
import no.uio.microobject.data.LiteralExpr
import no.uio.microobject.data.Statement
import no.uio.microobject.data.TRUEEXPR
import no.uio.microobject.main.Settings
import no.uio.microobject.type.*
import java.util.*

//This will be used for snapshots
class State(initStack  : Stack<StackEntry>, initHeap: GlobalMemory, simMemory: SimulationMemory, initInfo : StaticTable, private val settings: Settings, private val interpreter: Interpreter? = null) {
    private val stack: Stack<StackEntry> = initStack.clone() as Stack<StackEntry>
    private val heap: GlobalMemory = initHeap.toMutableMap()
    private val staticInfo: StaticTable = initInfo.copy()
    private val simulation : SimulationMemory = simMemory.toMap().toMutableMap()

    companion object{
        val HEADER =
        """
        @prefix owl: <http://www.w3.org/2002/07/owl#> .
        @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
        @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
        @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
        """.trimIndent()

        val VOCAB = this::class.java.classLoader.getResource("vocab.owl").readText()

        val MINIMAL =
        """                     
        smol:null rdf:type owl:NamedIndividual , smol:Object .
        prog:_Entry_ rdf:type owl:NamedIndividual , smol:Class .
        
        """.trimIndent()
    }

    private fun funVal(obj:LiteralExpr, store:String, target:LiteralExpr, prefix:String, overrideStr : String? = null) : String {
        var res = ""
        res = if(overrideStr == null) "run:${obj.literal} $prefix:${obj.tag}_$store "
        else "$overrideStr $prefix:${obj.tag}_$store "
        res += getTTLLiteral(target) + ".\n"
        return res
    }

    private fun getTTLLiteral(target:LiteralExpr) = if (target.literal == "null")
        "smol:${target.literal}"
    else if (target.tag == ERRORTYPE || target.tag == STRINGTYPE)
        target.literal
    else if (target.tag == INTTYPE)
        "\"${target.literal}\"^^xsd:integer"
    else
        "run:${target.literal}"

    fun dump(forceRule : Boolean = false) : String{
        //Builds always known information and meta data
        var res = settings.prefixes() + "\n"+HEADER + "\n" + VOCAB  + "\n" + MINIMAL


        res += staticInfo.dumpClasses()

        //dumps individuals
        for(obj in heap.keys){
            res += "run:${obj.literal} a prog:${(obj.tag as BaseType).name}.\n"
            res += "run:${obj.literal} rdf:type owl:NamedIndividual , smol:Object.\n"

            var useDefaultModels = true
            if(staticInfo.modelsTable[obj.tag.name] != null && staticInfo.modelsTable[obj.tag.name]!!.isNotEmpty()){
                for(mEntry in staticInfo.modelsTable[obj.tag.name]!!){
                    if(interpreter != null) {
                        val ret = interpreter.evalClassLevel(mEntry.first, obj)
                        if(ret == TRUEEXPR){
                            useDefaultModels = false
                            val target = heap[obj]!!.getOrDefault("__models", LiteralExpr("ERROR")).literal.removeSurrounding("\"")
                            val descr = mEntry.second.removeSurrounding("\"")
                            res += "$target $descr\n"
                            break
                        }
                    }
                }
            }


            //and their fields
            for(store in heap[obj]!!.keys) {
                if (store == "__models") {
                    val target = heap[obj]!!.getOrDefault(store, LiteralExpr("ERROR")).literal.removeSurrounding("\"")
                    res += "run:${obj.literal} domain:models $target.\n"
                } else if (store == "__describe") {
                    if(!useDefaultModels) continue
                    val target = heap[obj]!!.getOrDefault(store, LiteralExpr("ERROR")).literal + "\n"
                    res += target
 //               } else if(store.contains("dinoStatus") && staticInfo.fieldTable[obj.tag.name]!!.any { it.name == store && it.inferenceVisibility == Visibility.PUBLIC }){
                } else if(staticInfo.fieldTable[obj.tag.name]!!.any { it.name == store && it.inferenceVisibility == Visibility.PUBLIC }){
                    val target = heap[obj]!!.getOrDefault(store, LiteralExpr("ERROR"))
                    res += funVal(obj, store, target, "prog")

                    if(staticInfo.fieldTable[obj.tag.name]!!.any { it.name == store && it.isDomain } && heap[obj]!!.containsKey("__models") ){
                        res += funVal(obj, store, target, "domain",heap[obj]!!.getOrDefault("__models", LiteralExpr("ERROR")).literal.removeSurrounding("\""))
                    }
                }
            }

            if(!settings.useRule) {
                if (staticInfo.methodTable[obj.tag.name] != null) {
                    for (m in staticInfo.methodTable[obj.tag.name]!!.entries) {
                        var retVal : Pair<LiteralExpr, LiteralExpr>? = null
                        if (m.value.isRule) {
                            retVal = interpreter!!.evalCall(obj.literal, obj.tag.name, m.key)
                            val finalRet = getTTLLiteral(retVal.second)
                            res += "run:${obj.literal} prog:${m.value.declaringClass}_${m.key}_builtin_res $finalRet.\n"

                        }
                        if (m.value.isDomain && heap[obj]!!.containsKey("__models")) {
                            val models =
                                heap[obj]!!.getOrDefault(
                                    "__models",
                                    LiteralExpr("ERROR")
                                ).literal.removeSurrounding("\"")

                            if(retVal == null) retVal = interpreter!!.evalCall(obj.literal, obj.tag.name, m.key)
                            val finalRet = getTTLLiteral(retVal.second)
                            res += "$models domain:${m.value.declaringClass}_${m.key}_builtin_res $finalRet.\n"
                        }
                    }
                }
            }
        }



        // dumps processes

        res += "\n"
        var prevStackEntry: StackEntry? = null
        for (stackEntry in stack){
            if (prevStackEntry != null){
                res += "run:pro${prevStackEntry.id} smol:nextOnStack run:pro${stackEntry.id}.\n"
            }
            prevStackEntry = stackEntry
            res += "run:pro${stackEntry.id} rdf:type smol:Process.\n"
            res += "run:pro${stackEntry.id} smol:runsOnObject run:${stackEntry.obj}.\n"
            for ((key, value) in stackEntry.store){
                if (key != "this" && key.first() != '_') {
                    res += "run:pro${stackEntry.id} prog:${key} run:${value}.\n"
                }
            }
            res += "run:pro${stackEntry.id} smol:active prog:stmt${stackEntry.active.hashCode()}.\n"
            res += stackEntry.active.getRDF()
        }

        // dumps simulation processes
        for(obj in simulation.keys){
            res += "run:${obj.literal} rdf:type owl:NamedIndividual , smol:Simulation.\n"
            val sim = simulation.getValue(obj)
            res += sim.dump("run:${obj.literal}")
        }
        return res + "\n" + settings.background
    }
}



/*
We use the term "heap" NOT in the sense of C and other low-level here.
Heap memory is barely the opposite of local memory, we have no assumptions about the memory.
 */
typealias Memory = MutableMap<String, LiteralExpr>       // Maps variable names to values
typealias GlobalMemory = MutableMap<LiteralExpr, Memory>  // Maps object name literals to local memories
typealias SimulationMemory = MutableMap<LiteralExpr, SimulatorObject>  // Maps object name literals to local memories
typealias FieldEntry = List<FieldInfo>                   //list of fields
typealias ModelsEntry = Pair<Expression, String>      //guard expression and models string

enum class Visibility { PUBLIC, PROTECTED, PRIVATE}

data class FieldInfo(val name: String, val type: Type, val computationVisibility : Visibility, val inferenceVisibility: Visibility, val declaredIn : Type, val isDomain : Boolean)
data class MethodInfo(val stmt: Statement, val params: List<String>, val isRule : Boolean, val isDomain: Boolean, val declaringClass: String)
data class StaticTable(
    val fieldTable: Map<String, FieldEntry>,                // This maps class names to their fields
    val methodTable: Map<String, Map<String, MethodInfo>>, // This maps class names to a map that maps method names to their definition
    val hierarchy: MutableMap<String, MutableSet<String>> = mutableMapOf(),
    val modelsTable: Map<String, List<ModelsEntry>>                // This maps class names to models blocks
) { // DOWNWARDS class hierarchy
    override fun toString(): String =
"""
Class Hierarchy : $hierarchy 
FieldTable      : $fieldTable 
MethodTable     : $methodTable 
""".trimIndent()

    private fun getSuper(name : String) : String?{
        for(obj in hierarchy.entries){
            for(obj2 in obj.value){
                if(obj2 == name) return obj.key
            }
        }
        return null
    }

    fun getSuperMethod(className : String, methodName : String) : MethodInfo?{
        var current = getSuper(className)
        while(current != null && current != "Object"){
            if(!methodTable.containsKey(current)) return null
            if(methodTable[current]!!.containsKey(methodName)) return methodTable[current]!![methodName]
            current = getSuper(current)
        }
        return null
    }

    fun dumpClasses() : String{
        var res = ""
        for(obj in fieldTable){
            res += "prog:${obj.key} rdf:type smol:Class.\n"
            res += "prog:${obj.key} rdf:type owl:Class.\n"
            for(obj2 in obj.value){
                if(obj2.inferenceVisibility == Visibility.PRIVATE) continue
                val fieldName = obj.key+"_"+obj2.name
                res += "prog:${obj.key} smol:hasField prog:$fieldName.\n"
                res += "prog:$fieldName rdf:type smol:Field.\n"
                if(obj2.type == INTTYPE || obj2.type == STRINGTYPE) {
                    res += "prog:$fieldName rdf:type owl:DatatypeProperty.\n"
                } else {
                    res += "prog:$fieldName rdf:type owl:FunctionalProperty.\n"
                    res += "prog:$fieldName rdf:type owl:ObjectProperty.\n"
                }
                res += "prog:$fieldName rdfs:domain prog:${obj.key}.\n"
            }
        }

        //records all methods
        for(obj in methodTable){
            for(obj2 in obj.value){
                val metName = obj.key+"_"+obj2.key
                res += "prog:${obj.key} smol:hasMethod prog:$metName.\n"
                res += "prog:$metName rdf:type owl:NamedIndividual , smol:Method.\n"
            }
        }


        val all = methodTable.keys.toMutableSet()
        //records type hierarchy
        for(obj in hierarchy.entries){
            for(obj2 in obj.value){
                res += "prog:$obj2 rdfs:subClassOf prog:${obj.key}.\n"
                all -= obj2
            }
        }
        for(obj in all)
            res += "prog:$obj rdfs:subClassOf prog:Object.\n"

        return res
    }
}

data class StackEntry(val active: Statement, val store: Memory, val obj: LiteralExpr, val id: Int)
