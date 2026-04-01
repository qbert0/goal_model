package org.vnu.sme.goal.ast;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;

public class GoalModelCS {

    private final Token fName;

    private List<ActorDeclCS> actorDeclsCS;
    private List<IntentionalElementCS> ieDeclsCS;
    private List<RelationCS> relationDeclsCS;

    public GoalModelCS(Token fName) {
        this.fName = fName;
        this.actorDeclsCS = new ArrayList<>();
        this.ieDeclsCS = new ArrayList<>();
        this.relationDeclsCS = new ArrayList<>();
    }

    public Token getfName() {
        return fName;
    }

    public List<ActorDeclCS> getActorDeclsCS() {
        return actorDeclsCS;
    }

    public void setActorDeclsCS(List<ActorDeclCS> actorDeclsCS) {
        this.actorDeclsCS = actorDeclsCS;
    }

    public List<IntentionalElementCS> getIeDeclsCS() {
        return ieDeclsCS;
    }

    public void setIeDeclsCS(List<IntentionalElementCS> ieDeclsCS) {
        this.ieDeclsCS = ieDeclsCS;
    }

    public List<RelationCS> getRelationDeclsCS() {
        return relationDeclsCS;
    }

    public void setRelationDeclsCS(List<RelationCS> relationDeclsCS) {
        this.relationDeclsCS = relationDeclsCS;
    }

    public void addActorDeclCS(ActorDeclCS actorDeclCS) {
        this.actorDeclsCS.add(actorDeclCS);
    }

    public void addIeDeclCS(IntentionalElementCS ieDeclCS) {
        this.ieDeclsCS.add(ieDeclCS);
    }

    public void addRelationDeclCS(RelationCS relationDeclCS) {
        this.relationDeclsCS.add(relationDeclCS);
    }
}