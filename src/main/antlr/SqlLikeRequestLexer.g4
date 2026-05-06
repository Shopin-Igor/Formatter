lexer grammar SqlLikeRequestLexer;


SELECT : [sS][eE][lL][eE][cC][tT];
WHERE  : [wW][hH][eE][rR][eE];
FORMAT : [fF][oO][rR][mM][aA][tT] -> pushMode(FORMAT_MODE);
IF     : [iI][fF];
ELSE   : [eE][lL][sS][eE];
AS     : [aA][sS];

AND    : [aA][nN][dD];
OR     : [oO][rR];
NOT    : [nN][oO][tT];

TRUE   : [tT][rR][uU][eE];
FALSE  : [fF][aA][lL][sS][eE];
NULL   : [nN][uU][lL][lL];

EQ     : '==';
ASSIGN : '=' ;
NEQ    : '!=';
LTE    : '<=';
GTE    : '>=';
LT     : '<';
GT     : '>';
SEMI   : ';';

DOT    : '.';
LPAREN : '(';
RPAREN : ')';
LBRACE : '{' ;
RBRACE : '}' ;

ID
    : [a-zA-Z_][a-zA-Z0-9_]*
    ;

REPLACEMENT
    : '$'[a-zA-Z_][a-zA-Z0-9_]*
    ;

NUMBER
    : [0-9]+ ('.' [0-9]+)?
    ;

// строка или строка с \... внутри
STRING
    : '"' ( '\\' . | ~["\\] )* '"'
    | '\'' ( '\\' . | ~['\\] )* '\''
    ;

WS
    : [ \t\r\n]+ -> skip
    ;

mode FORMAT_MODE;
PLACEHOLDER
    : '$' [a-zA-Z_][a-zA-Z0-9_]* ('.' [a-zA-Z_][a-zA-Z0-9_]*)*
    ;
TEXT
    : ~[$]+
    ;