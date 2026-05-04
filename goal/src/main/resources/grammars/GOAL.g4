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

// achieve có hai dạng:
//   1. achieve: <expression>
//   2. achieve for unique (<typedVarList>) in <expression> : <expression>
achieveClause
    : ACHIEVE COLON body=expression SEMI?
    | ACHIEVE FOR UNIQUE LPAREN typedVarList RPAREN IN source=expression COLON body=expression SEMI?
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
 * Typed variable list (dùng trong achieve for unique)
 * Ví dụ: s: Student, c: Class
 * ------------------------- */
typedVarList
    : typedVar (COMMA typedVar)*
    ;

typedVar
    : IDENT COLON qualifiedName
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
    : conditionalImpliesExpression
    ;

conditionalImpliesExpression
    : conditionalOrExpression (IMPLIES conditionalOrExpression)*
    ;

conditionalOrExpression
    : conditionalXOrExpression (OR conditionalXOrExpression)*
    ;

conditionalXOrExpression
    : conditionalAndExpression (XOR conditionalAndExpression)*
    ;

conditionalAndExpression
    : equalityExpression (AND equalityExpression)*
    ;

equalityExpression
    : relationalExpression ((EQ | NEEDED_BY) relationalExpression)*
    ;

relationalExpression
    : additiveExpression ((LT | LE | GT | GE) additiveExpression)*
    ;

additiveExpression
    : multiplicativeExpression ((PLUS | MINUS) multiplicativeExpression)*
    ;

multiplicativeExpression
    : unaryExpression ((STAR | SLASH | DIV) unaryExpression)*
    ;

unaryExpression
    : (NOT | MINUS | PLUS) unaryExpression
    | postfixExpression
    ;

primaryExpression
    : literal
    | primaryAtom
    | LPAREN expression RPAREN
    ;

/* -------------------------
 * Path / navigation expressions
 * Ví dụ: self.classes->forAll(c | ...)
 *        self.students->size()
 *        self.doors->collect(d | d.openingTime)->max()
 * ------------------------- */
postfixExpression
    : primaryExpression pathSuffix*
    ;

primaryAtom
    : IDENT
    | SELF
    ;

pathSuffix
    : DOT IDENT AT_PRE          // attribute access with @pre
    | DOT IDENT                 // plain attribute / association access
    | DOT simpleCall            // plain operation call
    | CONTRIB_HURT collectionCall      // collection operation: ->forAll(...), ->size(), etc.
    | AT_PRE
    ;

simpleCall
    : IDENT LPAREN argumentList? RPAREN
    ;

/* -------------------------
 * Collection calls (->)
 * Covers iterator operations and aggregate functions defined in spec:
 *   forAll, exists, collect  (iterator – section 5)
 *   size, max, min, count    (aggregation – section 6.3)
 *   includes, excludes, isEmpty, notEmpty  (common OCL helpers)
 * Both iteratorCall and aggregateCall share the collectionCall rule.
 * ------------------------- */
collectionCall
    : iteratorCall
    | aggregateCall
    | simpleCall
    ;

// Iterator operations: forAll, exists, collect  — body separated by '|'
iteratorCall
    : iteratorOp LPAREN iteratorVars BAR expression RPAREN
    ;

iteratorOp
    : FORALL
    | EXISTS
    | EXIST
    | COLLECT
    | SELECT
    | REJECT
    | ANY
    | ONE
    | IS_UNIQUE
    | SORTED_BY
    | CLOSURE
    ;

// Aggregate / query operations that take no iterator variable:
//   ->size()  ->max()  ->min()  ->count(x | x > 0)  ->includes(expr)  ->excludes(expr)
//   ->isEmpty()  ->notEmpty()
aggregateCall
    : aggregateOp LPAREN argumentList? RPAREN               // size(), max(), min(), isEmpty(), notEmpty(), includes(e), excludes(e)
    | COUNT LPAREN iteratorVars BAR expression RPAREN       // count(x | x > 0)
    ;

aggregateOp
    : SIZE
    | SUM
    | MAX
    | MIN
    | IS_EMPTY
    | NOT_EMPTY
    | INCLUDES
    | EXCLUDES
    ;

/* -------------------------
 * Iterator variable declarations
 * Supports:
 *   c
 *   c : Class
 *   c, d
 *   c : Class, d : Class
 * ------------------------- */
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

// --- Relation / operator tokens ---
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
DIV             : 'div' ;

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

// --- Keywords (must appear before IDENT) ---
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

// Keywords for achieve for unique syntax (section 3.3)
FOR             : 'for' ;
UNIQUE          : 'unique' ;
IN              : 'in' ;

SELF            : 'self' ;
TRUE            : 'true' ;
FALSE           : 'false' ;
NULL            : 'null' ;

AND             : 'and' ;
OR              : 'or' ;
XOR             : 'xor' ;
NOT             : 'not' ;
IMPLIES         : 'implies' ;

// OCL iterator keywords (section 5)
FORALL          : 'forAll' ;
EXISTS          : 'exists' ;
EXIST           : 'exist' ;
COLLECT         : 'collect' ;
SELECT          : 'select' ;
REJECT          : 'reject' ;
ANY             : 'any' ;
ONE             : 'one' ;
IS_UNIQUE       : 'isUnique' ;
SORTED_BY       : 'sortedBy' ;
CLOSURE         : 'closure' ;

// OCL aggregation function keywords (section 6.3)
SIZE            : 'size' ;
SUM             : 'sum' ;
MAX             : 'max' ;
MIN             : 'min' ;
COUNT           : 'count' ;
IS_EMPTY        : 'isEmpty' ;
NOT_EMPTY       : 'notEmpty' ;
INCLUDES        : 'includes' ;
EXCLUDES        : 'excludes' ;

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
