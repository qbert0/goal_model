package org.vnu.sme.goal.mm.bpmn;

public final class BpmnSequenceFlow {
    private final String name;
    private final BpmnFlowNode source;
    private final BpmnFlowNode target;

    public BpmnSequenceFlow(String name, BpmnFlowNode source, BpmnFlowNode target) {
        this.name = name;
        this.source = source;
        this.target = target;
    }

    public String getName() {
        return name;
    }

    public BpmnFlowNode getSource() {
        return source;
    }

    public BpmnFlowNode getTarget() {
        return target;
    }
}
