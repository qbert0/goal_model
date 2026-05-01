package org.vnu.sme.goal.ast;

import org.antlr.v4.runtime.Token;

public abstract class RelationCS extends DescriptionContainerCS {
    private final Token fName;

    protected RelationCS(Token fName) {
        this.fName = fName;
    }

    public Token getfName() {
        return fName;
    }
}
