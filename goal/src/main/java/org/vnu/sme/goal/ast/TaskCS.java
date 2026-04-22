package org.vnu.sme.goal.ast;

import org.antlr.v4.runtime.Token;

public class TaskCS extends IntentionalElementCS {

    private String preExpression;
    private String postExpression;

    public TaskCS(Token fName) {
        super(fName);
    }

    public String getPreExpression() {
        return preExpression;
    }

    public void setPreExpression(String preExpression) {
        this.preExpression = preExpression;
    }

    public String getPostExpression() {
        return postExpression;
    }

    public void setPostExpression(String postExpression) {
        this.postExpression = postExpression;
    }
}