package org.vnu.sme.goal.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.Token;
import org.vnu.sme.goal.ast.ocl.OclExpressionCS;
import org.vnu.sme.goal.ast.ocl.OclVariableDeclarationCS;

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
    private OclExpressionCS oclExpression;
    private final List<OclVariableDeclarationCS> iterVars = new ArrayList<>();
    private OclExpressionCS sourceExpression;

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

    public OclExpressionCS getOclExpression() {
        return oclExpression;
    }

    public void setOclExpression(OclExpressionCS oclExpression) {
        this.oclExpression = oclExpression;
    }

    public List<OclVariableDeclarationCS> getIterVars() {
        return Collections.unmodifiableList(iterVars);
    }

    public void setIterVars(List<OclVariableDeclarationCS> iterVars) {
        this.iterVars.clear();
        if (iterVars != null) {
            this.iterVars.addAll(iterVars);
        }
    }

    public OclExpressionCS getSourceExpression() {
        return sourceExpression;
    }

    public void setSourceExpression(OclExpressionCS sourceExpression) {
        this.sourceExpression = sourceExpression;
    }
}
