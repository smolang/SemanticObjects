package microobject.runtime

import microobject.data.LiteralExpr
import microobject.data.Statement
import java.util.*

//This will be used for snapshots
class State(initStack  : Stack<StackEntry>, initHeap: GlobalMemory, initInfo : StaticTable, private val background : String) {
    private val stack: Stack<StackEntry> = initStack.clone() as Stack<StackEntry>
    private val heap: GlobalMemory = initHeap.toMutableMap()
    private val staticInfo: StaticTable = initInfo.copy()

    private val HEADER =
        """
        @prefix : <urn:> .
        @prefix owl: <http://www.w3.org/2002/07/owl#> .
        @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
        @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
        """.trimIndent()

    private val VOCAB =
        """
        :MOXClass   rdf:type owl:Class .
        :MOXField   rdf:type owl:Class .
        :MOXMethod  rdf:type owl:Class .
        :MOXObject  rdf:type owl:Class .
        :MOXStorage rdf:type owl:Class .
        
        :MOinstanceOf rdf:type owl:ObjectProperty ;
                      rdfs:domain :MOXObject ;
                      rdfs:range :MOXClass .
                      
        :MOhasField   rdf:type owl:ObjectProperty ;
                      rdfs:domain :MOXClass ;
                      rdfs:range :MOXField .
                      
        :MOhasMethod  rdf:type owl:ObjectProperty ;
                      rdfs:domain :MOXClass ;
                      rdfs:range :MOXMethod .
        
        :MOextends    rdf:type owl:ObjectProperty ;
                      rdfs:domain :MOXClass ;
                      rdfs:range :MOXClass .
        
        :MOstore      rdf:type owl:ObjectProperty ;
                      rdfs:domain :MOXObject ;
                      rdfs:range :MOXStorage .
                      
        :MOvalue      rdf:type owl:ObjectProperty ;
                      rdfs:domain :MOXStorage.
                      
        :MOfield      rdf:type owl:ObjectProperty ;
                      rdfs:domain :MOXStorage ;
                      rdfs:range :MOXField .
        """.trimIndent()


    private val MINIMAL =
        """
       :Test rdf:type owl:Class ;
             owl:equivalentClass :MOXfield .
                     
       :null rdf:type owl:NamedIndividual , :MOXObject .
       :_Entry_ rdf:type owl:NamedIndividual , :MOXClass .
        """.trimIndent()

    fun dump() : String{

        //Builds always known information and meta data
        var res = HEADER + "\n" + VOCAB + "\n"+ background + "\n" + MINIMAL


        //records all classes and their fields
        for(obj in staticInfo.fieldTable){
            res += ":${obj.key} rdf:type owl:NamedIndividual , :MOXClass.\n"
            for(obj2 in obj.value){
                res += ":${obj.key} :MOhasField :$obj2.\n"
                res += ":$obj2 rdf:type owl:NamedIndividual , :MOXField.\n"
                res += ":${obj.key} :MOhasField :$obj2.\n"
            }
        }

        //records all methods
        for(obj in staticInfo.methodTable){
            for(obj2 in obj.value){
                res += ":${obj.key} :MOhasMethod :${obj2.key}.\n"
                res += ":${obj2.key} rdf:type owl:NamedIndividual , :MOXMethod.\n"
                //res += ":MOXMethod :${obj2.key}.\n"
            }
        }

        //records type hierarchy
        for(obj in staticInfo.hierarchy.entries){
            for(obj2 in obj.value){
                res += ":$obj2 :MOextends :${obj.key}.\n"
            }
        }

        //dumps individuals
        var i = 0
        for(obj in heap.keys){
            res += ":${obj.literal} :MOinstanceOf :${obj.tag}.\n"
            res += ":${obj.literal} rdf:type owl:NamedIndividual , :MOXObject.\n"
            //and their fields
            for(store in heap[obj]!!.keys) {
                val target = heap[obj]!!.getOrDefault(store, LiteralExpr("ERROR"))
                res += ":${obj.literal} :MOstore _:dummy$i.\n"
                res += "_:dummy$i :MOfield :$store.\n"

                res += if (target.tag == "IGNORE") "_:dummy$i :MOvalue ${target.literal}.\n"
                       //else if (target.tag == "integer") "_:dummy$i :MOvalue ${target.literal}.\n"
                       else "_:dummy$i :MOvalue :${target.literal}.\n"

                i++
            }
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
}

data class StackEntry(val active: Statement, val store: Memory, val obj: LiteralExpr, val id: Int)