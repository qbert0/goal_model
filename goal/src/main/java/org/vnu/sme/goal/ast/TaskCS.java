package org.vnu.sme.goal.ast;

import org.antlr.v4.runtime.Token;
import org.vnu.sme.goal.mm.ocl.Expression;

public class TaskCS extends IntentionalElementCS {

    private Expression preExpression;
    private Expression postExpression;
    private Token preToken;
    private Token postToken;

    public TaskCS(Token fName) {
        super(fName);
    }

    public Expression getPreExpression() {
        return preExpression;
    }

    public void setPreExpression(Expression preExpression) {
        this.preExpression = preExpression;
    }

    public Expression getPostExpression() {
        return postExpression;
    }

    public void setPostExpression(Expression postExpression) {
        this.postExpression = postExpression;
    }

    public Token getPreToken() {
        return preToken;
    }

    public void setPreToken(Token preToken) {
        this.preToken = preToken;
    }

    public Token getPostToken() {
        return postToken;
    }

    public void setPostToken(Token postToken) {
        this.postToken = postToken;
    }
}
