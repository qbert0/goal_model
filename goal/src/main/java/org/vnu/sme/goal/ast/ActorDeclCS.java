package org.vnu.sme.goal.ast;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;

public abstract class ActorDeclCS extends DescriptionContainerCS {

    protected final Token fName;
    protected Token parentRef;           // Sau COLON
    protected Token instanceOfRef;       // Sau GT
    protected List<IntentionalElementCS> intentionalElements;

    public ActorDeclCS(Token fName) {
        this.fName = fName;
        this.intentionalElements = new ArrayList<>();
    }

    public Token getfName() {
        return fName;
    }

    public Token getParentRef() {
        return parentRef;
    }

    public void setParentRef(Token parentRef) {
        this.parentRef = parentRef;
    }

    public Token getInstanceOfRef() {
        return instanceOfRef;
    }

    public void setInstanceOfRef(Token instanceOfRef) {
        this.instanceOfRef = instanceOfRef;
    }

    public List<IntentionalElementCS> getIntentionalElements() {
        return intentionalElements;
    }

    public void setIntentionalElements(List<IntentionalElementCS> intentionalElements) {
        this.intentionalElements = intentionalElements;
    }

    public void addIntentionalElement(IntentionalElementCS element) {
        this.intentionalElements.add(element);
    }
}