grammar While;
/**
TODO: casts, unit, constraints on generics, drop special treatment of atomic types
**/
@header {
package antlr.microobject.gen;
}
//Strings
STRING : '"' .*? '"' ;

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
DERIVE : 'derive';
SIMULATE : 'simulate';
TICK : 'tick';
BREAKPOINT : 'breakpoint';
SUPER : 'super';

//Keywords: classes and methods
CLASS : 'class';
EXTENDS : 'extends';
RULE : 'rule';
OVERRIDE : 'override';
MAIN : 'main';

//Keywords: constants
TRUE : 'True';
FALSE : 'False';
NULL : 'null';
THIS: 'this';

//Keywords: operators
EQ : '=';
NEQ : '<>';
LT : '<';
GT : '>';
LEQ : '<=';
GEQ : '>=';
ASS : ':=';
PLUS : '+';
MULT : '*';
MINUS : '-';
AND : '&';
OR : '|';
NOT : '!';

//Keywords: others
DOT : '.';
SEMI : ';';
OPARAN : '(';
CPARAN : ')';
OBRACK : '[';
CBRACK : ']';
COMMA : ',';

//Names etc.
fragment DIG : [0-9];
fragment LET : [a-zA-Z_];
fragment LOD : LET | DIG;
NAME : LET LOD*;
CONSTANT :  DIG+;

namelist : NAME (COMMA NAME)*;

//Entry point
program : (class_def)* MAIN statement END;

//classes
class_def : CLASS (LT namelist GT)? NAME (EXTENDS NAME)? OPARAN paramList? CPARAN  method_def* END;
method_def : (builtinrule=RULE)? (overriding=OVERRIDE)? type NAME OPARAN paramList? CPARAN statement END;

//Statements
statement :   SKIP_S SEMI                                                                                                                               # skip_statment
			| (declType = type)? expression ASS expression SEMI                                                                                         # assign_statement
			| ((declType = type)? target=expression ASS)? SUPER OPARAN (expression (COMMA expression)*)? CPARAN SEMI                                    # super_statement
			| RETURN expression SEMI                                                                                                                    # return_statement
			| ((declType = type)? target=expression ASS)? expression DOT NAME OPARAN (expression (COMMA expression)*)? CPARAN SEMI                      # call_statement
			| (declType = type)? target=expression ASS NEW NAME (LT namelist GT)? OPARAN (expression (COMMA expression)*)? CPARAN SEMI                  # create_statement
			| BREAKPOINT (OPARAN expression CPARAN)? SEMI                                                                                               # debug_statement
			| PRINTLN OPARAN expression CPARAN SEMI                                                                                                     # output_statement
			| (declType = type)? target=expression ASS ACCESS OPARAN query=expression (COMMA expression (COMMA expression)*)? CPARAN SEMI               # sparql_statement
			| (declType = type)? target=expression ASS DERIVE OPARAN query=expression CPARAN SEMI                                                       # owl_statement
			| (declType = type)? target=expression ASS SIMULATE OPARAN path=STRING (COMMA varInitList)? CPARAN SEMI                                     # simulate_statement
			| TICK OPARAN fmu=expression COMMA time=expression CPARAN SEMI                                                                              # tick_statement
			| IF expression THEN statement (ELSE statement)? END next=statement?                                                                        # if_statement
            | WHILE expression DO statement END next=statement?                                                                                         # while_statement
            | statement statement                                                                                                                       # sequence_statement
            ;


//Expressions
expression :      THIS                           # this_expression
                | THIS DOT NAME                  # field_expression
                | NAME                           # var_expression
                | CONSTANT                       # const_expression
                | TRUE                           # true_expression
                | FALSE                          # false_expression
                | STRING                         # string_expression
                | NULL                           # null_expression
                | expression DOT NAME			 # external_field_expression
                | expression PLUS expression     # plus_expression
                | expression MINUS expression    # minus_expression
                | expression MULT expression     # mult_expression
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
     | NAME OBRACK in=paramList? SEMI out=paramList? CBRACK    #fmu_type
     ;
typelist : type (COMMA type)*;
param : type NAME;
paramList : param (COMMA param)*;
varInit : NAME ASS expression;
varInitList : varInit (COMMA varInit)*;