package org.vnu.sme.goal.parser.bpmn;

import java.util.LinkedHashMap;
import java.util.Map;

import org.vnu.sme.goal.ast.bpmn.BpmnCollaborationCS;
import org.vnu.sme.goal.ast.bpmn.BpmnEndEventCS;
import org.vnu.sme.goal.ast.bpmn.BpmnExclusiveGatewayCS;
import org.vnu.sme.goal.ast.bpmn.BpmnFlowNodeCS;
import org.vnu.sme.goal.ast.bpmn.BpmnMessageFlowCS;
import org.vnu.sme.goal.ast.bpmn.BpmnModelCS;
import org.vnu.sme.goal.ast.bpmn.BpmnParallelGatewayCS;
import org.vnu.sme.goal.ast.bpmn.BpmnParticipantCS;
import org.vnu.sme.goal.ast.bpmn.BpmnProcessCS;
import org.vnu.sme.goal.ast.bpmn.BpmnSequenceFlowCS;
import org.vnu.sme.goal.ast.bpmn.BpmnStartEventCS;
import org.vnu.sme.goal.ast.bpmn.BpmnTaskCS;
import org.vnu.sme.goal.mm.bpmn.BpmnCollaboration;
import org.vnu.sme.goal.mm.bpmn.BpmnEndEvent;
import org.vnu.sme.goal.mm.bpmn.BpmnExclusiveGateway;
import org.vnu.sme.goal.mm.bpmn.BpmnFlowNode;
import org.vnu.sme.goal.mm.bpmn.BpmnMessageFlow;
import org.vnu.sme.goal.mm.bpmn.BpmnModel;
import org.vnu.sme.goal.mm.bpmn.BpmnParallelGateway;
import org.vnu.sme.goal.mm.bpmn.BpmnParticipant;
import org.vnu.sme.goal.mm.bpmn.BpmnProcess;
import org.vnu.sme.goal.mm.bpmn.BpmnSequenceFlow;
import org.vnu.sme.goal.mm.bpmn.BpmnStartEvent;
import org.vnu.sme.goal.mm.bpmn.BpmnTask;

public final class BpmnModelFactory {
    public BpmnModel create(BpmnModelCS astModel) {
        BpmnModel model = new BpmnModel(astModel.getName());
        Map<String, BpmnProcess> processesByName = new LinkedHashMap<>();

        for (BpmnProcessCS processCS : astModel.getProcesses()) {
            if (processesByName.containsKey(processCS.getName())) {
                throw new BpmnParseException("Duplicate BPMN process '" + processCS.getName() + "'.");
            }

            BpmnProcess process = new BpmnProcess(processCS.getName());
            Map<String, BpmnFlowNode> nodesByName = new LinkedHashMap<>();
            for (BpmnFlowNodeCS flowNodeCS : processCS.getFlowNodes()) {
                BpmnFlowNode flowNode = createFlowNode(flowNodeCS);
                if (nodesByName.put(flowNode.getName(), flowNode) != null) {
                    throw new BpmnParseException("Duplicate BPMN flow node '" + flowNode.getName()
                            + "' in process '" + process.getName() + "'.");
                }
                process.addFlowNode(flowNode);
            }
            validateProcessNodeKinds(process);
            for (BpmnSequenceFlowCS sequenceFlowCS : processCS.getSequenceFlows()) {
                BpmnFlowNode source = nodesByName.get(sequenceFlowCS.getSourceName());
                BpmnFlowNode target = nodesByName.get(sequenceFlowCS.getTargetName());
                if (source == null || target == null) {
                    throw new BpmnParseException("Process '" + process.getName()
                            + "' has sequenceFlow '" + sequenceFlowCS.getName()
                            + "' referencing unknown node(s).");
                }
                process.addSequenceFlow(new BpmnSequenceFlow(sequenceFlowCS.getName(), source, target));
            }
            model.addProcess(process);
            processesByName.put(process.getName(), process);
        }

        model.setCollaboration(createCollaboration(astModel.getCollaboration(), model, processesByName));
        return model;
    }

    private BpmnCollaboration createCollaboration(BpmnCollaborationCS collaborationCS,
                                                  BpmnModel model,
                                                  Map<String, BpmnProcess> processesByName) {
        if (collaborationCS == null) {
            throw new BpmnParseException("BPMN model must declare exactly one collaboration.");
        }

        BpmnCollaboration collaboration = new BpmnCollaboration(collaborationCS.getName());
        Map<String, BpmnParticipant> participantsByName = new LinkedHashMap<>();
        for (BpmnParticipantCS participantCS : collaborationCS.getParticipants()) {
            BpmnProcess process = processesByName.get(participantCS.getProcessRefName());
            if (process == null) {
                throw new BpmnParseException("Participant '" + participantCS.getName()
                        + "' references unknown process '" + participantCS.getProcessRefName() + "'.");
            }
            if (participantsByName.containsKey(participantCS.getName())) {
                throw new BpmnParseException("Duplicate BPMN participant '" + participantCS.getName() + "'.");
            }
            BpmnParticipant participant = new BpmnParticipant(participantCS.getName(), process);
            collaboration.addParticipant(participant);
            participantsByName.put(participant.getName(), participant);
        }

        for (BpmnMessageFlowCS messageFlowCS : collaborationCS.getMessageFlows()) {
            BpmnParticipant sourceParticipant = participantsByName.get(messageFlowCS.getSource().getParticipantName());
            BpmnParticipant targetParticipant = participantsByName.get(messageFlowCS.getTarget().getParticipantName());
            if (sourceParticipant == null || targetParticipant == null) {
                throw new BpmnParseException("Message flow '" + messageFlowCS.getName()
                        + "' references unknown participant(s).");
            }
            BpmnFlowNode sourceNode = sourceParticipant.getProcess().getFlowNode(messageFlowCS.getSource().getNodeName());
            BpmnFlowNode targetNode = targetParticipant.getProcess().getFlowNode(messageFlowCS.getTarget().getNodeName());
            if (sourceNode == null || targetNode == null) {
                throw new BpmnParseException("Message flow '" + messageFlowCS.getName()
                        + "' references unknown endpoint node(s).");
            }
            collaboration.addMessageFlow(new BpmnMessageFlow(
                    messageFlowCS.getName(),
                    sourceParticipant,
                    sourceNode,
                    targetParticipant,
                    targetNode));
        }
        return collaboration;
    }

    private BpmnFlowNode createFlowNode(BpmnFlowNodeCS flowNodeCS) {
        if (flowNodeCS instanceof BpmnStartEventCS startEventCS) {
            return new BpmnStartEvent(startEventCS.getName());
        }
        if (flowNodeCS instanceof BpmnEndEventCS endEventCS) {
            return new BpmnEndEvent(endEventCS.getName());
        }
        if (flowNodeCS instanceof BpmnTaskCS taskCS) {
            return new BpmnTask(taskCS.getName());
        }
        if (flowNodeCS instanceof BpmnExclusiveGatewayCS exclusiveGatewayCS) {
            return new BpmnExclusiveGateway(exclusiveGatewayCS.getName());
        }
        if (flowNodeCS instanceof BpmnParallelGatewayCS parallelGatewayCS) {
            return new BpmnParallelGateway(parallelGatewayCS.getName());
        }
        throw new IllegalArgumentException("Unsupported BPMN flow node: " + flowNodeCS.getClass().getName());
    }

    private void validateProcessNodeKinds(BpmnProcess process) {
        if (process.getFlowNodesByType(BpmnStartEvent.class).isEmpty()) {
            throw new BpmnParseException("Process '" + process.getName() + "' has no startEvent.");
        }
        if (process.getFlowNodesByType(BpmnEndEvent.class).isEmpty()) {
            throw new BpmnParseException("Process '" + process.getName() + "' has no endEvent.");
        }
    }
}
