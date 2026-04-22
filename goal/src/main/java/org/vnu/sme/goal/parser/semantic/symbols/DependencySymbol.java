package org.vnu.sme.goal.parser.semantic.symbols;

import org.antlr.v4.runtime.Token;

public final class DependencySymbol {
    private final String name;
    private final Token declarationToken;
    private final String dependerRawRef;
    private final String dependeeRawRef;
    private ElementSymbol depender;
    private ElementSymbol dependee;
    private ElementSymbol dependum;

    public DependencySymbol(String name, Token declarationToken, String dependerRawRef, String dependeeRawRef) {
        this.name = name;
        this.declarationToken = declarationToken;
        this.dependerRawRef = dependerRawRef;
        this.dependeeRawRef = dependeeRawRef;
    }

    public String getName() {
        return name;
    }

    public Token getDeclarationToken() {
        return declarationToken;
    }

    public String getDependerRawRef() {
        return dependerRawRef;
    }

    public String getDependeeRawRef() {
        return dependeeRawRef;
    }

    public ElementSymbol getDepender() {
        return depender;
    }

    public void setDepender(ElementSymbol depender) {
        this.depender = depender;
    }

    public ElementSymbol getDependee() {
        return dependee;
    }

    public void setDependee(ElementSymbol dependee) {
        this.dependee = dependee;
    }

    public ElementSymbol getDependum() {
        return dependum;
    }

    public void setDependum(ElementSymbol dependum) {
        this.dependum = dependum;
    }
}

