// Generated from /home/edkam/src/keyma/MicroObjects/src/main/resources/While.g4 by ANTLR 4.8
package microobject.gen;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link WhileParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface WhileVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link WhileParser#namelist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamelist(WhileParser.NamelistContext ctx);
	/**
	 * Visit a parse tree produced by {@link WhileParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(WhileParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link WhileParser#class_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClass_def(WhileParser.Class_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link WhileParser#method_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethod_def(WhileParser.Method_defContext ctx);
	/**
	 * Visit a parse tree produced by the {@code call_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCall_statement(WhileParser.Call_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code output_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOutput_statement(WhileParser.Output_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code owl_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOwl_statement(WhileParser.Owl_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code assign_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssign_statement(WhileParser.Assign_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code skip_statment}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSkip_statment(WhileParser.Skip_statmentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code debug_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDebug_statement(WhileParser.Debug_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code create_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_statement(WhileParser.Create_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code if_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf_statement(WhileParser.If_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code while_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhile_statement(WhileParser.While_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sequence_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSequence_statement(WhileParser.Sequence_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code sparql_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSparql_statement(WhileParser.Sparql_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code return_statement}
	 * labeled alternative in {@link WhileParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturn_statement(WhileParser.Return_statementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code string_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitString_expression(WhileParser.String_expressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code external_field_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExternal_field_expression(WhileParser.External_field_expressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code this_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThis_expression(WhileParser.This_expressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code var_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVar_expression(WhileParser.Var_expressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code plus_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlus_expression(WhileParser.Plus_expressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code geq_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeq_expression(WhileParser.Geq_expressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code minus_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMinus_expression(WhileParser.Minus_expressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code const_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConst_expression(WhileParser.Const_expressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code nested_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNested_expression(WhileParser.Nested_expressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code eq_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEq_expression(WhileParser.Eq_expressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code field_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitField_expression(WhileParser.Field_expressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code neq_expression}
	 * labeled alternative in {@link WhileParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNeq_expression(WhileParser.Neq_expressionContext ctx);
}