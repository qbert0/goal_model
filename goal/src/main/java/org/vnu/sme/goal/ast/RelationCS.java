package org.vnu.sme.goal.ast;

import org.antlr.v4.runtime.Token;

public abstract class RelationCS extends DescriptionContainerCS {

    protected final Token fName;

    public RelationCS(Token fName) {
        this.fName = fName;
    }

    public Token getfName() {
        return fName;
    }
}