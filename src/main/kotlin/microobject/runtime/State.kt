package microobject.runtime

import microobject.data.LiteralExpr
import microobject.data.Statement
import java.util.*

//This will be used for snapshots
class State(initStack  : Stack<StackEntry>, initHeap: GlobalMemory, initInfo : StaticTable, private val background : String) {
    private val stack: Stack<StackEntry> = initStack.clone() as Stack<StackEntry>
    private val heap: GlobalMemory = initHeap.toMutableMap()
    private val staticInfo: StaticTable = initInfo.copy()

    companion object{
        private val HEADER =
        """
        @prefix : <urn:> .
        @prefix smol: <https://github.com/Edkamb/SemanticObjects#> .
        @prefix prog: <urn:> .
        @prefix run: <urn:> .
        @prefix owl: <http://www.w3.org/2002/07/owl#> .
        @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
        @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
        @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
        """.trimIndent()

        private val VOCAB = this::class.java.classLoader.getResource("vocab.owl").readText()

        private val MINIMAL =
        """                     
        smol:null rdf:type owl:NamedIndividual , smol:Object .
        prog:_Entry_ rdf:type owl:NamedIndividual , smol:Class .

        """.trimIndent()
    }

    fun dump() : String{

        //Builds always known information and meta data
        var res = HEADER + "\n" + VOCAB + "\n" + background + "\n" + MINIMAL


        //records all classes and their fields
        for(obj in staticInfo.fieldTable){
            res += ":${obj.key} rdf:type owl:NamedIndividual , smol:Class.\n"
            for(obj2 in obj.value){
                res += ":$obj2 rdfs:subPropertyOf smol:Field.\n"
                res += ":${obj.key} smol:hasField :$obj2.\n"
                res += """
                prog:${obj.key} rdfs:subClassOf [
                    rdf:type owl:Restriction;
                    owl:onProperty prog:$obj2 ;
                    owl:cardinality 1
                ] .

                """.trimIndent()
            }
        }

        //records all methods
        for(obj in staticInfo.methodTable){
            for(obj2 in obj.value){
                res += ":${obj.key} smol:hasMethod prog:${obj2.key}.\n"
                res += "prog:${obj2.key} rdf:type owl:NamedIndividual , smol:Method.\n"
            }
        }

        //records type hierarchy
        for(obj in staticInfo.hierarchy.entries){
            for(obj2 in obj.value){
                res += ":$obj2 smol:extends prog:${obj.key}.\n"
            }
        }

        //dumps individuals
        var i = 0
        for(obj in heap.keys){
            res += ":${obj.literal} smol:instanceOf prog:${obj.tag}.\n"
            res += ":${obj.literal} rdf:type owl:NamedIndividual , smol:Object.\n"
            //and their fields
            for(store in heap[obj]!!.keys) {
                val target = heap[obj]!!.getOrDefault(store, LiteralExpr("ERROR"))
                res += ":${obj.literal} prog:$store "
                res += if(target.tag == "IGNORE" || target.tag == "string")
                    "${target.literal}.\n"
                else if(target.literal == "null")
                    "smol:${target.literal}.\n"
                else
                    ":${target.literal}.\n"
                i++
            }
        }

        // dumps processes
        res += "\n"
        var prevStackEntry: StackEntry? = null
        for (stackEntry in stack){
            if (prevStackEntry != null){
                res += ":pro${prevStackEntry.id} smol:nextOnStack run:pro${stackEntry.id}.\n"
            }
            prevStackEntry = stackEntry
            res += "run:pro${stackEntry.id} rdf:type smol:Process.\n"
            res += "run:pro${stackEntry.id} smol:runsOnObject run:${stackEntry.obj}.\n"
            for ((key, value) in stackEntry.store){
                if (key != "this" && key.first() != '_') {
                    res += "run:pro${stackEntry.id} prog:${key} run:${value}.\n"
                }
            }
            res += ":pro${stackEntry.id} smol:active prog:stmt${stackEntry.active.hashCode()}.\n"
            res += stackEntry.active.getRDF()
        }

        return res
    }
}



/*
We use the term "heap" NOT in the sense of C and other low-level here.
Heap memory is barely the opposite of local memory, we have no assumptions about the memory.
 */
typealias Memory = MutableMap<String, LiteralExpr>       // Maps variable names to values
typealias GlobalMemory = MutableMap<LiteralExpr, Memory>  // Maps object name literals to local memories
typealias FieldEntry = List<String>                   //list of fields
typealias MethodEntry = Pair<Statement, List<String>> //method body and list of parameters

data class StaticTable(
    val fieldTable: Map<String, FieldEntry>,               // This maps class names to their fields
    val methodTable: Map<String, Map<String, MethodEntry>>, // This maps class names to a map that maps method names to their definition
    val hierarchy: MutableMap<String, MutableSet<String>> = mutableMapOf()
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

    fun getSuperMethod(className : String, methodName : String) : MethodEntry?{
        var current = getSuper(className)
        while(current != null && current != "Object"){
            if(!methodTable.containsKey(current)) return null
            if(methodTable[current]!!.containsKey(methodName)) return methodTable[current]!![methodName]
            current = getSuper(current)
        }
        return null
    }
}

data class StackEntry(val active: Statement, val store: Memory, val obj: LiteralExpr, val id: Int)