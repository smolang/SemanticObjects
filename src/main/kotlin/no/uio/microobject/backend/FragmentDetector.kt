package no.uio.microobject.backend

import no.uio.microobject.antlr.WhileBaseListener
import no.uio.microobject.antlr.WhileParser
import org.antlr.v4.runtime.tree.ParseTreeWalker

/* Will be used later, do not delete */
class FragmentDetector(val prog : WhileParser.ProgramContext) : WhileBaseListener() {
    var fmu = false
    var semantic = false

    init {
        ParseTreeWalker.DEFAULT.walk(this, prog)
    }

    override fun enterSimulate_statement(ctx: WhileParser.Simulate_statementContext?) {
        fmu = true
    }

    override fun enterTick_statement(ctx: WhileParser.Tick_statementContext?) {
        fmu = true
    }

    override fun enterSparql_statement(ctx: WhileParser.Sparql_statementContext?) {
        semantic = true
    }

    override fun enterConstruct_statement(ctx: WhileParser.Construct_statementContext?) {
        semantic = true
    }

    override fun enterOwl_statement(ctx: WhileParser.Owl_statementContext?) {
        semantic = true
    }

    override fun enterValidate_statement(ctx: WhileParser.Validate_statementContext?) {
        semantic = true
    }
}