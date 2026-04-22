package org.vnu.sme.goal.mm;

public class Dependency {
    private String name;
    private String description;
    private Actor depender;
    private Actor dependee;
    private IntentionalElement dependum;
    
    public Dependency(String name) {
        this.name = name;
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Actor getDepender() { return depender; }
    public void setDepender(Actor depender) { this.depender = depender; }
    
    public Actor getDependee() { return dependee; }
    public void setDependee(Actor dependee) { this.dependee = dependee; }
    
    public IntentionalElement getDependum() { return dependum; }
    public void setDependum(IntentionalElement dependum) { this.dependum = dependum; }
}