package org.vnu.sme.goal.ast.bpmn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BpmnCollaborationCS {
    private final String name;
    private final List<BpmnParticipantCS> participants = new ArrayList<>();
    private final List<BpmnMessageFlowCS> messageFlows = new ArrayList<>();

    public BpmnCollaborationCS(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addParticipant(BpmnParticipantCS participant) {
        participants.add(participant);
    }

    public List<BpmnParticipantCS> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    public void addMessageFlow(BpmnMessageFlowCS messageFlow) {
        messageFlows.add(messageFlow);
    }

    public List<BpmnMessageFlowCS> getMessageFlows() {
        return Collections.unmodifiableList(messageFlows);
    }
}
