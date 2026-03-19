// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 1, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine false, allowShortBlocksOnASingleLine true, alignSemicolons hanging, alignColons hanging

grammar ebnf;

// -------------------------------------------------------------------
// Rule file structure
// -------------------------------------------------------------------

rulelist
    : rule_* EOF
    ;

// <RuleName> ::= <pattern> => <formatExpr>;
rule_
    : ruleName EBNF_ASSIGN pattern FORMAT_ASSIGN formatExpr SEMI
    ;

formatExpr
    : formatAtom+
    ;

formatAtom
    : textLiteral
    | placeholder
    | formatDirective
    | formatGroup
    | conditionalFormat
    | joinFormat
    ;

textLiteral
    : STRING
    ;

placeholder
    : LT refName GT
    ;

formatDirective
    : SP
    | NL
    | INDENT
    | DEDENT
    ;

formatGroup
    : LPAREN formatExpr RPAREN
    ;

conditionalFormat
    : IFPRESENT LPAREN refName COMMA formatExpr RPAREN
    ;

joinFormat
    : JOIN LPAREN placeholder COMMA separatorExpr RPAREN
    ;

separatorExpr
    : separatorAtom+
    ;

separatorAtom
    : textLiteral
    | formatDirective
    | formatGroup
    ;

// For ex.: <ifStmt>
ruleName
    : LT refName GT
    ;

refName
    : IDENT (QMARK | STAR | PLUS)?
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
    : LT refName GT
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
fragment A : [aA];
fragment B : [bB];
fragment C : [cC];
fragment D : [dD];
fragment E : [eE];
fragment F : [fF];
fragment G : [gG];
fragment H : [hH];
fragment I : [iI];
fragment J : [jJ];
fragment K : [kK];
fragment L : [lL];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment P : [pP];
fragment Q : [qQ];
fragment R : [rR];
fragment S : [sS];
fragment T : [tT];
fragment U : [uU];
fragment V : [vV];
fragment W : [wW];
fragment X : [xX];
fragment Y : [yY];
fragment Z : [zZ];

EBNF_ASSIGN : '::=';
FORMAT_ASSIGN : '=>';

IFPRESENT : I F P R E S E N T;
JOIN      : J O I N;
SP     : S P;
NL     : N L;
INDENT : I N D E N T;
DEDENT : D E D E N T;


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

TRUE  : T R U E;
FALSE : F A L S E;
NULL  : N U L L;

IDENT
    : [a-zA-Z_] [a-zA-Z0-9_]*
    ;

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
