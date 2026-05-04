package org.vnu.sme.goal.mm.bpmn;

public final class BpmnMessageFlow {
    private final String name;
    private final BpmnParticipant sourceParticipant;
    private final BpmnFlowNode sourceNode;
    private final BpmnParticipant targetParticipant;
    private final BpmnFlowNode targetNode;

    public BpmnMessageFlow(String name,
                           BpmnParticipant sourceParticipant,
                           BpmnFlowNode sourceNode,
                           BpmnParticipant targetParticipant,
                           BpmnFlowNode targetNode) {
        this.name = name;
        this.sourceParticipant = sourceParticipant;
        this.sourceNode = sourceNode;
        this.targetParticipant = targetParticipant;
        this.targetNode = targetNode;
    }

    public String getName() {
        return name;
    }

    public BpmnParticipant getSourceParticipant() {
        return sourceParticipant;
    }

    public BpmnFlowNode getSourceNode() {
        return sourceNode;
    }

    public BpmnParticipant getTargetParticipant() {
        return targetParticipant;
    }

    public BpmnFlowNode getTargetNode() {
        return targetNode;
    }
}
