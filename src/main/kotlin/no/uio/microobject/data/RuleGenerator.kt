package no.uio.microobject.data

import no.uio.microobject.antlr.WhileParser
import no.uio.microobject.main.Settings
import no.uio.microobject.runtime.InterpreterBridge
import no.uio.microobject.runtime.Memory
import no.uio.microobject.runtime.StackEntry
import no.uio.microobject.type.INTTYPE
import no.uio.microobject.type.STRINGTYPE
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.Node
import org.apache.jena.graph.NodeFactory
import org.apache.jena.graph.Node_Blank
import org.apache.jena.graph.Triple
import org.apache.jena.reasoner.rulesys.Builtin
import org.apache.jena.reasoner.rulesys.BuiltinRegistry
import org.apache.jena.reasoner.rulesys.RuleContext
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin

/**
 * This class generates the functors and rules for invoke statement execution from within queries
 */
class RuleGenerator(val settings: Settings){

    /*
       This generates a functor for cl.nm.
       The interpreter bridge is a future-like pattern that will later hold the current state of the interpreter.
     */
    private fun buildFunctor(cl : WhileParser.Class_defContext,
                             nm : WhileParser.Method_defContext,
                             interpreterBridge: InterpreterBridge,
                             domain : Boolean) = object : BaseBuiltin() {
        override fun getName(): String = "${cl.className.text}_${nm.NAME()}_builtin"

        override fun getArgLength(): Int  = 1

        override fun headAction(args: Array<out Node>?, length: Int, context: RuleContext?) {
            //Get target object
            val thisVar = getArg(0, args, context)
            if(thisVar is Node_Blank) return //weird effects when we have blank nodes

            //Get current state and make a copy
            val myIpr = interpreterBridge.interpreter
                ?: throw Exception("Builtin functor cannot be expanded if the interpreter is unknown.")

            val (obj, topmost) = myIpr.evalCall(thisVar.toString().removePrefix(settings.runPrefix),
                                                cl.className.text,
                                                nm.NAME().text)
            val ret = topmost.literal

            //Build final triple and add it to the context
            val resNode = if (topmost.tag == INTTYPE) NodeFactory.createLiteral(ret.removeSurrounding("\""), XSDDatatype.XSDint) else
                if(topmost.tag == STRINGTYPE) NodeFactory.createLiteral(ret, XSDDatatype.XSDstring)  else NodeFactory.createURI("smol:$ret")
            val connectInNode = NodeFactory.createURI("${settings.progPrefix}${name}_res")
            val triple = Triple.create(thisVar, connectInNode, resNode)
            context!!.add(triple)
            println("adding $triple")
            if(domain){
                val modelsTarget = myIpr.heap[obj]?.get("__models")
                if(modelsTarget != null) {
                    val targetVar = NodeFactory.createURI(settings.replaceKnownPrefixesNoColon(modelsTarget!!.literal))
                    val targetInNode = NodeFactory.createURI("${settings.domainPrefix}${name}_res")
                    val triple = Triple.create(targetVar, targetInNode, resNode)
                    context!!.add(triple)
                    println("adding $triple")
                }
            }

        }
    }


    fun generateBuiltins(ctx: WhileParser.ProgramContext?, interpreterBridge: InterpreterBridge) : String{
        return ""
        /*
        var num = 0
        var rules = listOf<String>()
        for(cl in ctx!!.class_def()){
            for(nm in cl.method_def()) {
                if(nm.builtinrule != null){
                    val domain = nm.domainrule != null
                    if(settings.verbose) println("Generating builtin functor and rule for ${nm.NAME()}...")
                    val builtin : Builtin = buildFunctor(cl, nm, interpreterBridge, domain)
                    BuiltinRegistry.theRegistry.register(builtin)
                    val ruleString = "rule${num++}:"
                    val headString = "${builtin.name}(?this)"
                    val thisString = "(?this rdf:type prog:${cl.className.text})"
                    rules += "[$ruleString $thisString -> $headString]"
                }
            }
        }
        val str = rules.joinToString("")
        if(str != "" && settings.verbose) println("rules: $str")
        return str
        */
    }
}
