grammar GOAL;

@header {
package org.vnu.sme.goal.parser;
}

/* =========================================================
 * Parser Rules
 * ========================================================= */

goalModel
    : 'istar' IDENT
      actorBlock*
      actorRelationDecl*
      EOF
    ;

/* -------------------------
 * Actor blocks
 * ------------------------- */

actorBlock
    : actorHeader
      descriptionClause?
      actorBody*
      'end'
    ;

actorHeader
    : 'actor' IDENT
    | 'agent' IDENT
    | 'role' IDENT
    ;

actorBody
    : goalDecl
    | taskDecl
    | resourceDecl
    | qualityDecl
    | dependencyDecl
    ;

/* -------------------------
 * Element declarations inside actor
 * ------------------------- */

goalDecl
    : 'goal' IDENT goalRelationTail?
      descriptionClause?
      'end'
    ;

taskDecl
    : 'task' IDENT taskRelationTail?
      descriptionClause?
      'end'
    ;

resourceDecl
    : 'resource' IDENT resourceRelationTail?
      descriptionClause?
      'end'
    ;

qualityDecl
    : 'quality' IDENT qualityRelationTail?
      descriptionClause?
      'end'
    ;

/* -------------------------
 * Internal relations between elements
 * ------------------------- */

goalRelationTail
    : goalRelationOp IDENT
    ;

taskRelationTail
    : taskRelationOp IDENT
    ;

resourceRelationTail
    : 'neededBy' IDENT
    ;

qualityRelationTail
    : 'qualify' IDENT
    ;

goalRelationOp
    : 'and'
    | 'or'
    | 'help'
    | 'make'
    | 'hurt'
    | 'break'
    ;

taskRelationOp
    : 'help'
    | 'make'
    | 'hurt'
    | 'break'
    ;

/* -------------------------
 * Dependency inside actor block
 * Example:
 * depends DeliverPackage on Customer
 *     for { goal PaymentConfirmed, task SubmitOrder }
 * ------------------------- */

dependencyDecl
    : 'depends' IDENT 'on' IDENT 'for' LBRACE dependencyTarget (COMMA dependencyTarget)* RBRACE
    ;

dependencyTarget
    : elementType IDENT
    ;

elementType
    : 'goal'
    | 'task'
    | 'resource'
    | 'quality'
    ;

/* -------------------------
 * Relations between actors
 * Example:
 * agent Driver is DeliverySystem
 * agent Driver participant Customer
 * ------------------------- */

actorRelationDecl
    : actorType IDENT actorRelationOp IDENT
    ;

actorType
    : 'actor'
    | 'agent'
    | 'role'
    ;

actorRelationOp
    : 'is'
    | 'participant'
    ;

/* -------------------------
 * Common clauses
 * ------------------------- */

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