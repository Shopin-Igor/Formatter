grammar SqlLikeRequest;

// правила  Parserа

query
    : selectStmt EOF
    ;

selectStmt
    : SELECT selectTarget (WHERE expr)?
    ;

selectTarget
    : qualifiedName
    ;

expr
    : orExpr
    ;

orExpr
    : andExpr (OR andExpr)*
    ;

andExpr
    : notExpr (AND notExpr)*
    ;

notExpr
    : NOT notExpr
    | comparisonExpr
    ;

comparisonExpr
    : additiveExpr (compOp additiveExpr)?
    ;

additiveExpr
    : primary
    ;

primary
    : LPAREN expr RPAREN
    | literal
    | qualifiedName
    ;

compOp
    : EQ
    | NEQ
    | LT
    | LTE
    | GT
    | GTE
    ;

// Имя с точками, например IfStmt.thenStmt и т д
qualifiedName
    : ID (DOT ID)*
    ;

literal
    : STRING
    | NUMBER
    | TRUE
    | FALSE
    | NULL
    ;

// правила Lexera

SELECT : [sS][eE][lL][eE][cC][tT];
WHERE  : [wW][hH][eE][rR][eE];

AND    : [aA][nN][dD];
OR     : [oO][rR];
NOT    : [nN][oO][tT];

TRUE   : [tT][rR][uU][eE];
FALSE  : [fF][aA][lL][sS][eE];
NULL   : [nN][uU][lL][lL];

EQ     : '==';
NEQ    : '!=';
LTE    : '<=';
GTE    : '>=';
LT     : '<';
GT     : '>';

DOT    : '.';
LPAREN : '(';
RPAREN : ')';

ID
    : [a-zA-Z_][a-zA-Z0-9_]*
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
