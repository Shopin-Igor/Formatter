grammar SqlLikeRequest;

// правила  Parserа

query
    : formatStmt EOF
    ;

formatStmt
    : selectStmt (FORMAT ifStatement)?
    ;

// replace
//    : ID AS REPLACEMENT
//    ;

selectStmt
    : SELECT selectTarget (WHERE expr)?
    ;

ifStatement
    : IF expr blockAssign (ELSE blockAssign)?
    ;

//blockStmt
//    : LBRACE (expr)* RBRACE
//    ;

blockAssign
    : LBRACE assignStmt* RBRACE
    ;

assignStmt
    : REPLACEMENT (DOT ID)* ASSIGN expr SEMI
    | ID (DOT ID)*          ASSIGN expr SEMI
    ;

selectTarget
    : typeName (AS REPLACEMENT)?
    ;

typeName
    : ID
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
    : (ID | REPLACEMENT) (DOT ID)*
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
FORMAT : [fF][oO][rR][mM][aA][tT];
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
