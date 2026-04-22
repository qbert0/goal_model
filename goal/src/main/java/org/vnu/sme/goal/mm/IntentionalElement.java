package org.vnu.sme.goal.mm;

import java.util.ArrayList;
import java.util.List;

public abstract class IntentionalElement {
    protected String name;
    protected String description;
    protected Actor owner;
    protected List<Relation> outgoingRelations;
    protected List<Relation> incomingRelations;
    
    public IntentionalElement(String name) {
        this.name = name;
        this.outgoingRelations = new ArrayList<>();
        this.incomingRelations = new ArrayList<>();
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Actor getOwner() { return owner; }
    public void setOwner(Actor owner) { this.owner = owner; }
    
    public List<Relation> getOutgoingRelations() { return outgoingRelations; }
    public List<Relation> getIncomingRelations() { return incomingRelations; }
    
    public void addOutgoingRelation(Relation rel) { this.outgoingRelations.add(rel); }
    public void addIncomingRelation(Relation rel) { this.incomingRelations.add(rel); }
    
    public abstract String getType();
}