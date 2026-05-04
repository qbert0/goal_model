package org.vnu.sme.goal.mm.bpmn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BpmnCollaboration {
    private final String name;
    private final List<BpmnParticipant> participants = new ArrayList<>();
    private final List<BpmnMessageFlow> messageFlows = new ArrayList<>();

    public BpmnCollaboration(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addParticipant(BpmnParticipant participant) {
        participants.add(participant);
    }

    public List<BpmnParticipant> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    public void addMessageFlow(BpmnMessageFlow messageFlow) {
        messageFlows.add(messageFlow);
    }

    public List<BpmnMessageFlow> getMessageFlows() {
        return Collections.unmodifiableList(messageFlows);
    }
}
