package org.vnu.sme.goal.ast;

import org.antlr.v4.runtime.Token;

public class DependencyCS extends RelationCS {
    private Token dependerRef;
    private Token dependeeRef;
    private Token dependumRef;
    private String dependerQualifiedName;
    private String dependeeQualifiedName;
    private IntentionalElementCS dependumElement;

    public DependencyCS(Token fName) {
        super(fName);
    }

    public Token getDependerRef() {
        return dependerRef;
    }

    public void setDependerRef(Token dependerRef) {
        this.dependerRef = dependerRef;
        this.dependerQualifiedName = dependerRef == null ? null : dependerRef.getText();
    }

    public void setDependerRef(String dependerQualifiedName) {
        this.dependerQualifiedName = dependerQualifiedName;
    }

    public Token getDependeeRef() {
        return dependeeRef;
    }

    public void setDependeeRef(Token dependeeRef) {
        this.dependeeRef = dependeeRef;
        this.dependeeQualifiedName = dependeeRef == null ? null : dependeeRef.getText();
    }

    public void setDependeeRef(String dependeeQualifiedName) {
        this.dependeeQualifiedName = dependeeQualifiedName;
    }

    public Token getDependumRef() {
        return dependumRef;
    }

    public void setDependumRef(Token dependumRef) {
        this.dependumRef = dependumRef;
    }

    public String getDependerQualifiedName() {
        return dependerQualifiedName;
    }

    public void setDependerQualifiedName(String dependerQualifiedName) {
        this.dependerQualifiedName = dependerQualifiedName;
    }

    public String getDependeeQualifiedName() {
        return dependeeQualifiedName;
    }

    public void setDependeeQualifiedName(String dependeeQualifiedName) {
        this.dependeeQualifiedName = dependeeQualifiedName;
    }

    public IntentionalElementCS getDependumElement() {
        return dependumElement;
    }

    public void setDependumElement(IntentionalElementCS dependumElement) {
        this.dependumElement = dependumElement;
        this.dependumRef = dependumElement == null ? null : dependumElement.getfName();
    }

    // Compatibility API for the current GoalModelFactory.
    public IntentionalElementCS getDependum() {
        return dependumElement;
    }

    public void setDependum(IntentionalElementCS dependum) {
        setDependumElement(dependum);
    }
}
