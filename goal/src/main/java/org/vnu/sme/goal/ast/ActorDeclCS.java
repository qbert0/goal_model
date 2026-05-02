package org.vnu.sme.goal.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.Token;

public abstract class ActorDeclCS extends DescriptionContainerCS {

    protected final Token fName;
    private final List<Token> participatesInRefs = new ArrayList<>();
    private final List<Token> isARefs = new ArrayList<>();
    private final List<Token> wantsRefs = new ArrayList<>();
    private final List<IntentionalElementCS> intentionalElements = new ArrayList<>();

    protected ActorDeclCS(Token fName) {
        this.fName = fName;
    }

    public Token getfName() {
        return fName;
    }

    public List<Token> getParticipatesInRefs() {
        return Collections.unmodifiableList(participatesInRefs);
    }

    public void addParticipatesInRef(Token ref) {
        if (ref != null) {
            participatesInRefs.add(ref);
        }
    }

    public List<Token> getIsARefs() {
        return Collections.unmodifiableList(isARefs);
    }

    public void addIsARef(Token ref) {
        if (ref != null) {
            isARefs.add(ref);
        }
    }

    public List<Token> getWantsRefs() {
        return Collections.unmodifiableList(wantsRefs);
    }

    public void addWantsRef(Token ref) {
        if (ref != null) {
            wantsRefs.add(ref);
        }
    }

    public List<IntentionalElementCS> getIntentionalElements() {
        return Collections.unmodifiableList(intentionalElements);
    }

    public void setIntentionalElements(List<IntentionalElementCS> elements) {
        intentionalElements.clear();
        if (elements != null) {
            intentionalElements.addAll(elements);
        }
    }

    public void addIntentionalElement(IntentionalElementCS element) {
        if (element != null) {
            intentionalElements.add(element);
        }
    }

    // Compatibility API for GoalModelFactory / older parser code.
    public Token getParentRef() {
        return isARefs.isEmpty() ? null : isARefs.get(0);
    }

    public void setParentRef(Token parentRef) {
        isARefs.clear();
        addIsARef(parentRef);
    }

    public Token getInstanceOfRef() {
        return participatesInRefs.isEmpty() ? null : participatesInRefs.get(0);
    }

    public void setInstanceOfRef(Token instanceOfRef) {
        participatesInRefs.clear();
        addParticipatesInRef(instanceOfRef);
    }
}
