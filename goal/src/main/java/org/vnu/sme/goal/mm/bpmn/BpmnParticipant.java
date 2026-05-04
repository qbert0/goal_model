package org.vnu.sme.goal.mm.bpmn;

public final class BpmnParticipant {
    private final String name;
    private final BpmnProcess process;

    public BpmnParticipant(String name, BpmnProcess process) {
        this.name = name;
        this.process = process;
    }

    public String getName() {
        return name;
    }

    public BpmnProcess getProcess() {
        return process;
    }
}
