grammar SqlLikeRequestParser;

options { tokenVocab=SqlLikeRequestLexer; }

query
    : formatStmt EOF
    ;

formatStmt
    : selectStmt (FORMAT formatString)?
    ;


selectStmt
    : SELECT selectTarget (WHERE expr)?
    ;

formatString
  : formatPart*
  ;

formatPart
  : PLACEHOLDER
  | TEXT
  ;
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

