package org.vnu.sme.goal.mm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class GoalTaskElement extends ConcreteIntentionalElement {
    private final List<Refinement> parentRefinements;
    private final List<Refinement> childRefinements;

    protected GoalTaskElement(String name) {
        super(name);
        this.parentRefinements = new ArrayList<>();
        this.childRefinements = new ArrayList<>();
    }

    public List<Refinement> getParentRefinements() {
        return Collections.unmodifiableList(parentRefinements);
    }

    public List<Refinement> getChildRefinements() {
        return Collections.unmodifiableList(childRefinements);
    }

    public void addParentRefinement(Refinement refinement) {
        if (refinement != null && !parentRefinements.contains(refinement)) {
            parentRefinements.add(refinement);
        }
    }

    public void addChildRefinement(Refinement refinement) {
        if (refinement != null && !childRefinements.contains(refinement)) {
            childRefinements.add(refinement);
        }
    }
}
