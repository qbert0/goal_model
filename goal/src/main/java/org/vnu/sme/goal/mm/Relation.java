package org.vnu.sme.goal.mm;

public abstract class Relation {
    protected String name;
    protected String description;
    protected IntentionalElement source;
    protected IntentionalElement target;
    
    public Relation(String name) {
        this.name = name;
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public IntentionalElement getSource() { return source; }
    public void setSource(IntentionalElement source) { this.source = source; }
    
    public IntentionalElement getTarget() { return target; }
    public void setTarget(IntentionalElement target) { this.target = target; }
    
    public abstract String getType();
}