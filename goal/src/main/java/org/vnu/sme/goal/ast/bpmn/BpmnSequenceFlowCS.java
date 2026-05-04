package org.vnu.sme.goal.ast.bpmn;

public final class BpmnSequenceFlowCS {
    private final String name;
    private final String sourceName;
    private final String targetName;

    public BpmnSequenceFlowCS(String name, String sourceName, String targetName) {
        this.name = name;
        this.sourceName = sourceName;
        this.targetName = targetName;
    }

    public String getName() {
        return name;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getTargetName() {
        return targetName;
    }
}
