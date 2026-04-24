package org.vnu.sme.goal.mm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Refinement {
    public enum RefinementType { AND, OR }

    private String name;
    private String description;
    private GoalTaskElement parent;
    private final List<GoalTaskElement> children;

    protected Refinement(String name) {
        this.name = name;
        this.children = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public GoalTaskElement getParent() {
        return parent;
    }

    public void setParent(GoalTaskElement parent) {
        this.parent = parent;
        if (parent != null && !parent.getParentRefinements().contains(this)) {
            parent.addParentRefinement(this);
        }
    }

    public List<GoalTaskElement> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void addChild(GoalTaskElement child) {
        if (child != null && !children.contains(child)) {
            children.add(child);
            if (!child.getChildRefinements().contains(this)) {
                child.addChildRefinement(this);
            }
        }
    }

    public abstract RefinementType getRefinementType();

    public String getType() {
        return "Refinement";
    }
}
