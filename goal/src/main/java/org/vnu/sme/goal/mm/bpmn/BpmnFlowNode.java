package org.vnu.sme.goal.mm.bpmn;

public abstract class BpmnFlowNode {
    private final String name;

    protected BpmnFlowNode(String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }
}
