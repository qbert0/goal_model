package org.vnu.sme.goal.mm;

public class ContributionRelation extends Relation {
    public enum ContributionType { MAKE, HELP, SOME_PLUS, UNKNOWN, SOME_MINUS, HURT, BREAK }
    
    private ContributionType contributionType;
    
    public ContributionRelation(String name) { super(name); }
    
    public ContributionType getContributionType() { return contributionType; }
    public void setContributionType(ContributionType type) { this.contributionType = type; }
    
    @Override public String getType() { return "Contribution"; }
}