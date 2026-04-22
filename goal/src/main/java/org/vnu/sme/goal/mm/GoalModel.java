package org.vnu.sme.goal.mm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoalModel {
    private String name;
    private List<Actor> actors;
    private List<Dependency> dependencies;
    private Map<String, Actor> actorMap;
    private Map<String, IntentionalElement> elementMap;
    
    public GoalModel(String name) {
        this.name = name;
        this.actors = new ArrayList<>();
        this.dependencies = new ArrayList<>();
        this.actorMap = new HashMap<>();
        this.elementMap = new HashMap<>();
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public List<Actor> getActors() { return actors; }
    public List<Dependency> getDependencies() { return dependencies; }
    
    public void addActor(Actor actor) {
        actors.add(actor);
        actorMap.put(actor.getName(), actor);
        for (IntentionalElement element : actor.getElements()) {
            elementMap.put(actor.getName() + "." + element.getName(), element);
        }
    }
    
    public void addDependency(Dependency dependency) {
        dependencies.add(dependency);
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
            allElements.addAll(actor.getElements());
        }
        return allElements;
    }
}