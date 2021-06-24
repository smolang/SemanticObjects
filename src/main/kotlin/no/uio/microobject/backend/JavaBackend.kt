package no.uio.microobject.backend

import no.uio.microobject.antlr.WhileBaseListener
import no.uio.microobject.antlr.WhileParser
import no.uio.microobject.runtime.StaticTable
import no.uio.microobject.type.TypeChecker
import org.antlr.v4.runtime.tree.ParseTreeWalker

class JavaBackend(val prog: WhileParser.ProgramContext, val staticTable: StaticTable) : WhileBaseListener() {
    var depth = 0
    var classes = mutableListOf<StringBuilder>()
    var active : WhileParser.Class_defContext? = null
    var builder = StringBuilder()
    init {
        ParseTreeWalker.DEFAULT.walk(this, prog)
        classes.add(StringBuilder(
            """
        package no.uio.microobject.translate; //GENERATES:SMOLObject;
        
        interface SMOLObject { public String naiveRDF(); }
        """.trimIndent()))
        classes.add(StringBuilder(
            """
package no.uio.microobject.translate; //GENERATES:SMOLManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

class SMOLManager{
   private ArrayList<WeakReference<SMOLObject>> registered = new ArrayList<>();
   public void register(SMOLObject obj){ registered.add(new WeakReference(obj)); }
   static public SMOLManager instance = new SMOLManager();
   private SMOLManager() {}
}
        """.trimIndent()))
    }
    fun offsetDepth(off : Int){
        depth += off
    }
    fun addLine(str : String, offset : Int = 0)  {
        builder.appendLine("\t".repeat(depth + offset) + str)
    }
    fun addIntend()  {
        builder.append("\t".repeat(depth))
    }
    fun addText(str : String)  {
        builder.append(str)
    }
    private fun getType(type : WhileParser.TypeContext) = TypeChecker.translateType(type, active!!.className.text, mutableMapOf())
    fun getOutput() : String{
        return classes.joinToString("\n\n") { it.toString() }
    }

    override fun enterClass_def(ctx: WhileParser.Class_defContext?) {
        builder = StringBuilder()
        active = ctx
        addLine("package no.uio.microobject.translate; //GENERATES:${ctx!!.NAME().text};")
        addLine("")
        val generics = if(ctx.namelist() != null) "<"+ctx.namelist().NAME().joinToString(",") { it.text }+">" else ""
        val prefix = "public" + if(ctx.abs != null) " abstract" else ""
        val superType = if (ctx.superType != null) " extends" + getType(ctx.superType) else "implements SMOLObject"
        addLine("$prefix class ${ctx.NAME().text}$generics $superType {")
        depth++
    }

    override fun enterFieldDecl(ctx: WhileParser.FieldDeclContext?) {
        val mod = if(ctx!!.visibilitymodifier() != null) ctx.visibilitymodifier().text else "public"
        var type = getType(ctx.type()).toString()
        if(type == "Int") type = "int"
        addLine("$mod $type ${ctx.NAME().text};")
    }

    override fun exitClass_def(ctx: WhileParser.Class_defContext?) {
        val className = ctx!!.NAME().text
        val fields = staticTable.fieldTable[className]!!.map { Pair(it.type.toString(), it.name) }
        val pList = fields.map { Pair(if(it.first == "Int") "int" else it.first, it.second) }.joinToString(", "){ it.first + " " + it.second }

        addLine("public $className($pList) {")
        depth++
        val myFields = ctx.fieldDeclList().fieldDecl().map { Pair(getType(it.type()).toString(), it.NAME().text) }
        val otherFields = fields - myFields
        addLine("super(${otherFields.joinToString(","){it.second}});")
        for(f in myFields) addLine("this.${f.second} = ${f.second};")
        addLine("SMOLManager.instance.register(this);")
        depth--
        addLine("}")

        addLine("public String naiveRDF() {")
        depth++
        addLine("String str = super();")
        addLine("str += this + \" a smol:$className.\\n\";")
        for(f in myFields) addLine("str += this + \" smol:${className}_${f.second} \" + ${f.second} +\".\\n\";")
        addLine("return str;")
        depth--
        addLine("}")
        depth--
        addLine("}")
        classes.add(builder)
    }

    override fun enterMethod_def(ctx: WhileParser.Method_defContext?) {
        if(ctx!!.paramList() == null)
            addLine("public ${getType(ctx.type())} ${ctx.NAME().text}(){")
        else {
            addLine("public ${getType(ctx.type())} ${ctx.NAME().text}(")
            depth++
            for( p in 0 until ctx.paramList().param().size) {
                val param = ctx.paramList().param()[p]
                val suffix = if( p == ctx.paramList().param().size - 1 ) "" else ", "
                addLine(getType(param.type()).toString() + " " + param.NAME().text + suffix)
            }
            depth--
            addLine("){")
        }
        depth++
        val stmt = staticTable.methodTable[active!!.className.text]!![ctx.NAME().text]!!.first
        stmt.toJava(this)
    }

    override fun exitMethod_def(ctx: WhileParser.Method_defContext?) {
        depth--
        addLine("}")
    }

    /*
    override fun enterReturn_statement(ctx: WhileParser.Return_statementContext?) {
        addIntend()
        addText("return ")
    }

    override fun exitReturn_statement(ctx: WhileParser.Return_statementContext?) {
        addText(";\n")
    }

    override fun enterThis_expression(ctx: WhileParser.This_expressionContext?) {
        addText("this")
    }

    override fun enterField_expression(ctx: WhileParser.Field_expressionContext?) {
        addText("this."+ctx!!.NAME().text)
    }

    override fun enterVar_expression(ctx: WhileParser.Var_expressionContext?) {
        addText(ctx!!.NAME().text)
    }
    */

}