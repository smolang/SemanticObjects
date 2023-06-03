package no.uio.microobject.runtime

import api.VerificationAPI
import core.*
import core.Set
import no.uio.microobject.ast.expr.LiteralExpr
import no.uio.microobject.ast.expr.TRUEEXPR
import no.uio.microobject.type.BOOLEANTYPE
import no.uio.microobject.type.DOUBLETYPE
import no.uio.microobject.type.INTTYPE
import no.uio.microobject.type.STRINGTYPE
import org.javafmi.modeldescription.SimpleType
import org.javafmi.modeldescription.v2.ModelDescription
import org.javafmi.wrapper.Simulation
import org.javafmi.wrapper.variables.SingleRead
import scala.collection.JavaConverters
import scala.collection.immutable.Map
import kotlin.collections.HashMap

data class Snapshot( val time : Double, val values : List<Pair<String, Double>>, val role : String?)

class SimulatorObject(val path : String, memory : Memory){
    companion object  {
        const val ROLEFIELDNAME = "role"
        const val PSEUDOOFFSETFIELDNAME = "pseudoOffset"
        const val TIMEFIELDNAME = "time"
    }
    private val series = mutableListOf<Snapshot>()
    private var sim : Simulation = Simulation(path)
    private var time : Double = 0.0

    //representation for monitor
    var scen : FmuModel?
    var assignedScen = mutableListOf<SimulationScenario>()

    //additional fields
    var role : String = ""
    private var pseudoOffset : Double = 0.0


    fun read(name: String): LiteralExpr {
        val v = sim.modelDescription.getModelVariable(name)
        assignedScen.forEach { it.get(role, v.name) }

        if(name == ROLEFIELDNAME) return LiteralExpr(role, STRINGTYPE)
        if(name == PSEUDOOFFSETFIELDNAME) return LiteralExpr(pseudoOffset.toString(), DOUBLETYPE)
        if(name == TIMEFIELDNAME) return LiteralExpr(time.toString(), DOUBLETYPE)


        if(v.typeName == "Integer") return LiteralExpr(sim.read(name).asInteger().toString(), INTTYPE)
        if(v.typeName == "Boolean") return LiteralExpr(sim.read(name).asBoolean().toString(), BOOLEANTYPE)
        if(v.typeName == "Real") return LiteralExpr(sim.read(name).asDouble().toString(), DOUBLETYPE)
        return LiteralExpr(sim.read(name).asString(), STRINGTYPE)
    }
    fun tick(i : Double){
        sim.doStep(i)
        time += i
        assignedScen.forEach { it.tick(role) }
        addSnapshot()
    }

    private fun addSnapshot( ){
        /*
        var list = emptyList<Pair<String, Double>>()
        for(mVar in sim.modelDescription.modelVariables){
            if(mVar.causality == "output" && mVar.typeName == "Real"){
                val res = sim.read(mVar.name).asDouble()
                list = list + Pair(mVar.name, res)
            }
        }
        val roleName = if(role == "") null else role
        series.add(Snapshot(time, list, roleName))
         */
    }

    fun write(name: String, res: LiteralExpr) {
        if(name == ROLEFIELDNAME) {
            role = res.literal.removeSurrounding("\"","\"")
            return
        }
        if(name == PSEUDOOFFSETFIELDNAME) {
            pseudoOffset = res.literal.toDouble()
            return
        }
        if(name == TIMEFIELDNAME){
            throw Exception("You cannot write the time of an FMU.")
        }
        for(mVar in sim.modelDescription.modelVariables){
            if(mVar.name == name){
                if(mVar.causality == "input" && mVar.typeName == "Integer"){
                    sim.write(name).with(res.literal.toInt())
                    assignedScen.forEach { it.set(role, mVar.name) }
                    break
                } else if(mVar.causality == "input" && mVar.typeName == "Boolean"){
                    sim.write(name).with(res == TRUEEXPR)
                    assignedScen.forEach { it.set(role, mVar.name) }
                    break
                } else if(mVar.causality == "input" && mVar.typeName == "Real"){
                    sim.write(name).with(res.literal.toDouble())
                    assignedScen.forEach { it.set(role, mVar.name) }
                    break
                } else if(mVar.causality == "input" && mVar.typeName == "String"){
                    sim.write(name).with(res.literal )
                    assignedScen.forEach { it.set(role, mVar.name) }
                    break
                }
            }
        }
    }


    fun terminate() {
        sim.terminate()
    }

    init {
        val inputScenMap = mutableMapOf<String, InputPortConfig>()
        val outputTempScenMap = mutableMapOf<String, scala.collection.immutable.List<String>>()
        for(mVar in sim.modelDescription.modelVariables){
            if(mVar.causality == "input" || mVar.causality == "parameter"){
                inputScenMap.put(mVar.name, InputPortConfig("delayed")) //todo: check what the reactivity models
                if(!mVar.hasStartValue() && !memory.containsKey(mVar.name))
                    throw Exception("Failed to initialize variable ${mVar.name}: no initial value given")
                if(memory.containsKey(mVar.name)) {
                    when (mVar.typeName) {
                        "Integer" -> sim.write(mVar.name).with(memory[mVar.name]!!.literal.toInt())
                        "Boolean" -> sim.write(mVar.name).with(memory[mVar.name]!!.literal.toBoolean())
                        "Real" -> sim.write(mVar.name).with(memory[mVar.name]!!.literal.toDouble())
                        else -> /*if (mVar.typeName == "String")*/ sim.write(mVar.name).with(memory[mVar.name]!!.literal.removeSurrounding("\""))
                    }
                } else if(mVar.hasStartValue()){
                    //println("using default start value for ${sim.modelDescription.modelName}.${mVar.name}")
                    val anyStart = mVar.start
                    when (mVar.typeName) {
                        "Integer" -> sim.write(mVar.name).with(anyStart as Int)
                        "Boolean" -> sim.write(mVar.name).with(anyStart as Boolean)
                        "Real" -> sim.write(mVar.name).with(anyStart as Double)
                        else -> /*if (mVar.typeName == "String")*/ sim.write(mVar.name).with(anyStart as String)
                    }
                }
            }
            if(mVar.causality == "output" && mVar.hasStartValue() && !memory.containsKey(mVar.name)){
                val anyStart = mVar.start
                outputTempScenMap.put(mVar.name, JavaConverters.asScala(listOf<String>()).toList()) //otherwise we miss the outputs without dependencies
                when (mVar.typeName) {
                    "Integer" -> sim.write(mVar.name).with(anyStart as Int)
                    "Boolean" -> sim.write(mVar.name).with(anyStart as Boolean)
                    "Real" -> sim.write(mVar.name).with(anyStart as Double)
                    else -> /*if (mVar.typeName == "String")*/ sim.write(mVar.name).with(anyStart as String)
                }
            }
            if((mVar.causality == "output" && mVar.initial == "calculated") && memory.containsKey(mVar.name)) {
                throw Exception("Cannot initialize output or/and calculated variable ${mVar.name}")
            }
        }

        for (dep in (sim.modelDescription as ModelDescription).modelStructure.outputs){
            val ind = dep.index
            if(dep.dependencies.isNotBlank()) {
                val deps = dep.dependencies.split(" ").map { it.toInt() }
                val name = sim.modelDescription.getModelVariable(ind).name //-1?
                val depVars = deps.map { sim.modelDescription.getModelVariable(it).name }
                outputTempScenMap.put(name, JavaConverters.asScala(depVars).toList())
            }
        }

        val outputScenMap = outputTempScenMap.mapValues { OutputPortConfig(JavaConverters.asScala(listOf<String>()).toList(), it.value)}.toMutableMap()
        scen = ScenarioLoader.parse("", FmuConfig(toScalaMap(inputScenMap), toScalaMap(outputScenMap), false, "" ))

        sim.init(0.0)

        addSnapshot()
    }


    private fun dumpSingle(read : SingleRead, type : SimpleType) : String{
        return when(type){
            is org.javafmi.modeldescription.v2.IntegerType -> read.asInteger().toString()
            is org.javafmi.modeldescription.v1.IntegerType -> read.asInteger().toString()
            is org.javafmi.modeldescription.v2.StringType -> "'"+read.asString()+"'"
            is org.javafmi.modeldescription.v1.StringType -> "'"+read.asString()+"'"
            is org.javafmi.modeldescription.v2.BooleanType -> if(read.asBoolean()) "'1'" else "'0'"
            is org.javafmi.modeldescription.v1.BooleanType -> if(read.asBoolean()) "'1'" else "'0'"
            is org.javafmi.modeldescription.v2.RealType -> "'"+read.asDouble().toString()+"'"
            is org.javafmi.modeldescription.v1.RealType -> "'"+read.asDouble().toString()+"'"
            else -> throw Exception("Unknown Type")
        }
    }

    fun <K,V> toScalaMap(inp : MutableMap<K, V>): scala.collection.immutable.HashMap<K, V>? {
        val ins1  = JavaConverters.mapAsScalaMapConverter(inp).asScala()
        return scala.collection.immutable.HashMap<K, V>().concat(ins1)
    }

    fun assignedTo(simulationScenario: SimulationScenario) {
        assignedScen.add(simulationScenario)
    }

    fun canGet(f: String): Boolean {
        return assignedScen.all { it.canGet(role, f) }
    }
    fun canSet(f: String): Boolean {
        return assignedScen.all { it.canSet(role, f) }
    }
    fun canTick(): Boolean {
        return assignedScen.all { it.canTick(role) }
    }
}





class SimulationScenario(path : String){
    val assignedFmus = mutableMapOf<String, SimulatorObject>()
    var config : MasterModel
    var closed = false //closed <=> check succeeded and no FMU has been re-assigned

    val steps = mutableListOf<CosimStepInstruction>()

    init{
        config = ScenarioLoader.load(path)
    }

    fun assign(fmo : SimulatorObject){
        assignedFmus.put(fmo.role, fmo)
        fmo.assignedTo(this)
        config.scenario().fmus()
        closed = false
    }

    fun check() : Boolean{
        //check whether all FMUs are assigned
        val assignedAllInScenario = assignedFmus.keys.any { !config.scenario().fmus().contains(it) }
        val scenarioAllAssigned = config.scenario().fmus().keys().filter { assignedFmus.containsKey(it) }.isEmpty

        if(!(assignedAllInScenario && scenarioAllAssigned))
            return false

        //create new mastermodel
        val fmus = assignedFmus.mapValues { it.value.scen }.toMutableMap()
        config = config.copy(
            config.name(),
            config.scenario().copy(
                toScalaMap(fmus) as Map<String, FmuModel>,
                config.scenario().config(),
                config.scenario().connections(),
                config.scenario().maxPossibleStepSize()
            ),
            config.instantiation(),
            config.initialization(),
            config.cosimStep(),
            config.terminate()
        )
        closed = true


        return VerificationAPI.verifyAlgorithm(config)
    }


    fun <K,V> toScalaMap(inp : MutableMap<K, V>): scala.collection.immutable.HashMap<K, V>? {
        val ins1  = JavaConverters.mapAsScalaMapConverter(inp).asScala()
        return scala.collection.immutable.HashMap<K, V>().concat(ins1)
    }

    fun set(role: String, name: String) {
        if(!canSet(role, name)) throw Exception("Invalid action tick on $role")
        steps.add(Set(PortRef(role, name)))
    }
    fun get(role: String, name: String) {
        if(!canGet(role, name)) throw Exception("Invalid action tick on $role")
        steps.add(Get(PortRef(role, name)))
    }
    fun tick(role: String) {
        if(!canTick(role)) throw Exception("Invalid action tick on $role")
        steps.add(Step(role, DefaultStepSize()))
    }

    fun canGet(role: String, name: String) : Boolean {
        return VerificationAPI.dynamicVerification(config.scenario(), JavaConverters.iterableAsScalaIterable(steps).toList(), Get(PortRef(role, name))).correct()
    }
    fun canSet(role: String, name: String) : Boolean {
    //    val a = System.currentTimeMillis()
        val hue = JavaConverters.iterableAsScalaIterable(steps).toList()
        val scen = config.scenario()
    //    val b = System.currentTimeMillis()
        val res= VerificationAPI.dynamicVerification(scen, hue , Set(PortRef(role, name))).correct()
    //    val c = System.currentTimeMillis()
    //    println("translate : ${b-a}ms \t verify : ${c-b}ms")
        return res;
    }
    fun canTick(role: String) : Boolean {
        return VerificationAPI.dynamicVerification(config.scenario(), JavaConverters.iterableAsScalaIterable(steps).toList(), Step(role, DefaultStepSize())).correct()
    }

    fun disable() {
        assignedFmus.forEach { s, simulatorObject -> simulatorObject.assignedScen.remove(this)  }
        assignedFmus.clear()
    }
}