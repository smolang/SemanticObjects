// Generated from /home/edkam/src/keyma/MicroObjects/src/main/resources/While.g4 by ANTLR 4.8
package microobject.gen;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link WhileParser}.
 */
public interface WhileListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link WhileParser#namelist}.
	 * @param ctx the parse tree
	 */
	void enterNamelist(WhileParser.NamelistContext ctx);
	/**
	 * Exit a parse tree produced by {@link WhileParser#namelist}.
	 * @param ctx the parse tree
	 */
	void exitNamelist(WhileParser.NamelistContext ctx);
	/**
	 * Enter a parse tree produced by {@link WhileParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(WhileParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link WhileParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(WhileParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link WhileParser#class_def}.
	 * @param ctx the parse tree
	 */
	void enterClass_def(WhileParser.Class_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link WhileParser#class_def}.
	 * @param ctx the parse tree
	 */
	void exitClass_def(WhileParser.Class_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link WhileParser#method_def}.
	 * @param ctx the parse tree
	 */
	void enterMethod_def(WhileParser.Method_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link WhileParser#method_def}.
	 * @param ctx the parse tree
	 */
	void exitMethod_def(WhileParser.Method_defContext ctx);
	/**
	 * Enter a parse tree produced by the {@code call_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCall_statement(WhileParser.Call_statementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code call_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCall_statement(WhileParser.Call_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code output_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterOutput_statement(WhileParser.Output_statementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code output_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitOutput_statement(WhileParser.Output_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code owl_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterOwl_statement(WhileParser.Owl_statementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code owl_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitOwl_statement(WhileParser.Owl_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code assign_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterAssign_statement(WhileParser.Assign_statementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code assign_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitAssign_statement(WhileParser.Assign_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code skip_statment}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSkip_statment(WhileParser.Skip_statmentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code skip_statment}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSkip_statment(WhileParser.Skip_statmentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code debug_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterDebug_statement(WhileParser.Debug_statementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code debug_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitDebug_statement(WhileParser.Debug_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code create_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterCreate_statement(WhileParser.Create_statementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code create_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitCreate_statement(WhileParser.Create_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code if_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterIf_statement(WhileParser.If_statementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code if_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitIf_statement(WhileParser.If_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code while_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterWhile_statement(WhileParser.While_statementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code while_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitWhile_statement(WhileParser.While_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sequence_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSequence_statement(WhileParser.Sequence_statementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sequence_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSequence_statement(WhileParser.Sequence_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sparql_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterSparql_statement(WhileParser.Sparql_statementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sparql_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitSparql_statement(WhileParser.Sparql_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code return_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterReturn_statement(WhileParser.Return_statementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code return_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitReturn_statement(WhileParser.Return_statementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code string_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterString_expression(WhileParser.String_expressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code string_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitString_expression(WhileParser.String_expressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code external_field_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExternal_field_expression(WhileParser.External_field_expressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code external_field_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExternal_field_expression(WhileParser.External_field_expressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code this_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterThis_expression(WhileParser.This_expressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code this_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitThis_expression(WhileParser.This_expressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code var_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterVar_expression(WhileParser.Var_expressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code var_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitVar_expression(WhileParser.Var_expressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code plus_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterPlus_expression(WhileParser.Plus_expressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code plus_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitPlus_expression(WhileParser.Plus_expressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code geq_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterGeq_expression(WhileParser.Geq_expressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code geq_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitGeq_expression(WhileParser.Geq_expressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code minus_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterMinus_expression(WhileParser.Minus_expressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code minus_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitMinus_expression(WhileParser.Minus_expressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code const_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterConst_expression(WhileParser.Const_expressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code const_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitConst_expression(WhileParser.Const_expressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nested_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterNested_expression(WhileParser.Nested_expressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nested_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitNested_expression(WhileParser.Nested_expressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code eq_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterEq_expression(WhileParser.Eq_expressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code eq_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitEq_expression(WhileParser.Eq_expressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code field_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterField_expression(WhileParser.Field_expressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code field_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitField_expression(WhileParser.Field_expressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code neq_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterNeq_expression(WhileParser.Neq_expressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code neq_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitNeq_expression(WhileParser.Neq_expressionContext ctx);
}