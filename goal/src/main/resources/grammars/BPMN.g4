grammar BPMN;

@header {
package org.vnu.sme.goal.parser.bpmn;
}

bpmnModel
    : BPMN_KW modelName=IDENT LBRACE collaborationDecl processDecl+ RBRACE EOF
    ;

collaborationDecl
    : COLLABORATION_KW name=IDENT LBRACE participantDecl+ messageFlowDecl* RBRACE
    ;

participantDecl
    : PARTICIPANT_KW participantName=IDENT PROCESS_KW processRef=IDENT SEMI?
    ;

messageFlowDecl
    : MESSAGE_FLOW_KW flowName=IDENT? FROM_KW source=endpointRef TO_KW target=endpointRef SEMI?
    ;

endpointRef
    : participantName=IDENT DOT nodeName=IDENT
    ;

processDecl
    : PROCESS_KW processName=IDENT LBRACE flowNodeDecl+ sequenceFlowDecl+ RBRACE
    ;

flowNodeDecl
    : startEventDecl
    | endEventDecl
    | taskDecl
    | exclusiveGatewayDecl
    | parallelGatewayDecl
    ;

startEventDecl
    : START_EVENT_KW nodeName=IDENT SEMI?
    ;

endEventDecl
    : END_EVENT_KW nodeName=IDENT SEMI?
    ;

taskDecl
    : TASK_KW nodeName=IDENT SEMI?
    ;

exclusiveGatewayDecl
    : EXCLUSIVE_GATEWAY_KW nodeName=IDENT SEMI?
    ;

parallelGatewayDecl
    : PARALLEL_GATEWAY_KW nodeName=IDENT SEMI?
    ;

sequenceFlowDecl
    : SEQUENCE_FLOW_KW flowName=IDENT? FROM_KW sourceName=IDENT TO_KW targetName=IDENT SEMI?
    ;

BPMN_KW : 'bpmn';
COLLABORATION_KW : 'collaboration';
PARTICIPANT_KW : 'participant';
PROCESS_KW : 'process';
MESSAGE_FLOW_KW : 'messageFlow';
SEQUENCE_FLOW_KW : 'sequenceFlow';
START_EVENT_KW : 'startEvent';
END_EVENT_KW : 'endEvent';
TASK_KW : 'task';
EXCLUSIVE_GATEWAY_KW : 'exclusiveGateway';
PARALLEL_GATEWAY_KW : 'parallelGateway';
FROM_KW : 'from';
TO_KW : 'to';

LBRACE : '{';
RBRACE : '}';
SEMI : ';';
DOT : '.';

IDENT
    : [A-Za-z_] [A-Za-z0-9_]*
    ;

LINE_COMMENT
    : '//' ~[\r\n]* -> skip
    ;

BLOCK_COMMENT
    : '/*' .*? '*/' -> skip
    ;

WS
    : [ \t\r\n]+ -> skip
    ;
