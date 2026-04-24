package org.vnu.sme.goal.mm.ocl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IteratorExp extends Expression {
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

    private final Expression source;
    private final IteratorKind kind;
    private final String iteratorName;
    private final List<VariableDeclaration> variables;
    private final Expression body;

    public IteratorExp(String text, Expression source, IteratorKind kind, String iteratorName,
            List<VariableDeclaration> variables, Expression body) {
        super(text);
        this.source = source;
        this.kind = kind;
        this.iteratorName = iteratorName;
        this.variables = new ArrayList<>(variables);
        this.body = body;
    }

    public Expression getSource() {
        return source;
    }

    public IteratorKind getKind() {
        return kind;
    }

    public String getIteratorName() {
        return iteratorName;
    }

    public List<VariableDeclaration> getVariables() {
        return Collections.unmodifiableList(variables);
    }

    public Expression getBody() {
        return body;
    }
}
