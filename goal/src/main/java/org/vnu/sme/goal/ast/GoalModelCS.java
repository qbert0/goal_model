package org.vnu.sme.goal.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.Token;

public class GoalModelCS {

    private final Token fName;
    private final List<ActorDeclCS> actorDeclsCS;
    private final List<IntentionalElementCS> ieDeclsCS;
    private final List<RelationCS> relationDeclsCS;

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
        return Collections.unmodifiableList(actorDeclsCS);
    }

    public void setActorDeclsCS(List<ActorDeclCS> actorDeclsCS) {
        this.actorDeclsCS.clear();
        if (actorDeclsCS != null) {
            this.actorDeclsCS.addAll(actorDeclsCS);
        }
    }

    public List<IntentionalElementCS> getIeDeclsCS() {
        return Collections.unmodifiableList(ieDeclsCS);
    }

    public void setIeDeclsCS(List<IntentionalElementCS> ieDeclsCS) {
        this.ieDeclsCS.clear();
        if (ieDeclsCS != null) {
            this.ieDeclsCS.addAll(ieDeclsCS);
        }
    }

    public List<RelationCS> getRelationDeclsCS() {
        return Collections.unmodifiableList(relationDeclsCS);
    }

    public List<DependencyCS> getDependencyDeclsCS() {
        return relationDeclsCS.stream()
                .filter(DependencyCS.class::isInstance)
                .map(DependencyCS.class::cast)
                .collect(Collectors.toUnmodifiableList());
    }

    public void setDependencyDeclsCS(List<DependencyCS> dependencyDeclsCS) {
        relationDeclsCS.clear();
        if (dependencyDeclsCS != null) {
            relationDeclsCS.addAll(dependencyDeclsCS);
        }
    }

    public void addActorDeclCS(ActorDeclCS actorDeclCS) {
        this.actorDeclsCS.add(actorDeclCS);
    }

    public void addIeDeclCS(IntentionalElementCS ieDeclCS) {
        this.ieDeclsCS.add(ieDeclCS);
    }

    public void addDependencyDeclCS(DependencyCS dependencyDeclCS) {
        addRelationDeclCS(dependencyDeclCS);
    }

    public void addRelationDeclCS(RelationCS relationDeclCS) {
        if (relationDeclCS != null) {
            this.relationDeclsCS.add(relationDeclCS);
        }
    }
}
