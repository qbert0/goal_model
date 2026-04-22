package org.vnu.sme.goal.mm;

import java.util.ArrayList;
import java.util.List;

public abstract class Actor {
    protected String name;
    protected String description;
    protected Actor parent;
    protected Actor instanceOf;
    protected List<IntentionalElement> elements;
    
    public Actor(String name) {
        this.name = name;
        this.elements = new ArrayList<>();
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Actor getParent() { return parent; }
    public void setParent(Actor parent) { this.parent = parent; }
    
    public Actor getInstanceOf() { return instanceOf; }
    public void setInstanceOf(Actor instanceOf) { this.instanceOf = instanceOf; }
    
    public List<IntentionalElement> getElements() { return elements; }
    public void addElement(IntentionalElement element) { this.elements.add(element); }
    
    public abstract String getType();
}