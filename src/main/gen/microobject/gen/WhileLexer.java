// Generated from /home/edkam/src/keyma/MicroObjects/src/main/resources/While.g4 by ANTLR 4.8
package microobject.gen;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class WhileLexer extends Lexer {
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
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"STRING", "WS", "COMMENT", "LINE_COMMENT", "TRUE", "FALSE", "SKIP_S", 
			"EQ", "NEQ", "LT", "GT", "LEQ", "GEQ", "RETURN", "ASS", "DOT", "SEMI", 
			"IF", "FI", "THEN", "NEW", "ELSE", "WHILE", "DO", "OD", "THIS", "OPARAN", 
			"CPARAN", "PLUS", "MINUS", "AND", "OR", "PRINTLN", "CLASS", "END", "EXTENDS", 
			"ACCESS", "DERIVE", "BREAKPOINT", "COMMA", "DIG", "LET", "LOD", "NAME", 
			"CONSTANT"
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


	public WhileLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "While.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2,\u012f\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\3\2\3\2\7\2`\n\2\f\2\16\2c\13\2\3\2\3\2\3\3\6\3h\n\3"+
		"\r\3\16\3i\3\3\3\3\3\4\3\4\3\4\3\4\7\4r\n\4\f\4\16\4u\13\4\3\4\3\4\3\4"+
		"\3\4\3\4\3\5\3\5\3\5\3\5\7\5\u0080\n\5\f\5\16\5\u0083\13\5\3\5\3\5\3\6"+
		"\3\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\t\3\t\3"+
		"\n\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r\3\r\3\16\3\16\3\16\3\17\3\17\3\17"+
		"\3\17\3\17\3\17\3\17\3\20\3\20\3\20\3\21\3\21\3\22\3\22\3\23\3\23\3\23"+
		"\3\24\3\24\3\24\3\25\3\25\3\25\3\25\3\25\3\26\3\26\3\26\3\26\3\27\3\27"+
		"\3\27\3\27\3\27\3\30\3\30\3\30\3\30\3\30\3\30\3\31\3\31\3\31\3\32\3\32"+
		"\3\32\3\33\3\33\3\33\3\33\3\33\3\34\3\34\3\35\3\35\3\36\3\36\3\37\3\37"+
		"\3 \3 \3!\3!\3\"\3\"\3\"\3\"\3\"\3\"\3#\3#\3#\3#\3#\3#\3$\3$\3$\3$\3%"+
		"\3%\3%\3%\3%\3%\3%\3%\3&\3&\3&\3&\3&\3&\3&\3\'\3\'\3\'\3\'\3\'\3\'\3\'"+
		"\3(\3(\3(\3(\3(\3(\3(\3(\3(\3(\3(\3)\3)\3*\3*\3+\3+\3,\3,\5,\u011e\n,"+
		"\3-\3-\7-\u0122\n-\f-\16-\u0125\13-\3.\6.\u0128\n.\r.\16.\u0129\3.\3."+
		"\5.\u012e\n.\4as\2/\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r"+
		"\31\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33"+
		"\65\34\67\359\36;\37= ?!A\"C#E$G%I&K\'M(O)Q*S\2U\2W\2Y+[,\3\2\6\5\2\13"+
		"\f\16\17\"\"\4\2\f\f\17\17\3\2\62;\5\2C\\aac|\2\u0134\2\3\3\2\2\2\2\5"+
		"\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2"+
		"\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33"+
		"\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2"+
		"\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2"+
		"\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2"+
		"\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2K"+
		"\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2\2\2\2Y\3\2\2\2\2[\3\2\2\2\3]\3\2"+
		"\2\2\5g\3\2\2\2\7m\3\2\2\2\t{\3\2\2\2\13\u0086\3\2\2\2\r\u008b\3\2\2\2"+
		"\17\u0091\3\2\2\2\21\u0096\3\2\2\2\23\u0098\3\2\2\2\25\u009b\3\2\2\2\27"+
		"\u009d\3\2\2\2\31\u009f\3\2\2\2\33\u00a2\3\2\2\2\35\u00a5\3\2\2\2\37\u00ac"+
		"\3\2\2\2!\u00af\3\2\2\2#\u00b1\3\2\2\2%\u00b3\3\2\2\2\'\u00b6\3\2\2\2"+
		")\u00b9\3\2\2\2+\u00be\3\2\2\2-\u00c2\3\2\2\2/\u00c7\3\2\2\2\61\u00cd"+
		"\3\2\2\2\63\u00d0\3\2\2\2\65\u00d3\3\2\2\2\67\u00d8\3\2\2\29\u00da\3\2"+
		"\2\2;\u00dc\3\2\2\2=\u00de\3\2\2\2?\u00e0\3\2\2\2A\u00e2\3\2\2\2C\u00e4"+
		"\3\2\2\2E\u00ea\3\2\2\2G\u00f0\3\2\2\2I\u00f4\3\2\2\2K\u00fc\3\2\2\2M"+
		"\u0103\3\2\2\2O\u010a\3\2\2\2Q\u0115\3\2\2\2S\u0117\3\2\2\2U\u0119\3\2"+
		"\2\2W\u011d\3\2\2\2Y\u011f\3\2\2\2[\u012d\3\2\2\2]a\7$\2\2^`\13\2\2\2"+
		"_^\3\2\2\2`c\3\2\2\2ab\3\2\2\2a_\3\2\2\2bd\3\2\2\2ca\3\2\2\2de\7$\2\2"+
		"e\4\3\2\2\2fh\t\2\2\2gf\3\2\2\2hi\3\2\2\2ig\3\2\2\2ij\3\2\2\2jk\3\2\2"+
		"\2kl\b\3\2\2l\6\3\2\2\2mn\7\61\2\2no\7,\2\2os\3\2\2\2pr\13\2\2\2qp\3\2"+
		"\2\2ru\3\2\2\2st\3\2\2\2sq\3\2\2\2tv\3\2\2\2us\3\2\2\2vw\7,\2\2wx\7\61"+
		"\2\2xy\3\2\2\2yz\b\4\2\2z\b\3\2\2\2{|\7\61\2\2|}\7\61\2\2}\u0081\3\2\2"+
		"\2~\u0080\n\3\2\2\177~\3\2\2\2\u0080\u0083\3\2\2\2\u0081\177\3\2\2\2\u0081"+
		"\u0082\3\2\2\2\u0082\u0084\3\2\2\2\u0083\u0081\3\2\2\2\u0084\u0085\b\5"+
		"\2\2\u0085\n\3\2\2\2\u0086\u0087\7v\2\2\u0087\u0088\7t\2\2\u0088\u0089"+
		"\7w\2\2\u0089\u008a\7g\2\2\u008a\f\3\2\2\2\u008b\u008c\7h\2\2\u008c\u008d"+
		"\7c\2\2\u008d\u008e\7n\2\2\u008e\u008f\7u\2\2\u008f\u0090\7g\2\2\u0090"+
		"\16\3\2\2\2\u0091\u0092\7u\2\2\u0092\u0093\7m\2\2\u0093\u0094\7k\2\2\u0094"+
		"\u0095\7r\2\2\u0095\20\3\2\2\2\u0096\u0097\7?\2\2\u0097\22\3\2\2\2\u0098"+
		"\u0099\7>\2\2\u0099\u009a\7@\2\2\u009a\24\3\2\2\2\u009b\u009c\7>\2\2\u009c"+
		"\26\3\2\2\2\u009d\u009e\7@\2\2\u009e\30\3\2\2\2\u009f\u00a0\7>\2\2\u00a0"+
		"\u00a1\7?\2\2\u00a1\32\3\2\2\2\u00a2\u00a3\7@\2\2\u00a3\u00a4\7?\2\2\u00a4"+
		"\34\3\2\2\2\u00a5\u00a6\7t\2\2\u00a6\u00a7\7g\2\2\u00a7\u00a8\7v\2\2\u00a8"+
		"\u00a9\7w\2\2\u00a9\u00aa\7t\2\2\u00aa\u00ab\7p\2\2\u00ab\36\3\2\2\2\u00ac"+
		"\u00ad\7<\2\2\u00ad\u00ae\7?\2\2\u00ae \3\2\2\2\u00af\u00b0\7\60\2\2\u00b0"+
		"\"\3\2\2\2\u00b1\u00b2\7=\2\2\u00b2$\3\2\2\2\u00b3\u00b4\7k\2\2\u00b4"+
		"\u00b5\7h\2\2\u00b5&\3\2\2\2\u00b6\u00b7\7h\2\2\u00b7\u00b8\7k\2\2\u00b8"+
		"(\3\2\2\2\u00b9\u00ba\7v\2\2\u00ba\u00bb\7j\2\2\u00bb\u00bc\7g\2\2\u00bc"+
		"\u00bd\7p\2\2\u00bd*\3\2\2\2\u00be\u00bf\7p\2\2\u00bf\u00c0\7g\2\2\u00c0"+
		"\u00c1\7y\2\2\u00c1,\3\2\2\2\u00c2\u00c3\7g\2\2\u00c3\u00c4\7n\2\2\u00c4"+
		"\u00c5\7u\2\2\u00c5\u00c6\7g\2\2\u00c6.\3\2\2\2\u00c7\u00c8\7y\2\2\u00c8"+
		"\u00c9\7j\2\2\u00c9\u00ca\7k\2\2\u00ca\u00cb\7n\2\2\u00cb\u00cc\7g\2\2"+
		"\u00cc\60\3\2\2\2\u00cd\u00ce\7f\2\2\u00ce\u00cf\7q\2\2\u00cf\62\3\2\2"+
		"\2\u00d0\u00d1\7q\2\2\u00d1\u00d2\7f\2\2\u00d2\64\3\2\2\2\u00d3\u00d4"+
		"\7v\2\2\u00d4\u00d5\7j\2\2\u00d5\u00d6\7k\2\2\u00d6\u00d7\7u\2\2\u00d7"+
		"\66\3\2\2\2\u00d8\u00d9\7*\2\2\u00d98\3\2\2\2\u00da\u00db\7+\2\2\u00db"+
		":\3\2\2\2\u00dc\u00dd\7-\2\2\u00dd<\3\2\2\2\u00de\u00df\7/\2\2\u00df>"+
		"\3\2\2\2\u00e0\u00e1\7(\2\2\u00e1@\3\2\2\2\u00e2\u00e3\7~\2\2\u00e3B\3"+
		"\2\2\2\u00e4\u00e5\7r\2\2\u00e5\u00e6\7t\2\2\u00e6\u00e7\7k\2\2\u00e7"+
		"\u00e8\7p\2\2\u00e8\u00e9\7v\2\2\u00e9D\3\2\2\2\u00ea\u00eb\7e\2\2\u00eb"+
		"\u00ec\7n\2\2\u00ec\u00ed\7c\2\2\u00ed\u00ee\7u\2\2\u00ee\u00ef\7u\2\2"+
		"\u00efF\3\2\2\2\u00f0\u00f1\7g\2\2\u00f1\u00f2\7p\2\2\u00f2\u00f3\7f\2"+
		"\2\u00f3H\3\2\2\2\u00f4\u00f5\7g\2\2\u00f5\u00f6\7z\2\2\u00f6\u00f7\7"+
		"v\2\2\u00f7\u00f8\7g\2\2\u00f8\u00f9\7p\2\2\u00f9\u00fa\7f\2\2\u00fa\u00fb"+
		"\7u\2\2\u00fbJ\3\2\2\2\u00fc\u00fd\7c\2\2\u00fd\u00fe\7e\2\2\u00fe\u00ff"+
		"\7e\2\2\u00ff\u0100\7g\2\2\u0100\u0101\7u\2\2\u0101\u0102\7u\2\2\u0102"+
		"L\3\2\2\2\u0103\u0104\7f\2\2\u0104\u0105\7g\2\2\u0105\u0106\7t\2\2\u0106"+
		"\u0107\7k\2\2\u0107\u0108\7x\2\2\u0108\u0109\7g\2\2\u0109N\3\2\2\2\u010a"+
		"\u010b\7d\2\2\u010b\u010c\7t\2\2\u010c\u010d\7g\2\2\u010d\u010e\7c\2\2"+
		"\u010e\u010f\7m\2\2\u010f\u0110\7r\2\2\u0110\u0111\7q\2\2\u0111\u0112"+
		"\7k\2\2\u0112\u0113\7p\2\2\u0113\u0114\7v\2\2\u0114P\3\2\2\2\u0115\u0116"+
		"\7.\2\2\u0116R\3\2\2\2\u0117\u0118\t\4\2\2\u0118T\3\2\2\2\u0119\u011a"+
		"\t\5\2\2\u011aV\3\2\2\2\u011b\u011e\5U+\2\u011c\u011e\5S*\2\u011d\u011b"+
		"\3\2\2\2\u011d\u011c\3\2\2\2\u011eX\3\2\2\2\u011f\u0123\5U+\2\u0120\u0122"+
		"\5W,\2\u0121\u0120\3\2\2\2\u0122\u0125\3\2\2\2\u0123\u0121\3\2\2\2\u0123"+
		"\u0124\3\2\2\2\u0124Z\3\2\2\2\u0125\u0123\3\2\2\2\u0126\u0128\5S*\2\u0127"+
		"\u0126\3\2\2\2\u0128\u0129\3\2\2\2\u0129\u0127\3\2\2\2\u0129\u012a\3\2"+
		"\2\2\u012a\u012e\3\2\2\2\u012b\u012e\5\13\6\2\u012c\u012e\5\r\7\2\u012d"+
		"\u0127\3\2\2\2\u012d\u012b\3\2\2\2\u012d\u012c\3\2\2\2\u012e\\\3\2\2\2"+
		"\13\2ais\u0081\u011d\u0123\u0129\u012d\3\2\3\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}