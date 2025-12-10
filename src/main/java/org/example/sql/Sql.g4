grammar Sql;

@header {
    package org.example.sql;
}

parse
    : statement EOF
    ;

statement
    : selectStmt
    ;

selectStmt
    : SELECT selectElements FROM tableName (WHERE whereExpr)? # SelectSmt
    ;

selectElements
    : STAR                               # selectAll
    | columnName (COMMA columnName)*     # selectColumns
    ;

whereExpr
    : condition
    ;

condition
    : columnName comparisonOperator literalValue
    ;

comparisonOperator
    : EQ
    | LT
    | GT
    ;

// "лексические" правила (токены)

tableName   : IDENTIFIER ;
columnName  : IDENTIFIER ;
literalValue: STRING | NUMBER ;

SELECT  : [sS][eE][lL][eE][cC][tT] ;
FROM    : [fF][rR][oO][mM] ;
WHERE   : [wW][hH][eE][rR][eE] ;

STAR    : '*' ;
COMMA   : ',' ;
EQ      : '=' ;
LT      : '<' ;
GT      : '>' ;

NUMBER  : [0-9]+ ;

STRING
    : '\'' (~['\\] | '\\' .)* '\''
    ;

IDENTIFIER
    : [a-zA-Z_][a-zA-Z_0-9]*
    ;

WS
    : [ \t\r\n]+ -> skip
    ;
