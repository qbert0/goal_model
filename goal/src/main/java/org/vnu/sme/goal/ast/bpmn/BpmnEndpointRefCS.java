package org.vnu.sme.goal.ast.bpmn;

public final class BpmnEndpointRefCS {
    private final String participantName;
    private final String nodeName;

    public BpmnEndpointRefCS(String participantName, String nodeName) {
        this.participantName = participantName;
        this.nodeName = nodeName;
    }

    public String getParticipantName() {
        return participantName;
    }

    public String getNodeName() {
        return nodeName;
    }
}
