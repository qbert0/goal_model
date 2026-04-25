package org.vnu.sme.goal.parser.semantic.symbols;

import java.util.LinkedHashMap;
import java.util.Map;
import org.antlr.v4.runtime.Token;
import org.vnu.sme.goal.parser.semantic.enums.ActorKind;

public final class ActorSymbol {
    private final String name;
    private final ActorKind kind;
    private final Token declarationToken;
    private final Map<String, ElementSymbol> elementTable = new LinkedHashMap<>();

    public ActorSymbol(String name, ActorKind kind, Token declarationToken) {
        this.name = name;
        this.kind = kind;
        this.declarationToken = declarationToken;
    }

    public String getName() {
        return name;
    }

    public ActorKind getKind() {
        return kind;
    }

    public Token getDeclarationToken() {
        return declarationToken;
    }

    public Map<String, ElementSymbol> getElementTable() {
        return elementTable;
    }
}

