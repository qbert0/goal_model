package org.vnu.sme.goal.ast;

import org.antlr.v4.runtime.Token;

public abstract class IntentionalElementCS extends DescriptionContainerCS {

    protected final Token fName;

    public IntentionalElementCS(Token fName) {
        this.fName = fName;
    }

    public Token getfName() {
        return fName;
    }
}