package org.vnu.sme.goal.ast;

import org.antlr.v4.runtime.Token;
import org.vnu.sme.goal.mm.ocl.Expression;

public class GoalCS extends IntentionalElementCS {

    public enum GoalType {
        ACHIEVE,
        MAINTAIN,
        AVOID
    }

    private GoalType goalType;
    private Expression oclExpression;

    public GoalCS(Token fName) {
        super(fName);
    }

    public GoalType getGoalType() {
        return goalType;
    }

    public void setGoalType(GoalType goalType) {
        this.goalType = goalType;
    }

    public Expression getOclExpression() {
        return oclExpression;
    }

    public void setOclExpression(Expression oclExpression) {
        this.oclExpression = oclExpression;
    }
}
