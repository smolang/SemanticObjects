// Generated from /home/edkam/src/keyma/MicroObjects/src/main/resources/While.g4 by ANTLR 4.8
package microobject.gen;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class WhileParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.8", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		STRING=1, WS=2, COMMENT=3, LINE_COMMENT=4, TRUE=5, FALSE=6, SKIP_S=7, 
		EQ=8, NEQ=9, LT=10, GT=11, LEQ=12, GEQ=13, RETURN=14, ASS=15, DOT=16, 
		SEMI=17, IF=18, FI=19, THEN=20, NEW=21, ELSE=22, WHILE=23, DO=24, OD=25, 
		THIS=26, OPARAN=27, CPARAN=28, PLUS=29, MINUS=30, AND=31, OR=32, PRINTLN=33, 
		CLASS=34, END=35, EXTENDS=36, ACCESS=37, DERIVE=38, BREAKPOINT=39, COMMA=40, 
		NAME=41, CONSTANT=42;
	public static final int
		RULE_namelist = 0, RULE_program = 1, RULE_class_def = 2, RULE_method_def = 3, 
		RULE_statement = 4, RULE_expression = 5;
	private static String[] makeRuleNames() {
		return new String[] {
			"namelist", "program", "class_def", "method_def", "statement", "expression"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, "'true'", "'false'", "'skip'", "'='", "'<>'", 
			"'<'", "'>'", "'<='", "'>='", "'return'", "':='", "'.'", "';'", "'if'", 
			"'fi'", "'then'", "'new'", "'else'", "'while'", "'do'", "'od'", "'this'", 
			"'('", "')'", "'+'", "'-'", "'&'", "'|'", "'print'", "'class'", "'end'", 
			"'extends'", "'access'", "'derive'", "'breakpoint'", "','"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "STRING", "WS", "COMMENT", "LINE_COMMENT", "TRUE", "FALSE", "SKIP_S", 
			"EQ", "NEQ", "LT", "GT", "LEQ", "GEQ", "RETURN", "ASS", "DOT", "SEMI", 
			"IF", "FI", "THEN", "NEW", "ELSE", "WHILE", "DO", "OD", "THIS", "OPARAN", 
			"CPARAN", "PLUS", "MINUS", "AND", "OR", "PRINTLN", "CLASS", "END", "EXTENDS", 
			"ACCESS", "DERIVE", "BREAKPOINT", "COMMA", "NAME", "CONSTANT"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "While.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public WhileParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class NamelistContext extends ParserRuleContext {
		public List<TerminalNode> NAME() { return getTokens(WhileParser.NAME); }
		public TerminalNode NAME(int i) {
			return getToken(WhileParser.NAME, i);
		}
		public List<TerminalNode> COMMA() { return getTokens(WhileParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(WhileParser.COMMA, i);
		}
		public NamelistContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namelist; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterNamelist(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitNamelist(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitNamelist(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamelistContext namelist() throws RecognitionException {
		NamelistContext _localctx = new NamelistContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_namelist);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(12);
			match(NAME);
			setState(17);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(13);
				match(COMMA);
				setState(14);
				match(NAME);
				}
				}
				setState(19);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ProgramContext extends ParserRuleContext {
		public TerminalNode DO() { return getToken(WhileParser.DO, 0); }
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public TerminalNode OD() { return getToken(WhileParser.OD, 0); }
		public List<Class_defContext> class_def() {
			return getRuleContexts(Class_defContext.class);
		}
		public Class_defContext class_def(int i) {
			return getRuleContext(Class_defContext.class,i);
		}
		public ProgramContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_program; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterProgram(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitProgram(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitProgram(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgramContext program() throws RecognitionException {
		ProgramContext _localctx = new ProgramContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_program);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(21); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(20);
				class_def();
				}
				}
				setState(23); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==CLASS );
			setState(25);
			match(DO);
			setState(26);
			statement(0);
			setState(27);
			match(OD);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Class_defContext extends ParserRuleContext {
		public TerminalNode CLASS() { return getToken(WhileParser.CLASS, 0); }
		public List<TerminalNode> NAME() { return getTokens(WhileParser.NAME); }
		public TerminalNode NAME(int i) {
			return getToken(WhileParser.NAME, i);
		}
		public TerminalNode OPARAN() { return getToken(WhileParser.OPARAN, 0); }
		public TerminalNode CPARAN() { return getToken(WhileParser.CPARAN, 0); }
		public TerminalNode END() { return getToken(WhileParser.END, 0); }
		public TerminalNode EXTENDS() { return getToken(WhileParser.EXTENDS, 0); }
		public NamelistContext namelist() {
			return getRuleContext(NamelistContext.class,0);
		}
		public List<Method_defContext> method_def() {
			return getRuleContexts(Method_defContext.class);
		}
		public Method_defContext method_def(int i) {
			return getRuleContext(Method_defContext.class,i);
		}
		public Class_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_class_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterClass_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitClass_def(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitClass_def(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Class_defContext class_def() throws RecognitionException {
		Class_defContext _localctx = new Class_defContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_class_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(29);
			match(CLASS);
			setState(30);
			match(NAME);
			setState(33);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EXTENDS) {
				{
				setState(31);
				match(EXTENDS);
				setState(32);
				match(NAME);
				}
			}

			setState(35);
			match(OPARAN);
			setState(37);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NAME) {
				{
				setState(36);
				namelist();
				}
			}

			setState(39);
			match(CPARAN);
			setState(43);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NAME) {
				{
				{
				setState(40);
				method_def();
				}
				}
				setState(45);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(46);
			match(END);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Method_defContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(WhileParser.NAME, 0); }
		public TerminalNode OPARAN() { return getToken(WhileParser.OPARAN, 0); }
		public TerminalNode CPARAN() { return getToken(WhileParser.CPARAN, 0); }
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public TerminalNode END() { return getToken(WhileParser.END, 0); }
		public NamelistContext namelist() {
			return getRuleContext(NamelistContext.class,0);
		}
		public Method_defContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_method_def; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterMethod_def(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitMethod_def(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitMethod_def(this);
			else return visitor.visitChildren(this);
		}
	}

	public final Method_defContext method_def() throws RecognitionException {
		Method_defContext _localctx = new Method_defContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_method_def);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(48);
			match(NAME);
			setState(49);
			match(OPARAN);
			setState(51);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==NAME) {
				{
				setState(50);
				namelist();
				}
			}

			setState(53);
			match(CPARAN);
			setState(54);
			statement(0);
			setState(55);
			match(END);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StatementContext extends ParserRuleContext {
		public StatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statement; }
	 
		public StatementContext() { }
		public void copyFrom(StatementContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class Call_statementContext extends StatementContext {
		public ExpressionContext target;
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode DOT() { return getToken(WhileParser.DOT, 0); }
		public TerminalNode NAME() { return getToken(WhileParser.NAME, 0); }
		public TerminalNode OPARAN() { return getToken(WhileParser.OPARAN, 0); }
		public TerminalNode CPARAN() { return getToken(WhileParser.CPARAN, 0); }
		public TerminalNode SEMI() { return getToken(WhileParser.SEMI, 0); }
		public TerminalNode ASS() { return getToken(WhileParser.ASS, 0); }
		public List<TerminalNode> COMMA() { return getTokens(WhileParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(WhileParser.COMMA, i);
		}
		public Call_statementContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterCall_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitCall_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitCall_statement(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class Output_statementContext extends StatementContext {
		public TerminalNode PRINTLN() { return getToken(WhileParser.PRINTLN, 0); }
		public TerminalNode OPARAN() { return getToken(WhileParser.OPARAN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode CPARAN() { return getToken(WhileParser.CPARAN, 0); }
		public TerminalNode SEMI() { return getToken(WhileParser.SEMI, 0); }
		public Output_statementContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterOutput_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitOutput_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitOutput_statement(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class Owl_statementContext extends StatementContext {
		public ExpressionContext target;
		public ExpressionContext query;
		public TerminalNode ASS() { return getToken(WhileParser.ASS, 0); }
		public TerminalNode DERIVE() { return getToken(WhileParser.DERIVE, 0); }
		public TerminalNode OPARAN() { return getToken(WhileParser.OPARAN, 0); }
		public TerminalNode CPARAN() { return getToken(WhileParser.CPARAN, 0); }
		public TerminalNode SEMI() { return getToken(WhileParser.SEMI, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public Owl_statementContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterOwl_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitOwl_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitOwl_statement(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class Assign_statementContext extends StatementContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode ASS() { return getToken(WhileParser.ASS, 0); }
		public TerminalNode SEMI() { return getToken(WhileParser.SEMI, 0); }
		public Assign_statementContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterAssign_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitAssign_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitAssign_statement(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class Skip_statmentContext extends StatementContext {
		public TerminalNode SKIP_S() { return getToken(WhileParser.SKIP_S, 0); }
		public TerminalNode SEMI() { return getToken(WhileParser.SEMI, 0); }
		public Skip_statmentContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterSkip_statment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitSkip_statment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitSkip_statment(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class Debug_statementContext extends StatementContext {
		public TerminalNode BREAKPOINT() { return getToken(WhileParser.BREAKPOINT, 0); }
		public TerminalNode SEMI() { return getToken(WhileParser.SEMI, 0); }
		public TerminalNode OPARAN() { return getToken(WhileParser.OPARAN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode CPARAN() { return getToken(WhileParser.CPARAN, 0); }
		public Debug_statementContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterDebug_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitDebug_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitDebug_statement(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class Create_statementContext extends StatementContext {
		public ExpressionContext target;
		public TerminalNode ASS() { return getToken(WhileParser.ASS, 0); }
		public TerminalNode NEW() { return getToken(WhileParser.NEW, 0); }
		public TerminalNode NAME() { return getToken(WhileParser.NAME, 0); }
		public TerminalNode OPARAN() { return getToken(WhileParser.OPARAN, 0); }
		public TerminalNode CPARAN() { return getToken(WhileParser.CPARAN, 0); }
		public TerminalNode SEMI() { return getToken(WhileParser.SEMI, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(WhileParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(WhileParser.COMMA, i);
		}
		public Create_statementContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterCreate_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitCreate_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitCreate_statement(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class If_statementContext extends StatementContext {
		public StatementContext next;
		public TerminalNode IF() { return getToken(WhileParser.IF, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode THEN() { return getToken(WhileParser.THEN, 0); }
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public TerminalNode END() { return getToken(WhileParser.END, 0); }
		public TerminalNode ELSE() { return getToken(WhileParser.ELSE, 0); }
		public If_statementContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterIf_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitIf_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitIf_statement(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class While_statementContext extends StatementContext {
		public StatementContext next;
		public TerminalNode WHILE() { return getToken(WhileParser.WHILE, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode DO() { return getToken(WhileParser.DO, 0); }
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public TerminalNode END() { return getToken(WhileParser.END, 0); }
		public While_statementContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterWhile_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitWhile_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitWhile_statement(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class Sequence_statementContext extends StatementContext {
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public Sequence_statementContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterSequence_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitSequence_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitSequence_statement(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class Sparql_statementContext extends StatementContext {
		public ExpressionContext target;
		public ExpressionContext query;
		public TerminalNode ASS() { return getToken(WhileParser.ASS, 0); }
		public TerminalNode ACCESS() { return getToken(WhileParser.ACCESS, 0); }
		public TerminalNode OPARAN() { return getToken(WhileParser.OPARAN, 0); }
		public TerminalNode CPARAN() { return getToken(WhileParser.CPARAN, 0); }
		public TerminalNode SEMI() { return getToken(WhileParser.SEMI, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(WhileParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(WhileParser.COMMA, i);
		}
		public Sparql_statementContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterSparql_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitSparql_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitSparql_statement(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class Return_statementContext extends StatementContext {
		public TerminalNode RETURN() { return getToken(WhileParser.RETURN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(WhileParser.SEMI, 0); }
		public Return_statementContext(StatementContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterReturn_statement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitReturn_statement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitReturn_statement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementContext statement() throws RecognitionException {
		return statement(0);
	}

	private StatementContext statement(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		StatementContext _localctx = new StatementContext(_ctx, _parentState);
		StatementContext _prevctx = _localctx;
		int _startState = 8;
		enterRecursionRule(_localctx, 8, RULE_statement, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(170);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
			case 1:
				{
				_localctx = new Skip_statmentContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(58);
				match(SKIP_S);
				setState(59);
				match(SEMI);
				}
				break;
			case 2:
				{
				_localctx = new Assign_statementContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(60);
				expression(0);
				setState(61);
				match(ASS);
				setState(62);
				expression(0);
				setState(63);
				match(SEMI);
				}
				break;
			case 3:
				{
				_localctx = new Return_statementContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(65);
				match(RETURN);
				setState(66);
				expression(0);
				setState(67);
				match(SEMI);
				}
				break;
			case 4:
				{
				_localctx = new Call_statementContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(72);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
				case 1:
					{
					setState(69);
					((Call_statementContext)_localctx).target = expression(0);
					setState(70);
					match(ASS);
					}
					break;
				}
				setState(74);
				expression(0);
				setState(75);
				match(DOT);
				setState(76);
				match(NAME);
				setState(77);
				match(OPARAN);
				setState(86);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << STRING) | (1L << THIS) | (1L << OPARAN) | (1L << NAME) | (1L << CONSTANT))) != 0)) {
					{
					setState(78);
					expression(0);
					setState(83);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(79);
						match(COMMA);
						setState(80);
						expression(0);
						}
						}
						setState(85);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(88);
				match(CPARAN);
				setState(89);
				match(SEMI);
				}
				break;
			case 5:
				{
				_localctx = new Create_statementContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(91);
				((Create_statementContext)_localctx).target = expression(0);
				setState(92);
				match(ASS);
				setState(93);
				match(NEW);
				setState(94);
				match(NAME);
				setState(95);
				match(OPARAN);
				setState(104);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << STRING) | (1L << THIS) | (1L << OPARAN) | (1L << NAME) | (1L << CONSTANT))) != 0)) {
					{
					setState(96);
					expression(0);
					setState(101);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(97);
						match(COMMA);
						setState(98);
						expression(0);
						}
						}
						setState(103);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(106);
				match(CPARAN);
				setState(107);
				match(SEMI);
				}
				break;
			case 6:
				{
				_localctx = new Debug_statementContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(109);
				match(BREAKPOINT);
				setState(114);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OPARAN) {
					{
					setState(110);
					match(OPARAN);
					setState(111);
					expression(0);
					setState(112);
					match(CPARAN);
					}
				}

				setState(116);
				match(SEMI);
				}
				break;
			case 7:
				{
				_localctx = new Output_statementContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(117);
				match(PRINTLN);
				setState(118);
				match(OPARAN);
				setState(119);
				expression(0);
				setState(120);
				match(CPARAN);
				setState(121);
				match(SEMI);
				}
				break;
			case 8:
				{
				_localctx = new Sparql_statementContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(123);
				((Sparql_statementContext)_localctx).target = expression(0);
				setState(124);
				match(ASS);
				setState(125);
				match(ACCESS);
				setState(126);
				match(OPARAN);
				setState(127);
				((Sparql_statementContext)_localctx).query = expression(0);
				setState(137);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(128);
					match(COMMA);
					setState(129);
					expression(0);
					setState(134);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==COMMA) {
						{
						{
						setState(130);
						match(COMMA);
						setState(131);
						expression(0);
						}
						}
						setState(136);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				setState(139);
				match(CPARAN);
				setState(140);
				match(SEMI);
				}
				break;
			case 9:
				{
				_localctx = new Owl_statementContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(142);
				((Owl_statementContext)_localctx).target = expression(0);
				setState(143);
				match(ASS);
				setState(144);
				match(DERIVE);
				setState(145);
				match(OPARAN);
				setState(146);
				((Owl_statementContext)_localctx).query = expression(0);
				setState(147);
				match(CPARAN);
				setState(148);
				match(SEMI);
				}
				break;
			case 10:
				{
				_localctx = new If_statementContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(150);
				match(IF);
				setState(151);
				expression(0);
				setState(152);
				match(THEN);
				setState(153);
				statement(0);
				setState(156);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==ELSE) {
					{
					setState(154);
					match(ELSE);
					setState(155);
					statement(0);
					}
				}

				setState(158);
				match(END);
				setState(160);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
				case 1:
					{
					setState(159);
					((If_statementContext)_localctx).next = statement(0);
					}
					break;
				}
				}
				break;
			case 11:
				{
				_localctx = new While_statementContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(162);
				match(WHILE);
				setState(163);
				expression(0);
				setState(164);
				match(DO);
				setState(165);
				statement(0);
				setState(166);
				match(END);
				setState(168);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
				case 1:
					{
					setState(167);
					((While_statementContext)_localctx).next = statement(0);
					}
					break;
				}
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(176);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,18,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Sequence_statementContext(new StatementContext(_parentctx, _parentState));
					pushNewRecursionContext(_localctx, _startState, RULE_statement);
					setState(172);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(173);
					statement(2);
					}
					} 
				}
				setState(178);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,18,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class ExpressionContext extends ParserRuleContext {
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
	 
		public ExpressionContext() { }
		public void copyFrom(ExpressionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class String_expressionContext extends ExpressionContext {
		public TerminalNode STRING() { return getToken(WhileParser.STRING, 0); }
		public String_expressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterString_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitString_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitString_expression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class External_field_expressionContext extends ExpressionContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode DOT() { return getToken(WhileParser.DOT, 0); }
		public TerminalNode NAME() { return getToken(WhileParser.NAME, 0); }
		public External_field_expressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterExternal_field_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitExternal_field_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitExternal_field_expression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class This_expressionContext extends ExpressionContext {
		public TerminalNode THIS() { return getToken(WhileParser.THIS, 0); }
		public This_expressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterThis_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitThis_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitThis_expression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class Var_expressionContext extends ExpressionContext {
		public TerminalNode NAME() { return getToken(WhileParser.NAME, 0); }
		public Var_expressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterVar_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitVar_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitVar_expression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class Plus_expressionContext extends ExpressionContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode PLUS() { return getToken(WhileParser.PLUS, 0); }
		public Plus_expressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterPlus_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitPlus_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitPlus_expression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class Geq_expressionContext extends ExpressionContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode GEQ() { return getToken(WhileParser.GEQ, 0); }
		public Geq_expressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterGeq_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitGeq_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitGeq_expression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class Minus_expressionContext extends ExpressionContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode MINUS() { return getToken(WhileParser.MINUS, 0); }
		public Minus_expressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterMinus_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitMinus_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitMinus_expression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class Const_expressionContext extends ExpressionContext {
		public TerminalNode CONSTANT() { return getToken(WhileParser.CONSTANT, 0); }
		public Const_expressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterConst_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitConst_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitConst_expression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class Nested_expressionContext extends ExpressionContext {
		public TerminalNode OPARAN() { return getToken(WhileParser.OPARAN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode CPARAN() { return getToken(WhileParser.CPARAN, 0); }
		public Nested_expressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterNested_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitNested_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitNested_expression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class Eq_expressionContext extends ExpressionContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode EQ() { return getToken(WhileParser.EQ, 0); }
		public Eq_expressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterEq_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitEq_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitEq_expression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class Field_expressionContext extends ExpressionContext {
		public TerminalNode THIS() { return getToken(WhileParser.THIS, 0); }
		public TerminalNode DOT() { return getToken(WhileParser.DOT, 0); }
		public TerminalNode NAME() { return getToken(WhileParser.NAME, 0); }
		public Field_expressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterField_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitField_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitField_expression(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class Neq_expressionContext extends ExpressionContext {
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode NEQ() { return getToken(WhileParser.NEQ, 0); }
		public Neq_expressionContext(ExpressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).enterNeq_expression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WhileListener ) ((WhileListener)listener).exitNeq_expression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof WhileVisitor ) return ((WhileVisitor<? extends T>)visitor).visitNeq_expression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		return expression(0);
	}

	private ExpressionContext expression(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExpressionContext _localctx = new ExpressionContext(_ctx, _parentState);
		ExpressionContext _prevctx = _localctx;
		int _startState = 10;
		enterRecursionRule(_localctx, 10, RULE_expression, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(191);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
			case 1:
				{
				_localctx = new This_expressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(180);
				match(THIS);
				}
				break;
			case 2:
				{
				_localctx = new Field_expressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(181);
				match(THIS);
				setState(182);
				match(DOT);
				setState(183);
				match(NAME);
				}
				break;
			case 3:
				{
				_localctx = new Var_expressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(184);
				match(NAME);
				}
				break;
			case 4:
				{
				_localctx = new Const_expressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(185);
				match(CONSTANT);
				}
				break;
			case 5:
				{
				_localctx = new String_expressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(186);
				match(STRING);
				}
				break;
			case 6:
				{
				_localctx = new Nested_expressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(187);
				match(OPARAN);
				setState(188);
				expression(0);
				setState(189);
				match(CPARAN);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(213);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,21,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(211);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
					case 1:
						{
						_localctx = new Plus_expressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(193);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(194);
						match(PLUS);
						setState(195);
						expression(7);
						}
						break;
					case 2:
						{
						_localctx = new Minus_expressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(196);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(197);
						match(MINUS);
						setState(198);
						expression(6);
						}
						break;
					case 3:
						{
						_localctx = new Eq_expressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(199);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(200);
						match(EQ);
						setState(201);
						expression(5);
						}
						break;
					case 4:
						{
						_localctx = new Neq_expressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(202);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(203);
						match(NEQ);
						setState(204);
						expression(4);
						}
						break;
					case 5:
						{
						_localctx = new Geq_expressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(205);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(206);
						match(GEQ);
						setState(207);
						expression(3);
						}
						break;
					case 6:
						{
						_localctx = new External_field_expressionContext(new ExpressionContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(208);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(209);
						match(DOT);
						setState(210);
						match(NAME);
						}
						break;
					}
					} 
				}
				setState(215);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,21,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 4:
			return statement_sempred((StatementContext)_localctx, predIndex);
		case 5:
			return expression_sempred((ExpressionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean statement_sempred(StatementContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean expression_sempred(ExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 1:
			return precpred(_ctx, 6);
		case 2:
			return precpred(_ctx, 5);
		case 3:
			return precpred(_ctx, 4);
		case 4:
			return precpred(_ctx, 3);
		case 5:
			return precpred(_ctx, 2);
		case 6:
			return precpred(_ctx, 10);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3,\u00db\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\3\2\3\2\3\2\7\2\22\n\2\f\2\16\2"+
		"\25\13\2\3\3\6\3\30\n\3\r\3\16\3\31\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\5"+
		"\4$\n\4\3\4\3\4\5\4(\n\4\3\4\3\4\7\4,\n\4\f\4\16\4/\13\4\3\4\3\4\3\5\3"+
		"\5\3\5\5\5\66\n\5\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6"+
		"\3\6\3\6\3\6\3\6\3\6\3\6\5\6K\n\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\7\6T\n\6"+
		"\f\6\16\6W\13\6\5\6Y\n\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\7"+
		"\6f\n\6\f\6\16\6i\13\6\5\6k\n\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\5\6u\n"+
		"\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\7\6"+
		"\u0087\n\6\f\6\16\6\u008a\13\6\5\6\u008c\n\6\3\6\3\6\3\6\3\6\3\6\3\6\3"+
		"\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\5\6\u009f\n\6\3\6\3\6\5\6\u00a3"+
		"\n\6\3\6\3\6\3\6\3\6\3\6\3\6\5\6\u00ab\n\6\5\6\u00ad\n\6\3\6\3\6\7\6\u00b1"+
		"\n\6\f\6\16\6\u00b4\13\6\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3"+
		"\7\5\7\u00c2\n\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3"+
		"\7\3\7\3\7\3\7\3\7\7\7\u00d6\n\7\f\7\16\7\u00d9\13\7\3\7\2\4\n\f\b\2\4"+
		"\6\b\n\f\2\2\2\u00fb\2\16\3\2\2\2\4\27\3\2\2\2\6\37\3\2\2\2\b\62\3\2\2"+
		"\2\n\u00ac\3\2\2\2\f\u00c1\3\2\2\2\16\23\7+\2\2\17\20\7*\2\2\20\22\7+"+
		"\2\2\21\17\3\2\2\2\22\25\3\2\2\2\23\21\3\2\2\2\23\24\3\2\2\2\24\3\3\2"+
		"\2\2\25\23\3\2\2\2\26\30\5\6\4\2\27\26\3\2\2\2\30\31\3\2\2\2\31\27\3\2"+
		"\2\2\31\32\3\2\2\2\32\33\3\2\2\2\33\34\7\32\2\2\34\35\5\n\6\2\35\36\7"+
		"\33\2\2\36\5\3\2\2\2\37 \7$\2\2 #\7+\2\2!\"\7&\2\2\"$\7+\2\2#!\3\2\2\2"+
		"#$\3\2\2\2$%\3\2\2\2%\'\7\35\2\2&(\5\2\2\2\'&\3\2\2\2\'(\3\2\2\2()\3\2"+
		"\2\2)-\7\36\2\2*,\5\b\5\2+*\3\2\2\2,/\3\2\2\2-+\3\2\2\2-.\3\2\2\2.\60"+
		"\3\2\2\2/-\3\2\2\2\60\61\7%\2\2\61\7\3\2\2\2\62\63\7+\2\2\63\65\7\35\2"+
		"\2\64\66\5\2\2\2\65\64\3\2\2\2\65\66\3\2\2\2\66\67\3\2\2\2\678\7\36\2"+
		"\289\5\n\6\29:\7%\2\2:\t\3\2\2\2;<\b\6\1\2<=\7\t\2\2=\u00ad\7\23\2\2>"+
		"?\5\f\7\2?@\7\21\2\2@A\5\f\7\2AB\7\23\2\2B\u00ad\3\2\2\2CD\7\20\2\2DE"+
		"\5\f\7\2EF\7\23\2\2F\u00ad\3\2\2\2GH\5\f\7\2HI\7\21\2\2IK\3\2\2\2JG\3"+
		"\2\2\2JK\3\2\2\2KL\3\2\2\2LM\5\f\7\2MN\7\22\2\2NO\7+\2\2OX\7\35\2\2PU"+
		"\5\f\7\2QR\7*\2\2RT\5\f\7\2SQ\3\2\2\2TW\3\2\2\2US\3\2\2\2UV\3\2\2\2VY"+
		"\3\2\2\2WU\3\2\2\2XP\3\2\2\2XY\3\2\2\2YZ\3\2\2\2Z[\7\36\2\2[\\\7\23\2"+
		"\2\\\u00ad\3\2\2\2]^\5\f\7\2^_\7\21\2\2_`\7\27\2\2`a\7+\2\2aj\7\35\2\2"+
		"bg\5\f\7\2cd\7*\2\2df\5\f\7\2ec\3\2\2\2fi\3\2\2\2ge\3\2\2\2gh\3\2\2\2"+
		"hk\3\2\2\2ig\3\2\2\2jb\3\2\2\2jk\3\2\2\2kl\3\2\2\2lm\7\36\2\2mn\7\23\2"+
		"\2n\u00ad\3\2\2\2ot\7)\2\2pq\7\35\2\2qr\5\f\7\2rs\7\36\2\2su\3\2\2\2t"+
		"p\3\2\2\2tu\3\2\2\2uv\3\2\2\2v\u00ad\7\23\2\2wx\7#\2\2xy\7\35\2\2yz\5"+
		"\f\7\2z{\7\36\2\2{|\7\23\2\2|\u00ad\3\2\2\2}~\5\f\7\2~\177\7\21\2\2\177"+
		"\u0080\7\'\2\2\u0080\u0081\7\35\2\2\u0081\u008b\5\f\7\2\u0082\u0083\7"+
		"*\2\2\u0083\u0088\5\f\7\2\u0084\u0085\7*\2\2\u0085\u0087\5\f\7\2\u0086"+
		"\u0084\3\2\2\2\u0087\u008a\3\2\2\2\u0088\u0086\3\2\2\2\u0088\u0089\3\2"+
		"\2\2\u0089\u008c\3\2\2\2\u008a\u0088\3\2\2\2\u008b\u0082\3\2\2\2\u008b"+
		"\u008c\3\2\2\2\u008c\u008d\3\2\2\2\u008d\u008e\7\36\2\2\u008e\u008f\7"+
		"\23\2\2\u008f\u00ad\3\2\2\2\u0090\u0091\5\f\7\2\u0091\u0092\7\21\2\2\u0092"+
		"\u0093\7(\2\2\u0093\u0094\7\35\2\2\u0094\u0095\5\f\7\2\u0095\u0096\7\36"+
		"\2\2\u0096\u0097\7\23\2\2\u0097\u00ad\3\2\2\2\u0098\u0099\7\24\2\2\u0099"+
		"\u009a\5\f\7\2\u009a\u009b\7\26\2\2\u009b\u009e\5\n\6\2\u009c\u009d\7"+
		"\30\2\2\u009d\u009f\5\n\6\2\u009e\u009c\3\2\2\2\u009e\u009f\3\2\2\2\u009f"+
		"\u00a0\3\2\2\2\u00a0\u00a2\7%\2\2\u00a1\u00a3\5\n\6\2\u00a2\u00a1\3\2"+
		"\2\2\u00a2\u00a3\3\2\2\2\u00a3\u00ad\3\2\2\2\u00a4\u00a5\7\31\2\2\u00a5"+
		"\u00a6\5\f\7\2\u00a6\u00a7\7\32\2\2\u00a7\u00a8\5\n\6\2\u00a8\u00aa\7"+
		"%\2\2\u00a9\u00ab\5\n\6\2\u00aa\u00a9\3\2\2\2\u00aa\u00ab\3\2\2\2\u00ab"+
		"\u00ad\3\2\2\2\u00ac;\3\2\2\2\u00ac>\3\2\2\2\u00acC\3\2\2\2\u00acJ\3\2"+
		"\2\2\u00ac]\3\2\2\2\u00aco\3\2\2\2\u00acw\3\2\2\2\u00ac}\3\2\2\2\u00ac"+
		"\u0090\3\2\2\2\u00ac\u0098\3\2\2\2\u00ac\u00a4\3\2\2\2\u00ad\u00b2\3\2"+
		"\2\2\u00ae\u00af\f\3\2\2\u00af\u00b1\5\n\6\4\u00b0\u00ae\3\2\2\2\u00b1"+
		"\u00b4\3\2\2\2\u00b2\u00b0\3\2\2\2\u00b2\u00b3\3\2\2\2\u00b3\13\3\2\2"+
		"\2\u00b4\u00b2\3\2\2\2\u00b5\u00b6\b\7\1\2\u00b6\u00c2\7\34\2\2\u00b7"+
		"\u00b8\7\34\2\2\u00b8\u00b9\7\22\2\2\u00b9\u00c2\7+\2\2\u00ba\u00c2\7"+
		"+\2\2\u00bb\u00c2\7,\2\2\u00bc\u00c2\7\3\2\2\u00bd\u00be\7\35\2\2\u00be"+
		"\u00bf\5\f\7\2\u00bf\u00c0\7\36\2\2\u00c0\u00c2\3\2\2\2\u00c1\u00b5\3"+
		"\2\2\2\u00c1\u00b7\3\2\2\2\u00c1\u00ba\3\2\2\2\u00c1\u00bb\3\2\2\2\u00c1"+
		"\u00bc\3\2\2\2\u00c1\u00bd\3\2\2\2\u00c2\u00d7\3\2\2\2\u00c3\u00c4\f\b"+
		"\2\2\u00c4\u00c5\7\37\2\2\u00c5\u00d6\5\f\7\t\u00c6\u00c7\f\7\2\2\u00c7"+
		"\u00c8\7 \2\2\u00c8\u00d6\5\f\7\b\u00c9\u00ca\f\6\2\2\u00ca\u00cb\7\n"+
		"\2\2\u00cb\u00d6\5\f\7\7\u00cc\u00cd\f\5\2\2\u00cd\u00ce\7\13\2\2\u00ce"+
		"\u00d6\5\f\7\6\u00cf\u00d0\f\4\2\2\u00d0\u00d1\7\17\2\2\u00d1\u00d6\5"+
		"\f\7\5\u00d2\u00d3\f\f\2\2\u00d3\u00d4\7\22\2\2\u00d4\u00d6\7+\2\2\u00d5"+
		"\u00c3\3\2\2\2\u00d5\u00c6\3\2\2\2\u00d5\u00c9\3\2\2\2\u00d5\u00cc\3\2"+
		"\2\2\u00d5\u00cf\3\2\2\2\u00d5\u00d2\3\2\2\2\u00d6\u00d9\3\2\2\2\u00d7"+
		"\u00d5\3\2\2\2\u00d7\u00d8\3\2\2\2\u00d8\r\3\2\2\2\u00d9\u00d7\3\2\2\2"+
		"\30\23\31#\'-\65JUXgjt\u0088\u008b\u009e\u00a2\u00aa\u00ac\u00b2\u00c1"+
		"\u00d5\u00d7";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}