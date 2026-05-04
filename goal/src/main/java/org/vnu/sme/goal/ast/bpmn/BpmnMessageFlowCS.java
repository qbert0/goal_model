package org.vnu.sme.goal.ast.bpmn;

public final class BpmnMessageFlowCS {
    private final String name;
    private final BpmnEndpointRefCS source;
    private final BpmnEndpointRefCS target;

    public BpmnMessageFlowCS(String name, BpmnEndpointRefCS source, BpmnEndpointRefCS target) {
        this.name = name;
        this.source = source;
        this.target = target;
    }

    public String getName() {
        return name;
    }

    public BpmnEndpointRefCS getSource() {
        return source;
    }

    public BpmnEndpointRefCS getTarget() {
        return target;
    }
}
