package org.vnu.sme.goal.mm.bpmn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BpmnModel {
    private final String name;
    private BpmnCollaboration collaboration;
    private final List<BpmnProcess> processes = new ArrayList<>();
    private final Map<String, BpmnProcess> processesByName = new LinkedHashMap<>();

    public BpmnModel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public BpmnCollaboration getCollaboration() {
        return collaboration;
    }

    public void setCollaboration(BpmnCollaboration collaboration) {
        this.collaboration = collaboration;
    }

    public void addProcess(BpmnProcess process) {
        processes.add(process);
        processesByName.put(process.getName(), process);
    }

    public List<BpmnProcess> getProcesses() {
        return Collections.unmodifiableList(processes);
    }

    public BpmnProcess getProcess(String name) {
        return processesByName.get(name);
    }
}
