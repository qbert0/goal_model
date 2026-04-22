package org.vnu.sme.goal.ast;

import org.antlr.v4.runtime.Token;

public class GoalCS extends IntentionalElementCS {

    public enum GoalType {
        ACHIEVE,
        MAINTAIN,
        AVOID
    }

    private GoalType goalType;
    private String oclExpression;

    public GoalCS(Token fName) {
        super(fName);
    }

    public GoalType getGoalType() {
        return goalType;
    }

    public void setGoalType(GoalType goalType) {
        this.goalType = goalType;
    }

    public String getOclExpression() {
        return oclExpression;
    }

    public void setOclExpression(String oclExpression) {
        this.oclExpression = oclExpression;
    }
}