package org.vnu.sme.goal.mm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class IntentionalElement {
    private String name;
    private String description;
    private Actor owner;
    private final List<Contribution> outgoingContributions;
    private final List<Contribution> incomingContributions;

    protected IntentionalElement(String name) {
        this.name = name;
        this.outgoingContributions = new ArrayList<>();
        this.incomingContributions = new ArrayList<>();
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

    public Actor getOwner() {
        return owner;
    }

    public void setOwner(Actor owner) {
        this.owner = owner;
    }

    public List<Contribution> getOutgoingContributions() {
        return Collections.unmodifiableList(outgoingContributions);
    }

    public List<Contribution> getIncomingContributions() {
        return Collections.unmodifiableList(incomingContributions);
    }

    public void addOutgoingContribution(Contribution contribution) {
        if (contribution != null && !outgoingContributions.contains(contribution)) {
            outgoingContributions.add(contribution);
            contribution.setSource(this);
        }
    }

    public void addIncomingContribution(Contribution contribution) {
        if (contribution != null && !incomingContributions.contains(contribution)) {
            incomingContributions.add(contribution);
            contribution.setTarget(this);
        }
    }

    public abstract String getType();
}
