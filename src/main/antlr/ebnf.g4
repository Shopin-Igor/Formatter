// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 1, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine false, allowShortBlocksOnASingleLine true, alignSemicolons hanging, alignColons hanging

grammar ebnf;

// -------------------------------------------------------------------
// Rule file structure
// -------------------------------------------------------------------

rulelist
    : rule_* EOF
    ;

// <RuleName> ::= <pattern> ;
rule_
    : ruleName EBNF_ASSIGN pattern SEMI
    ;

// Rule name is in angle brackets: <ifStmt>
ruleName
    : LT IDENT GT
    ;

// -------------------------------------------------------------------
// Pattern language (matches JavaParser AST nodes, not Java tokens)
// -------------------------------------------------------------------

// Alternation: A | B | C
pattern
    : alternative (PIPE alternative)*
    ;

// Sequence of atoms (space-separated). Useful for matching NodeList patterns etc.
alternative
    : sequence?
    ;

sequence
    : atom+
    ;

// Postfix quantifiers: ?, *, +
atom
    : primary quantifier?
    ;

quantifier
    : QMARK
    | STAR
    | PLUS
    ;

// Primaries
primary
    : nodePattern
    | ruleRef
    | listPattern
    | group
    | literal
    ;

// Reference to another rule: <expr>
ruleRef
    : LT IDENT GT
    ;

// Grouping: ( ... )
group
    : LPAREN pattern RPAREN
    ;

// List: [ a, b, c ]
// Can combine with quantifiers on contained items: [ <stmt>* ]
listPattern
    : LBRACK (pattern (COMMA pattern)*)? RBRACK
    ;

// Node pattern: IfStmt(...) or just IfStmt
// TypeName should match JavaParser node simple class names: IfStmt, BlockStmt, BinaryExpr, NameExpr...
nodePattern
    : typeName (LPAREN fieldAssignments? RPAREN)?
    ;

typeName
    : IDENT
    ;

// Fields: condition = <expr>, elseStmt? = BlockStmt(...)
// Optional field match is written as: fieldName? = pattern
fieldAssignments
    : fieldAssignment (COMMA fieldAssignment)*
    ;

fieldAssignment
    : fieldName QMARK? EQ pattern
    ;

fieldName
    : IDENT
    ;

// Literals: for matching simple properties (operator, names, booleans, null)
literal
    : STRING
    | NUMBER
    | TRUE
    | FALSE
    | NULL
    | IDENT
    ;

// -------------------------------------------------------------------
// Lexer
// -------------------------------------------------------------------

EBNF_ASSIGN : '::=';

SEMI  : ';';
COMMA : ',';
EQ    : '=';

PIPE  : '|';

QMARK : '?';
STAR  : '*';
PLUS  : '+';

LPAREN : '(';
RPAREN : ')';

LBRACK : '[';
RBRACK : ']';

LT : '<';
GT : '>';

TRUE : 'true';
FALSE: 'false';
NULL : 'null';

// IDENT supports underscores
IDENT
    : [a-zA-Z_] [a-zA-Z0-9_]*
    ;

// "string"
STRING
    : '"' ( '\\' . | ~["\\] )* '"'
    ;

NUMBER
    : [0-9]+ ('.' [0-9]+)?
    ;

WS
    : [ \t\r\n]+ -> skip
    ;

LINE_COMMENT
    : '//' ~[\r\n]* -> skip
    ;

BLOCK_COMMENT
    : '/*' .*? '*/' -> skip
    ;
