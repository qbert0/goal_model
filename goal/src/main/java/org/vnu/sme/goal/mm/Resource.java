package org.vnu.sme.goal.mm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Resource extends ConcreteIntentionalElement {
    private final List<Task> neededByTasks;

    public Resource(String name) {
        super(name);
        this.neededByTasks = new ArrayList<>();
    }

    public List<Task> getNeededByTasks() {
        return Collections.unmodifiableList(neededByTasks);
    }

    public void addNeededByTask(Task task) {
        if (task != null && !neededByTasks.contains(task)) {
            neededByTasks.add(task);
            if (!task.getNeededResources().contains(this)) {
                task.addNeededResource(this);
            }
        }
    }

    @Override
    public String getType() {
        return "Resource";
    }
}
