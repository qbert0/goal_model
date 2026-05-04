package org.vnu.sme.goal.ast.ocl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OclIteratorExpCS extends OclExpressionCS {
    public enum IteratorKind {
        EXISTS,
        FOR_ALL,
        COLLECT,
        SELECT,
        REJECT,
        ANY,
        ONE,
        IS_UNIQUE,
        SORTED_BY,
        CLOSURE,
        UNKNOWN
    }

    private final OclExpressionCS source;
    private final IteratorKind kind;
    private final String iteratorName;
    private final List<OclVariableDeclarationCS> variables;
    private final OclExpressionCS body;

    public OclIteratorExpCS(String text, OclExpressionCS source, IteratorKind kind, String iteratorName,
            List<OclVariableDeclarationCS> variables, OclExpressionCS body) {
        super(text);
        this.source = source;
        this.kind = kind;
        this.iteratorName = iteratorName;
        this.variables = variables == null ? Collections.emptyList() : new ArrayList<>(variables);
        this.body = body;
    }

    public OclExpressionCS getSource() {
        return source;
    }

    public IteratorKind getKind() {
        return kind;
    }

    public String getIteratorName() {
        return iteratorName;
    }

    public List<OclVariableDeclarationCS> getVariables() {
        return Collections.unmodifiableList(variables);
    }

    public OclExpressionCS getBody() {
        return body;
    }
}
