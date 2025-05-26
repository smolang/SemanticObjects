grammar While;
/**
TODO: casts, unit, constraints on generics, drop special treatment of atomic types, FMU state copies
**/
@header {
package no.uio.microobject.antlr;
}
//Strings
STRING : '"' ('\\"'|.)*? '"' ;

//Whitespace and comments
WS           : [ \t\r\n\u000C]+ -> channel(HIDDEN);
COMMENT      : '/*' .*? '*/' -> channel(HIDDEN) ;
LINE_COMMENT : '//' ~[\r\n]* -> channel(HIDDEN) ;

//Keywords: statements
SKIP_S : 'skip';
RETURN : 'return';
IF : 'if';
THEN : 'then';
NEW : 'new';
ELSE : 'else';
WHILE : 'while';
DO : 'do';
PRINTLN : 'print';
END : 'end';
ACCESS : 'access';
CONSTRUCT : 'construct';
MEMBER : 'member';
SIMULATE : 'simulate';
VALIDATE : 'validate';
CLASSIFY : 'classify';
ADAPT : 'adapt';
TICK : 'tick';
BREAKPOINT : 'breakpoint';
SUPER : 'super';
DESTROY : 'destroy';
ABSTRACT : 'abstract'; //'abstract' collides with Java

//Keywords: classes and methods
CLASS : 'class';
EXTENDS : 'extends';
RULE : 'rule';
OVERRIDE : 'override';
MAIN : 'main';
HIDE : 'hidden';
MODELS : 'models';
CLASSIFIES: 'classifies';
RETRIEVES: 'retrieves';
DOMAIN : 'domain';
CONTEXT : 'context';

//Keywords: constants
TRUE : 'True';
FALSE : 'False';
NULL : 'null';
THIS: 'this';
UNIT: 'unit';

//Keywords: operators
EQ : '==';
NEQ : '!=';
LT : '<';
GT : '>';
LEQ : '<=';
GEQ : '>=';
ASS : '=';
PLUS : '+';
PLUSPLUS : '++';
MULT : '*';
MINUS : '-';
DIV : '/';
MOD : '%';
AND : '&';
OR : '|';
NOT : '!';

//Keywords: conversions
INTTOSTRING :    'intToString';
DOUBLETOSTRING : 'doubleToString';
BOOLEANTOSTRING: 'booleanToString';
INTTODOUBLE : 'intToDouble';
DOUBLETOINT : 'doubleToInt';

//Keywords: others
DOT : '.';
SEMI : ';';
OPARAN : '(';
CPARAN : ')';
OBRACK : '[';
CBRACK : ']';
COMMA : ',';
FMU : 'FMO';
PORT : 'port';
SPARQLMODE : 'SPARQL';
INFLUXMODE : 'INFLUXDB';
// Note that the IN, OUT constants are also used in
// TypeChecker.kt:translateType in the FMU branch; adapt strings there if
// changing the syntax here
IN : 'in';
OUT : 'out';

//Names etc.
fragment DIG : [0-9];
fragment LET : [a-zA-Z_];
fragment LOD : LET | DIG;
fragment EXPONENT : ('e' | 'E' | 'e+' | 'E+' | 'e-' | 'E-') DIG+;
NAME : LET LOD*;
// Note that this makes the grammar whitespace-sensitive -- in order to allow
// "x-1" and not only "x - 1", add a negation unary operator '-'.
INTEGER :  '0' | '-'? [1-9] DIG*;
FLOAT : INTEGER? '.' DIG+ EXPONENT? ;

namelist : NAME (COMMA NAME)*;

//Entry point
program : (class_def)* MAIN statement END (class_def)*;

//classes
class_def : (abs=ABSTRACT)? (hidden=HIDE)? CLASS  className = NAME (LT namelist GT)? (EXTENDS superType = type)? OPARAN (external=fieldDeclList)? CPARAN
            (internal = fieldDeclInitList)?
            (models_block)?
            (classifies_block (retrieves_block)?)?
            method_def*
            END;
method_def :  (abs=ABSTRACT)? (builtinrule=RULE)? (domainrule=DOMAIN)? (overriding=OVERRIDE)? type NAME OPARAN paramList? CPARAN (statement END)?;

models_block : MODELS owldescription=STRING SEMI                                                    #simple_models_block
             | MODELS OPARAN guard=expression CPARAN owldescription=STRING SEMI models_block        #complex_models_block
             ;
classifies_block : CLASSIFIES owldescription=STRING SEMI                 # adaptation_classifies_block
             ;
retrieves_block : RETRIEVES  selectquery=STRING SEMI                 # adaptation_retrieves_block
              ;
//Statements
statement :   SKIP_S SEMI                                                                                                                               # skip_statment
            | ((declType = type)? target=expression ASS)? CLASSIFY OPARAN context=expression CPARAN SEMI                                     # classify_statement
			| ADAPT OPARAN adapter=expression CPARAN SEMI                                                                      # adapt_statement
			| (declType = type)? expression ASS expression SEMI                                                                                         # assign_statement
			| ((declType = type)? target=expression ASS)? SUPER OPARAN (expression (COMMA expression)*)? CPARAN SEMI                                    # super_statement
			| RETURN expression SEMI                                                                                                                    # return_statement
			| fmu=expression DOT TICK OPARAN time=expression CPARAN SEMI                                                                                # tick_statement
			| ((declType = type)? target=expression ASS)? expression DOT NAME OPARAN (expression (COMMA expression)*)? CPARAN SEMI                      # call_statement
        // TODO: allow new statements without assignment
			| (declType = type)? target=expression ASS NEW newType = type OPARAN (expression (COMMA expression)*)? CPARAN (MODELS owldescription = expression)? SEMI                         # create_statement
			| BREAKPOINT SEMI                                                                                                                           # debug_statement
			| PRINTLN OPARAN expression CPARAN SEMI                                                                                                     # output_statement
			| DESTROY OPARAN expression CPARAN SEMI                                                                                                     # destroy_statement
			| (declType = type)? target=expression ASS ACCESS OPARAN query=expression (COMMA lang=modeexpression)? (COMMA expression)* CPARAN SEMI      # sparql_statement
			| (declType = type)? target=expression ASS CONSTRUCT OPARAN query=expression (COMMA expression)* CPARAN SEMI                                # construct_statement
			| (declType = type)? target=expression ASS MEMBER OPARAN query=expression CPARAN SEMI                                                       # owl_statement
			| (declType = type)? target=expression ASS VALIDATE OPARAN query=expression CPARAN SEMI                                                     # validate_statement
			| (declType = type)? target=expression ASS SIMULATE OPARAN path=STRING (COMMA varInitList)? CPARAN SEMI                                     # simulate_statement
			| IF expression THEN thenS=statement (ELSE elseE=statement)? END next=statement?                                                            # if_statement
            | WHILE expression DO statement END next=statement?                                                                                         # while_statement
            | statement statement                                                                                                                       # sequence_statement
            ;

modeexpression : SPARQLMODE                             #sparql_mode
               | INFLUXMODE OPARAN expression CPARAN    #influx_mode
               ;
//Expressions
expression :      THIS                           # this_expression
                | THIS DOT NAME                  # field_expression
                | NAME                           # var_expression
                | INTEGER                        # integer_expression
                | TRUE                           # true_expression
                | FALSE                          # false_expression
                | STRING                         # string_expression
                | FLOAT                          # double_expression
                | NULL                           # null_expression
                | UNIT                           # unit_expression
                | conversion OPARAN expression CPARAN # conversion_expression
                | expression DOT PORT OPARAN STRING CPARAN	# fmu_field_expression
                | expression DOT NAME			 # external_field_expression
                | expression DIV expression      # div_expression
                | expression MOD expression      # mod_expression
                | expression MULT expression     # mult_expression
                | expression PLUS expression     # plus_expression
                | expression MINUS expression    # minus_expression
                | expression PLUSPLUS expression # concat_expression
                | expression EQ expression       # eq_expression
                | expression NEQ expression      # neq_expression
                | expression GEQ expression      # geq_expression
                | expression LEQ expression      # leq_expression
                | expression GT expression       # gt_expression
                | expression LT expression       # lt_expression
                | expression AND expression      # and_expression
                | expression OR expression       # or_expression
                | NOT expression                 # not_expression
                | OPARAN expression CPARAN       # nested_expression
                ;

type : NAME                                                    #simple_type
     | NAME LT typelist GT                                     #nested_type
     | FMU OBRACK fmuParamList? CBRACK                         #fmu_type
     ;
typelist : type (COMMA type)*;
param : type NAME;
paramList : param (COMMA param)*;
fmuparam : direction=(IN | OUT) param;
fmuParamList : fmuparam (COMMA fmuparam)*;
fieldDecl : (hidden=HIDE | domain=DOMAIN | context=CONTEXT)? type NAME;
fieldDeclList : fieldDecl (COMMA fieldDecl)*;
fieldDeclInit : (hidden=HIDE | domain=DOMAIN)? type NAME ASS expression SEMI;
fieldDeclInitList : fieldDeclInit fieldDeclInit*;
varInit : NAME ASS expression;
varInitList : varInit (COMMA varInit)*;
conversion: INTTOSTRING | DOUBLETOSTRING | BOOLEANTOSTRING | INTTODOUBLE | DOUBLETOINT;
