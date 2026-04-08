grammar GOAL;

@header {
package org.vnu.sme.goal.parser;
}

/* =========================================================
 * Parser Rules
 * ========================================================= */

goalModel
    : ISTAR IDENT (actor | dependencyDefinition)* EOF
    ;

/* -------------------------
 * Actor hierarchy
 * ------------------------- */
actor
    : actorDefinition
    | agentDefinition
    | roleDefinition
    ;

actorDefinition
    : ACTOR IDENT (COLON IDENT)? (GT IDENT)? LBRACE actorBody RBRACE
    ;

agentDefinition
    : AGENT IDENT (COLON IDENT)? (GT IDENT)? LBRACE actorBody RBRACE
    ;

roleDefinition
    : ROLE IDENT (COLON IDENT)? (GT IDENT)? LBRACE actorBody RBRACE
    ;

actorBody
    : intentionalElement*
    ;

/* -------------------------
 * Intentional elements
 * ------------------------- */
intentionalElement
    : goalDecl
    | taskDecl
    | qualityDecl
    | resourceDecl
    ;

// Quan hệ nối tiếp ngay sau IDENT và trước LBRACE, phân tách bằng dấu phẩy
goalDecl
    : GOAL IDENT relationList? LBRACE elementBody RBRACE
    ;

taskDecl
    : TASK IDENT relationList? LBRACE elementBody RBRACE
    ;

qualityDecl
    : QUALITY IDENT relationList? LBRACE elementBody RBRACE
    ;

resourceDecl
    : RESOURCE IDENT relationList? LBRACE elementBody RBRACE
    ;

elementBody
    : descriptionClause*
    ;

/* -------------------------
 * Relations 
 * ------------------------- */
relationList
    : relation (COMMA relation)*
    ;

relation
    : relOp IDENT
    ;

relOp
    : AND_REFINE
    | OR_REFINE
    | CONTRIB_MAKE
    | CONTRIB_HELP
    | CONTRIB_HURT
    | CONTRIB_BREAK
    | QUALIFY
    | NEEDED_BY
    ;

/* -------------------------
 * Dependency
 * ------------------------- */
dependencyDefinition
    : DEPENDENCY IDENT LBRACE
        dependerClause
        dependeeClause
        dependumClause
      RBRACE
    ;

dependerClause
    : DEPENDER qualifiedName
    ;

dependeeClause
    : DEPENDEE qualifiedName
    ;

// dependum bản chất là chứa một intentionalElement bên trong (ví dụ: goal OrderAccepted {...})
dependumClause
    : DEPENDUM intentionalElement
    ;

qualifiedName
    : IDENT (DOT IDENT)*
    ;

/* -------------------------
 * Common clauses
 * ------------------------- */
// EQ (=) và SEMI (;) được cho phép tùy chọn (optional) để linh hoạt với các ví dụ trong docs
descriptionClause
    : DESCRIPTION EQ? STRING SEMI?
    ;


/* =========================================================
 * Lexer Rules
 * ========================================================= */

// Operators
AND_REFINE      : '&>' ;
OR_REFINE       : '|>' ;
CONTRIB_MAKE    : '++>' ;
CONTRIB_HELP    : '+>' ; 
CONTRIB_HURT    : '->' ;
CONTRIB_BREAK   : '-->' ;
QUALIFY         : '=>' ;
NEEDED_BY       : '<>' ;

// Separators
LBRACE          : '{' ;
RBRACE          : '}' ;
SEMI            : ';' ;
COLON           : ':' ;
GT              : '>' ;
DOT             : '.' ;
EQ              : '=' ;
COMMA           : ',' ;

// Keywords
ISTAR           : 'istar' ;
ACTOR           : 'actor' ;
AGENT           : 'agent' ;
ROLE            : 'role' ;
GOAL            : 'goal' ;
TASK            : 'task' ;
QUALITY         : 'quality' ;
RESOURCE        : 'resource' ;
DESCRIPTION     : 'description' ;
DEPENDENCY      : 'dependency' ;
DEPENDER        : 'depender' ;
DEPENDEE        : 'dependee' ;
DEPENDUM        : 'dependum' ;

// Lexical tokens
IDENT
    : [a-zA-Z_][a-zA-Z_0-9]*
    ;

STRING
    : '"' ( ~["\\] | '\\' . )* '"'
    ;

// Whitespace and comments
WS
    : [ \t\r\n\f]+ -> skip
    ;

LINE_COMMENT
    : '//' ~[\r\n]* -> skip
    ;

BLOCK_COMMENT
    : '/*' .*? '*/' -> skip
    ;