package org.vnu.sme.goal.ast;

import org.antlr.v4.runtime.Token;

public class DependencyCS extends RelationCS {

    private Token dependerRef;
    private Token dependeeRef;
    private Token dependumRef;

    public DependencyCS(Token fName) {
        super(fName);
    }

    public Token getDependerRef() {
        return dependerRef;
    }

    public void setDependerRef(Token dependerRef) {
        this.dependerRef = dependerRef;
    }

    public Token getDependeeRef() {
        return dependeeRef;
    }

    public void setDependeeRef(Token dependeeRef) {
        this.dependeeRef = dependeeRef;
    }

    public Token getDependumRef() {
        return dependumRef;
    }

    public void setDependumRef(Token dependumRef) {
        this.dependumRef = dependumRef;
    }
}