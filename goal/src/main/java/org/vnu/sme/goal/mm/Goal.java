package org.vnu.sme.goal.mm;

import org.vnu.sme.goal.mm.ocl.OpaqueExpression;

public class Goal extends GoalTaskElement {
    public enum GoalType { ACHIEVE, MAINTAIN, AVOID }

    private GoalClause goalClause;

    public Goal(String name) {
        super(name);
    }

    public GoalClause getGoalClause() {
        return goalClause;
    }

    public void setGoalClause(GoalClause goalClause) {
        this.goalClause = goalClause;
    }

    // Compatibility API
    public GoalType getGoalType() {
        if (goalClause instanceof Achieve) {
            return GoalType.ACHIEVE;
        }
        if (goalClause instanceof Maintain) {
            return GoalType.MAINTAIN;
        }
        if (goalClause instanceof Avoid) {
            return GoalType.AVOID;
        }
        return null;
    }

    public void setGoalType(GoalType goalType) {
        if (goalType == null) {
            this.goalClause = null;
            return;
        }

        switch (goalType) {
            case ACHIEVE:
                this.goalClause = new Achieve();
                break;
            case MAINTAIN:
                this.goalClause = new Maintain();
                break;
            case AVOID:
                this.goalClause = new Avoid();
                break;
            default:
                this.goalClause = null;
        }
    }

    public String getOclExpression() {
        return goalClause == null ? null : goalClause.getPrimaryText();
    }

    public void setOclExpression(String oclExpression) {
        if (oclExpression == null || goalClause == null) {
            return;
        }
        goalClause.addExpression(new OpaqueExpression(oclExpression));
    }

    @Override
    public String getType() {
        return "Goal";
    }
}
