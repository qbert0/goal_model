grammar GOAL;

@header {
package org.vnu.sme.goal.parser;

import org.tzi.use.parser.ocl.ASTType;
import org.tzi.use.uml.ocl.type.Type;
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
    : GOAL IDENT relationList? LBRACE goalBody RBRACE
    ;

taskDecl
    : TASK IDENT relationList? LBRACE taskBody RBRACE
    ;

qualityDecl
    : QUALITY IDENT relationList? LBRACE elementBody RBRACE
    ;

resourceDecl
    : RESOURCE IDENT relationList? LBRACE elementBody RBRACE
    ;

/* -------------------------
 * Element bodies
 * ------------------------- */
goalBody
    : descriptionClause?
      goalClause?
    ;

goalClause
    : achieveClause
    | maintainClause
    | avoidClause
    ;

taskBody
    : descriptionClause?
      preClause?
      postClause?
    ;

elementBody
    : descriptionClause*
    ;

/* -------------------------
 * Goal / Task OCL clauses
 * ------------------------- */
achieveClause
    : ACHIEVE COLON expression SEMI?
    ;

maintainClause
    : MAINTAIN COLON expression SEMI?
    ;

avoidClause
    : AVOID COLON expression SEMI?
    ;

preClause
    : PRE COLON expression SEMI?
    ;

postClause
    : POST COLON expression SEMI?
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
 * OCL Expression Rules
 * ========================================================= */

expression
    : impliesExpr
    ;

impliesExpr
    : orExpr (IMPLIES orExpr)*
    ;

orExpr
    : andExpr (OR andExpr)*
    ;

andExpr
    : equalityExpr (AND equalityExpr)*
    ;

equalityExpr
    : relationalExpr ((EQ | NEEDED_BY) relationalExpr)*
    ;

relationalExpr
    : additiveExpr ((LT | LE | GT | GE) additiveExpr)*
    ;

additiveExpr
    : multiplicativeExpr ((PLUS | MINUS) multiplicativeExpr)*
    ;

multiplicativeExpr
    : unaryExpr ((STAR | SLASH) unaryExpr)*
    ;

unaryExpr
    : NOT unaryExpr
    | MINUS unaryExpr
    | primaryExpr
    ;

primaryExpr
    : literal
    | SELF
    | LPAREN expression RPAREN
    | pathExpr
    ;

pathExpr
    : primaryAtom pathSuffix*
    ;

primaryAtom
    : IDENT
    | SELF
    ;

pathSuffix
    : DOT IDENT
    | DOT IDENT AT_PRE
    | DOT simpleCall
    | CONTRIB_HURT collectionCall
    | AT_PRE
    ;

simpleCall
    : IDENT LPAREN argumentList? RPAREN
    ;

collectionCall
    : iteratorCall
    | simpleCall
    ;

iteratorCall
    : IDENT LPAREN iteratorVars BAR expression RPAREN
    ;

iteratorVars
    : IDENT
    | IDENT COLON qualifiedName
    | IDENT COMMA IDENT
    | IDENT COLON qualifiedName COMMA IDENT COLON qualifiedName
    ;

argumentList
    : expression (COMMA expression)*
    ;

literal
    : STRING
    | INT
    | REAL
    | TRUE
    | FALSE
    | NULL
    | enumLiteral
    ;

enumLiteral
    : IDENT DOUBLE_COLON IDENT
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

// OCL extra operators
AT_PRE          : '@pre' ;
DOUBLE_COLON    : '::' ;
LE              : '<=' ;
GE              : '>=' ;
PLUS            : '+' ;
MINUS           : '-' ;
STAR            : '*' ;
SLASH           : '/' ;

// Separators
LBRACE          : '{' ;
RBRACE          : '}' ;
LPAREN          : '(' ;
RPAREN          : ')' ;
SEMI            : ';' ;
COLON           : ':' ;
GT              : '>' ;
LT              : '<' ;
DOT             : '.' ;
EQ              : '=' ;
COMMA           : ',' ;
BAR             : '|' ;

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

ACHIEVE         : 'achieve' ;
MAINTAIN        : 'maintain' ;
AVOID           : 'avoid' ;
PRE             : 'pre' ;
POST            : 'post' ;

SELF            : 'self' ;
TRUE            : 'true' ;
FALSE           : 'false' ;
NULL            : 'null' ;

AND             : 'and' ;
OR              : 'or' ;
NOT             : 'not' ;
IMPLIES         : 'implies' ;

// Lexical tokens
IDENT
    : [a-zA-Z_][a-zA-Z_0-9]*
    ;

INT
    : [0-9]+
    ;

REAL
    : [0-9]+ '.' [0-9]+
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