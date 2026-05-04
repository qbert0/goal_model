package org.vnu.sme.goal.ast;

import org.antlr.v4.runtime.Token;
import org.vnu.sme.goal.ast.ocl.OclExpressionCS;

public class TaskCS extends IntentionalElementCS {

    private OclExpressionCS preExpression;
    private OclExpressionCS postExpression;
    private Token preToken;
    private Token postToken;

    public TaskCS(Token fName) {
        super(fName);
    }

    public OclExpressionCS getPreExpression() {
        return preExpression;
    }

    public void setPreExpression(OclExpressionCS preExpression) {
        this.preExpression = preExpression;
    }

    public OclExpressionCS getPostExpression() {
        return postExpression;
    }

    public void setPostExpression(OclExpressionCS postExpression) {
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
