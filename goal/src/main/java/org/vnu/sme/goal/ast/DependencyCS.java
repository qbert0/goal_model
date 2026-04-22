package org.vnu.sme.goal.ast;

import org.antlr.v4.runtime.Token;

public class DependencyCS extends DescriptionContainerCS {

    private final Token fName;
    private String dependerRef;
    private String dependeeRef;
    private IntentionalElementCS dependum;

    public DependencyCS(Token fName) {
        this.fName = fName;
    }

    public Token getfName() {
        return fName;
    }

    public String getDependerRef() {
        return dependerRef;
    }

    public void setDependerRef(String dependerRef) {
        this.dependerRef = dependerRef;
    }

    public String getDependeeRef() {
        return dependeeRef;
    }

    public void setDependeeRef(String dependeeRef) {
        this.dependeeRef = dependeeRef;
    }

    public IntentionalElementCS getDependum() {
        return dependum;
    }

    public void setDependum(IntentionalElementCS dependum) {
        this.dependum = dependum;
    }
}