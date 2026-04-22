package org.vnu.sme.goal.mm;

public class Goal extends IntentionalElement {
    public enum GoalType { ACHIEVE, MAINTAIN, AVOID }
    
    private GoalType goalType;
    private String oclExpression;
    
    public Goal(String name) { super(name); }
    
    public GoalType getGoalType() { return goalType; }
    public void setGoalType(GoalType goalType) { this.goalType = goalType; }
    
    public String getOclExpression() { return oclExpression; }
    public void setOclExpression(String oclExpression) { this.oclExpression = oclExpression; }
    
    @Override public String getType() { return "Goal"; }
}