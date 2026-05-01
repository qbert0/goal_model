package org.vnu.sme.goal.mm;

public class Dependency {
    private String name;
    private String description;
    private GoalModel goalModel;
    private Actor depender;
    private Actor dependee;
    private IntentionalElement dependumElement;
    private IntentionalElement dependerElement;
    private IntentionalElement dependeeElement;

    public Dependency(String name) {
        this.name = name;
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

    public Actor getDepender() {
        return depender;
    }

    public void setDepender(Actor depender) {
        this.depender = depender;
    }

    public Actor getDependee() {
        return dependee;
    }

    public void setDependee(Actor dependee) {
        this.dependee = dependee;
    }

    public IntentionalElement getDependumElement() {
        return dependumElement;
    }

    public void setDependumElement(IntentionalElement dependumElement) {
        this.dependumElement = dependumElement;
    }

    public IntentionalElement getDependerElement() {
        return dependerElement;
    }

    public void setDependerElement(IntentionalElement dependerElement) {
        this.dependerElement = dependerElement;
    }

    public IntentionalElement getDependeeElement() {
        return dependeeElement;
    }

    public void setDependeeElement(IntentionalElement dependeeElement) {
        this.dependeeElement = dependeeElement;
    }

    public IntentionalElement getDependum() {
        return getDependumElement();
    }

    public void setDependum(IntentionalElement dependum) {
        setDependumElement(dependum);
    }
}
