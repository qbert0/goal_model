package org.vnu.sme.goal.ast.bpmn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BpmnModelCS {
    private final String name;
    private BpmnCollaborationCS collaboration;
    private final List<BpmnProcessCS> processes = new ArrayList<>();

    public BpmnModelCS(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public BpmnCollaborationCS getCollaboration() {
        return collaboration;
    }

    public void setCollaboration(BpmnCollaborationCS collaboration) {
        this.collaboration = collaboration;
    }

    public void addProcess(BpmnProcessCS process) {
        processes.add(process);
    }

    public List<BpmnProcessCS> getProcesses() {
        return Collections.unmodifiableList(processes);
    }
}
