package org.vnu.sme.goal.mm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoalModel {
    private String name;
    private final List<Actor> actors;
    private final List<Dependency> dependencies;
    private final Map<String, Actor> actorMap;
    private final Map<String, IntentionalElement> elementMap;

    public GoalModel(String name) {
        this.name = name;
        this.actors = new ArrayList<>();
        this.dependencies = new ArrayList<>();
        this.actorMap = new HashMap<>();
        this.elementMap = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Actor> getActors() {
        return Collections.unmodifiableList(actors);
    }

    public List<Dependency> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    public void addActor(Actor actor) {
        if (actor == null || actors.contains(actor)) {
            return;
        }
        actors.add(actor);
        actor.setGoalModel(this);
        actorMap.put(actor.getName(), actor);
        for (IntentionalElement element : actor.getWantedElements()) {
            registerElement(actor, element);
        }
    }

    public void addDependency(Dependency dependency) {
        if (dependency == null || dependencies.contains(dependency)) {
            return;
        }
        dependencies.add(dependency);
        dependency.setGoalModel(this);
        IntentionalElement dependum = dependency.getDependumElement();
        if (dependum != null) {
            registerElement(null, dependum);
        }
    }

    public Actor getActor(String name) {
        return actorMap.get(name);
    }

    public IntentionalElement getElement(String qualifiedName) {
        return elementMap.get(qualifiedName);
    }

    public List<IntentionalElement> getAllElements() {
        List<IntentionalElement> allElements = new ArrayList<>();
        for (Actor actor : actors) {
            allElements.addAll(actor.getWantedElements());
        }
        for (Dependency dependency : dependencies) {
            if (dependency.getDependumElement() != null) {
                allElements.add(dependency.getDependumElement());
            }
        }
        return allElements;
    }

    public void registerElement(Actor owner, IntentionalElement element) {
        if (element == null) {
            return;
        }
        if (owner != null) {
            elementMap.put(owner.getName() + "." + element.getName(), element);
        }
        elementMap.putIfAbsent(element.getName(), element);
    }

    public List<Contribution> getContributions() {
        List<Contribution> contributions = new ArrayList<>();
        for (IntentionalElement element : getAllElements()) {
            for (Contribution contribution : element.getOutgoingContributions()) {
                if (!contributions.contains(contribution)) {
                    contributions.add(contribution);
                }
            }
        }
        return contributions;
    }

    public List<Refinement> getRefinements() {
        List<Refinement> refinements = new ArrayList<>();
        for (IntentionalElement element : getAllElements()) {
            if (!(element instanceof GoalTaskElement)) {
                continue;
            }
            GoalTaskElement goalTaskElement = (GoalTaskElement) element;
            for (Refinement refinement : goalTaskElement.getChildRefinements()) {
                if (!refinements.contains(refinement)) {
                    refinements.add(refinement);
                }
            }
            for (Refinement refinement : goalTaskElement.getParentRefinements()) {
                if (!refinements.contains(refinement)) {
                    refinements.add(refinement);
                }
            }
        }
        return refinements;
    }
}
