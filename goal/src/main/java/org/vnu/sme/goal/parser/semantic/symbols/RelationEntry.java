package org.vnu.sme.goal.parser.semantic.symbols;

import org.antlr.v4.runtime.Token;
import org.vnu.sme.goal.ast.OutgoingLink;

public final class RelationEntry {
    private final OutgoingLink.Kind operator;
    private final Token targetRef;
    private ElementSymbol resolvedTarget;

    public RelationEntry(OutgoingLink.Kind operator, Token targetRef) {
        this.operator = operator;
        this.targetRef = targetRef;
    }

    public OutgoingLink.Kind getOperator() {
        return operator;
    }

    public Token getTargetRef() {
        return targetRef;
    }

    public ElementSymbol getResolvedTarget() {
        return resolvedTarget;
    }

    public void setResolvedTarget(ElementSymbol resolvedTarget) {
        this.resolvedTarget = resolvedTarget;
    }
}

