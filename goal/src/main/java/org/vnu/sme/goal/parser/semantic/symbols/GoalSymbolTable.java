package org.vnu.sme.goal.parser.semantic.symbols;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class GoalSymbolTable {
    private final String modelName;
    private final Map<String, ActorSymbol> actorsByName = new LinkedHashMap<>();
    private final Map<String, ElementSymbol> elementsByQualifiedName = new LinkedHashMap<>();
    private final Map<String, DependencySymbol> dependenciesByName = new LinkedHashMap<>();

    public GoalSymbolTable(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }

    public Map<String, ActorSymbol> getActorsByName() {
        return actorsByName;
    }

    public Map<String, ElementSymbol> getElementsByQualifiedName() {
        return elementsByQualifiedName;
    }

    public Map<String, DependencySymbol> getDependenciesByName() {
        return dependenciesByName;
    }

    public Optional<ActorSymbol> resolveActor(String actorName) {
        return Optional.ofNullable(actorsByName.get(actorName));
    }

    public Optional<ElementSymbol> resolveElement(String qualifiedName) {
        return Optional.ofNullable(elementsByQualifiedName.get(qualifiedName));
    }
}

