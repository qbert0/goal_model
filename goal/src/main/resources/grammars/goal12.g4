grammar GOAL;

@header {
package org.vnu.sme.goal.parser;
}
/* =========================================================
 * Parser Rules
 * ========================================================= */

goalModel
    : 'istar' IDENT
      actorDecl*
      ieDecl*
      relationDecl*
      EOF
    ;

/* -------------------------
 * Actor declarations
 * ------------------------- */

actorDecl
    : actorCS
    | agentCS
    | roleCS
    ;

actorCS
    : 'actor' IDENT
      descriptionClause?
      participatesInClause?
      isAClause?
      wantsClause?
      'end'
    ;

agentCS
    : 'agent' IDENT
      descriptionClause?
      participatesInClause?
      isAClause?
      wantsClause?
      'end'
    ;

roleCS
    : 'role' IDENT
      descriptionClause?
      participatesInClause?
      isAClause?
      wantsClause?
      'end'
    ;

/* -------------------------
 * Intentional Elements
 * ------------------------- */

ieDecl
    : goalCS
    | taskCS
    | resourceCS
    | qualityCS
    ;

goalCS
    : 'goal' IDENT
      descriptionClause?
      'end'
    ;

taskCS
    : 'task' IDENT
      descriptionClause?
      'end'
    ;

resourceCS
    : 'resource' IDENT
      descriptionClause?
      'end'
    ;

qualityCS
    : 'quality' IDENT
      descriptionClause?
      'end'
    ;

/* -------------------------
 * Relations
 * ------------------------- */

relationDecl
    : dependencyCS
    | refinementCS
    | contributionCS
    | neededByCS
    | qualificationCS
    ;

/* -------------------------
 * Actor-side relation clauses
 * ------------------------- */

participatesInClause
    : 'participatesIn' '=' LBRACE actorRef (COMMA actorRef)* RBRACE
    ;

isAClause
    : 'isA' '=' LBRACE actorRef (COMMA actorRef)* RBRACE
    ;

wantsClause
    : 'wants' '=' LBRACE ieRef (COMMA ieRef)* RBRACE
    ;

/* -------------------------
 * Dependency
 * depender -> dependum -> dependee
 * ------------------------- */

dependencyCS
    : 'dependency' IDENT
      descriptionClause?
      'depender' '=' actorRef
      'dependee' '=' actorRef
      'dependum' '=' ieRef
      'end'
    ;

/* -------------------------
 * Refinement
 * Có 2 loại: and / or
 * parent <- children
 * ------------------------- */

refinementCS
    : 'refinement' IDENT
      'type' '=' refinementType
      'parent' '=' goalTaskRef
      'children' '=' LBRACE goalTaskRef (COMMA goalTaskRef)+ RBRACE
      descriptionClause?
      'end'
    ;

refinementType
    : 'and'
    | 'or'
    ;

/* -------------------------
 * Contribution
 * source contributesTo target
 * Thường source/target là intentional elements
 * ------------------------- */

contributionCS
    : 'contribution' IDENT
      'from' '=' ieRef
      'to' '=' ieRef
      'type' '=' contributionType
      descriptionClause?
      'end'
    ;

contributionType
    : 'make'
    | 'help'
    | 'someplus'
    | 'unknown'
    | 'someminus'
    | 'hurt'
    | 'break'
    ;

/* -------------------------
 * Resource neededBy Task
 * ------------------------- */

neededByCS
    : 'neededBy' IDENT
      'resource' '=' resourceRef
      'task' '=' taskRef
      descriptionClause?
      'end'
    ;

/* -------------------------
 * Qualification
 * Quality qualifies Task/Resource
 * ------------------------- */

qualificationCS
    : 'qualification' IDENT
      'quality' '=' qualityRef
      'target' '=' qualifiedRef
      descriptionClause?
      'end'
    ;

/* =========================================================
 * Reference Rules
 * ========================================================= */

actorRef
    : IDENT
    ;

ieRef
    : IDENT
    ;

goalTaskRef
    : IDENT
    ;

resourceRef
    : IDENT
    ;

taskRef
    : IDENT
    ;

qualityRef
    : IDENT
    ;

qualifiedRef
    : IDENT
    ;

/* =========================================================
 * Common Clauses
 * ========================================================= */

descriptionClause
    : 'description' '=' STRING
    ;

/* =========================================================
 * Lexer Rules
 * ========================================================= */

LBRACE : '{' ;
RBRACE : '}' ;
COMMA  : ',' ;

STRING
    : '"' ( ~["\\] | '\\' . )* '"'
    ;

IDENT
    : [a-zA-Z_][a-zA-Z_0-9]*
    ;

WS
    : [ \t\r\n\f]+ -> skip
    ;

LINE_COMMENT
    : '//' ~[\r\n]* -> skip
    ;

BLOCK_COMMENT
    : '/*' .*? '*/' -> skip
    ;