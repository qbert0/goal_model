package org.vnu.sme.goal.ast;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;

public class GoalModelCS {

    private final Token fName;
    private List<ActorDeclCS> actorDeclsCS;
    private List<IntentionalElementCS> ieDeclsCS;
    private List<DependencyCS> dependencyDeclsCS;

    public GoalModelCS(Token fName) {
        this.fName = fName;
        this.actorDeclsCS = new ArrayList<>();
        this.ieDeclsCS = new ArrayList<>();
        this.dependencyDeclsCS = new ArrayList<>();
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

    public List<DependencyCS> getDependencyDeclsCS() {
        return dependencyDeclsCS;
    }

    public void setDependencyDeclsCS(List<DependencyCS> dependencyDeclsCS) {
        this.dependencyDeclsCS = dependencyDeclsCS;
    }

    public void addActorDeclCS(ActorDeclCS actorDeclCS) {
        this.actorDeclsCS.add(actorDeclCS);
    }

    public void addIeDeclCS(IntentionalElementCS ieDeclCS) {
        this.ieDeclsCS.add(ieDeclCS);
    }

    public void addDependencyDeclCS(DependencyCS dependencyDeclCS) {
        this.dependencyDeclsCS.add(dependencyDeclCS);
    }
}