package org.vnu.sme.goal.ast.bpmn;

public abstract class BpmnFlowNodeCS {
    private final String name;

    protected BpmnFlowNodeCS(String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }
}
