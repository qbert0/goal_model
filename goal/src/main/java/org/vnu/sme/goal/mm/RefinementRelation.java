package org.vnu.sme.goal.mm;

public class RefinementRelation extends Relation {
    public enum RefinementType { AND, OR }
    
    private RefinementType refinementType;
    
    public RefinementRelation(String name) { super(name); }
    
    public RefinementType getRefinementType() { return refinementType; }
    public void setRefinementType(RefinementType type) { this.refinementType = type; }
    
    @Override public String getType() { return "Refinement"; }
}