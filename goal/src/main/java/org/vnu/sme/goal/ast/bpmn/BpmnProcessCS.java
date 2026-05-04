package org.vnu.sme.goal.ast.bpmn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BpmnProcessCS {
    private final String name;
    private final List<BpmnFlowNodeCS> flowNodes = new ArrayList<>();
    private final List<BpmnSequenceFlowCS> sequenceFlows = new ArrayList<>();

    public BpmnProcessCS(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addFlowNode(BpmnFlowNodeCS flowNode) {
        flowNodes.add(flowNode);
    }

    public List<BpmnFlowNodeCS> getFlowNodes() {
        return Collections.unmodifiableList(flowNodes);
    }

    public void addSequenceFlow(BpmnSequenceFlowCS sequenceFlow) {
        sequenceFlows.add(sequenceFlow);
    }

    public List<BpmnSequenceFlowCS> getSequenceFlows() {
        return Collections.unmodifiableList(sequenceFlows);
    }
}
