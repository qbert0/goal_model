package org.vnu.sme.goal.mm.bpmn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BpmnProcess {
    private final String name;
    private final Map<String, BpmnFlowNode> flowNodes = new LinkedHashMap<>();
    private final List<BpmnSequenceFlow> sequenceFlows = new ArrayList<>();

    public BpmnProcess(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addFlowNode(BpmnFlowNode node) {
        flowNodes.put(node.getName(), node);
    }

    public BpmnFlowNode getFlowNode(String name) {
        return flowNodes.get(name);
    }

    public Collection<BpmnFlowNode> getFlowNodes() {
        return Collections.unmodifiableCollection(flowNodes.values());
    }

    public void addSequenceFlow(BpmnSequenceFlow flow) {
        sequenceFlows.add(flow);
    }

    public List<BpmnSequenceFlow> getSequenceFlows() {
        return Collections.unmodifiableList(sequenceFlows);
    }

    public <T extends BpmnFlowNode> List<T> getFlowNodesByType(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (BpmnFlowNode node : flowNodes.values()) {
            if (type.isInstance(node)) {
                result.add(type.cast(node));
            }
        }
        return result;
    }

    public List<BpmnTask> getTasks() {
        List<BpmnTask> result = new ArrayList<>();
        for (BpmnFlowNode node : flowNodes.values()) {
            if (node instanceof BpmnTask task) {
                result.add(task);
            }
        }
        return result;
    }

    public List<BpmnFlowNode> getOutgoing(BpmnFlowNode node) {
        List<BpmnFlowNode> result = new ArrayList<>();
        for (BpmnSequenceFlow flow : sequenceFlows) {
            if (flow.getSource().equals(node)) {
                result.add(flow.getTarget());
            }
        }
        return result;
    }

    public List<BpmnFlowNode> getIncoming(BpmnFlowNode node) {
        List<BpmnFlowNode> result = new ArrayList<>();
        for (BpmnSequenceFlow flow : sequenceFlows) {
            if (flow.getTarget().equals(node)) {
                result.add(flow.getSource());
            }
        }
        return result;
    }
}
