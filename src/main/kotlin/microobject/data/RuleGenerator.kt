package microobject.data

import antlr.microobject.gen.WhileParser
import microobject.main.Settings
import microobject.runtime.InterpreterBridge
import microobject.runtime.Memory
import microobject.runtime.StackEntry
import microobject.type.BaseType
import org.apache.jena.graph.Node
import org.apache.jena.graph.NodeFactory
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
                             interpreterBridge: InterpreterBridge) = object : BaseBuiltin() {
        override fun getName(): String = "${cl.className.text}_${nm.NAME()}_builtin"

        override fun getArgLength(): Int  = 1

        override fun headAction(args: Array<out Node>?, length: Int, context: RuleContext?) {
            //Get target object
            val thisVar = getArg(0, args, context)

            //Get current state and make a copy
            val ipr = interpreterBridge.interpreter
                ?: throw Exception("Builtin functor cannot be expanded if the interpreter is unknown.")
            val myIpr = ipr.coreCopy()

            //Construct initial state
            val classStmt =
                myIpr.staticInfo.methodTable[cl.className.text
                    ?: throw Exception("Error during builtin generation")]
                    ?: throw Exception("Error during builtin generation")
            val met = classStmt[nm.NAME().text] ?: throw Exception("Error during builtin generation")
            val mem: Memory = mutableMapOf()
            val obj = LiteralExpr(
                thisVar.toString().removePrefix(settings.runPrefix),
                BaseType(cl.className.text)
            )
            mem["this"] = obj
            val se = StackEntry(met.first, mem, obj, Names.getStackId())
            myIpr.stack.push(se)

            //Run your own mini-REPL
            //But 1. We ignore `breakpoint` and
            //    2. we do not terminate the interpreter but stop at the last return so we get the last return value
            while (true) {
                if (myIpr.stack.peek().active is ReturnStmt && myIpr.stack.size == 1) {
                    //Evaluate final return expressions
                    val resStmt = myIpr.stack.peek().active as ReturnStmt
                    val res = resStmt.value
                    val ret = myIpr.evalTopMost(res).literal

                    //Build final triple and add it to the context
                    val str = if (ret.toIntOrNull() == null) ret else "smol:$ret"
                    val resNode = NodeFactory.createURI(str)
                    val connectInNode = NodeFactory.createURI("${settings.progPrefix}${name}_res")
                    val triple = Triple.create(thisVar, connectInNode, resNode)
                    context!!.add(triple)
                    break
                }
                myIpr.makeStep()
            }
        }
    }


    fun generateBuiltins(ctx: WhileParser.ProgramContext?, interpreterBridge: InterpreterBridge) : String{
        var num = 0
        var retString = "["
        for(cl in ctx!!.class_def()){
            for(nm in cl.method_def()) {
                if(nm.builtinrule != null){
                    println("Generating builtin functor and rule for ${nm.NAME()}...")
                    val builtin : Builtin = buildFunctor(cl, nm, interpreterBridge)
                    BuiltinRegistry.theRegistry.register(builtin)
                    val ruleString = "rule${num++}:"
                    val headString = "${builtin.name}(?this)"
                    val thisString = "(?this smol:instanceOf prog:${cl.className.text})"
                    retString += " $ruleString $thisString -> $headString "
                }
            }
        }
        val str = if(retString != "[") "$retString]" else ""
        if(str != "") println("rules: $str")
        return str
    }
}