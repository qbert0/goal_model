package org.vnu.sme.goal.ast;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;

public abstract class ActorDeclCS extends DescriptionContainerCS {

    protected final Token fName;
    protected List<Token> participatesInRefs;
    protected List<Token> isARefs;
    protected List<Token> wantsRefs;

    public ActorDeclCS(Token fName) {
        this.fName = fName;
        this.participatesInRefs = new ArrayList<>();
        this.isARefs = new ArrayList<>();
        this.wantsRefs = new ArrayList<>();
    }

    public Token getfName() {
        return fName;
    }

    public List<Token> getParticipatesInRefs() {
        return participatesInRefs;
    }

    public void setParticipatesInRefs(List<Token> participatesInRefs) {
        this.participatesInRefs = participatesInRefs;
    }

    public void addParticipatesInRef(Token ref) {
        this.participatesInRefs.add(ref);
    }

    public List<Token> getIsARefs() {
        return isARefs;
    }

    public void setIsARefs(List<Token> isARefs) {
        this.isARefs = isARefs;
    }

    public void addIsARef(Token ref) {
        this.isARefs.add(ref);
    }

    public List<Token> getWantsRefs() {
        return wantsRefs;
    }

    public void setWantsRefs(List<Token> wantsRefs) {
        this.wantsRefs = wantsRefs;
    }

    public void addWantsRef(Token ref) {
        this.wantsRefs.add(ref);
    }
}