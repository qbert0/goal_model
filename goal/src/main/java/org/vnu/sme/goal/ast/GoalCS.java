package org.vnu.sme.goal.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.Token;
import org.vnu.sme.goal.mm.ocl.Expression;
import org.vnu.sme.goal.mm.ocl.VariableDeclaration;

/**
 * Concrete-syntax node for {@code goal} declarations. Holds the body of either
 * the simple form ({@code achieve|maintain|avoid: <expr>}) or the form-2
 * {@code achieve for unique (s: T, ...) in <sourceExpr> : <bodyExpr>} variant.
 */
public class GoalCS extends IntentionalElementCS {

    public enum GoalType {
        ACHIEVE,
        ACHIEVE_UNIQUE,
        MAINTAIN,
        AVOID
    }

    private GoalType goalType;
    private Token clauseToken;
    private Expression oclExpression;
    private final List<VariableDeclaration> iterVars = new ArrayList<>();
    private Expression sourceExpression;

    public GoalCS(Token fName) {
        super(fName);
    }

    public GoalType getGoalType() {
        return goalType;
    }

    public void setGoalType(GoalType goalType) {
        this.goalType = goalType;
    }

    public Token getClauseToken() {
        return clauseToken;
    }

    public void setClauseToken(Token clauseToken) {
        this.clauseToken = clauseToken;
    }

    public Expression getOclExpression() {
        return oclExpression;
    }

    public void setOclExpression(Expression oclExpression) {
        this.oclExpression = oclExpression;
    }

    public List<VariableDeclaration> getIterVars() {
        return Collections.unmodifiableList(iterVars);
    }

    public void setIterVars(List<VariableDeclaration> iterVars) {
        this.iterVars.clear();
        if (iterVars != null) {
            this.iterVars.addAll(iterVars);
        }
    }

    public Expression getSourceExpression() {
        return sourceExpression;
    }

    public void setSourceExpression(Expression sourceExpression) {
        this.sourceExpression = sourceExpression;
    }
}
