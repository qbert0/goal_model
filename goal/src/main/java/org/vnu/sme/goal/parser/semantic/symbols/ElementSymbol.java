package org.vnu.sme.goal.parser.semantic.symbols;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.Token;
import org.vnu.sme.goal.parser.semantic.enums.ElementKind;

public final class ElementSymbol {
    private final String name;
    private final ElementKind kind;
    private final ActorSymbol ownerActor;
    private final Token declarationToken;
    private final List<RelationEntry> relations = new ArrayList<>();
    private boolean leaf = true;

    public ElementSymbol(String name, ElementKind kind, ActorSymbol ownerActor, Token declarationToken) {
        this.name = name;
        this.kind = kind;
        this.ownerActor = ownerActor;
        this.declarationToken = declarationToken;
    }

    public String getName() {
        return name;
    }

    public ElementKind getKind() {
        return kind;
    }

    public ActorSymbol getOwnerActor() {
        return ownerActor;
    }

    public Token getDeclarationToken() {
        return declarationToken;
    }

    public List<RelationEntry> getRelations() {
        return relations;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public void setLeaf(boolean leaf) {
        this.leaf = leaf;
    }

    public String getQualifiedName() {
        return ownerActor.getName() + "." + name;
    }
}

