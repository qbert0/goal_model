package org.vnu.sme.goal.mm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Actor {
    private String name;
    private String description;
    private GoalModel goalModel;
    private Actor isAActor;
    private Actor participatesInActor;
    private final List<IntentionalElement> wantedElements;

    public Actor(String name) {
        this.name = name;
        this.wantedElements = new ArrayList<>();
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

    public GoalModel getGoalModel() {
        return goalModel;
    }

    public void setGoalModel(GoalModel goalModel) {
        this.goalModel = goalModel;
    }

    public Actor getIsAActor() {
        return isAActor;
    }

    public void setIsAActor(Actor isAActor) {
        this.isAActor = isAActor;
    }

    public Actor getParticipatesInActor() {
        return participatesInActor;
    }

    public void setParticipatesInActor(Actor participatesInActor) {
        this.participatesInActor = participatesInActor;
    }

    public List<IntentionalElement> getWantedElements() {
        return Collections.unmodifiableList(wantedElements);
    }

    public void addWantedElement(IntentionalElement element) {
        if (element != null && !wantedElements.contains(element)) {
            wantedElements.add(element);
            element.setOwner(this);
        }
    }

    public Actor getParent() {
        return getIsAActor();
    }

    public void setParent(Actor parent) {
        setIsAActor(parent);
    }

    public Actor getInstanceOf() {
        return getParticipatesInActor();
    }

    public void setInstanceOf(Actor instanceOf) {
        setParticipatesInActor(instanceOf);
    }

    public List<IntentionalElement> getElements() {
        return getWantedElements();
    }

    public void addElement(IntentionalElement element) {
        addWantedElement(element);
    }

    public String getType() {
        return "Actor";
    }
}
