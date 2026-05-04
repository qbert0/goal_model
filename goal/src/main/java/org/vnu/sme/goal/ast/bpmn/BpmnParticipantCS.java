package org.vnu.sme.goal.ast.bpmn;

public final class BpmnParticipantCS {
    private final String name;
    private final String processRefName;

    public BpmnParticipantCS(String name, String processRefName) {
        this.name = name;
        this.processRefName = processRefName;
    }

    public String getName() {
        return name;
    }

    public String getProcessRefName() {
        return processRefName;
    }
}
