package org.vnu.sme.goal.ast;

import org.antlr.v4.runtime.Token;

public class ContributionCS extends RelationCS {

    private Token fromRef;
    private Token toRef;
    private Token type;

    public ContributionCS(Token fName) {
        super(fName);
    }

    public Token getFromRef() {
        return fromRef;
    }

    public void setFromRef(Token fromRef) {
        this.fromRef = fromRef;
    }

    public Token getToRef() {
        return toRef;
    }

    public void setToRef(Token toRef) {
        this.toRef = toRef;
    }

    public Token getType() {
        return type;
    }

    public void setType(Token type) {
        this.type = type;
    }
}