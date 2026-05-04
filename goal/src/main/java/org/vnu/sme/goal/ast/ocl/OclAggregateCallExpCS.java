package org.vnu.sme.goal.ast.ocl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OclAggregateCallExpCS extends OclFeatureCallExpCS {
    public enum Kind {
        SIZE,
        SUM,
        MAX,
        MIN,
        IS_EMPTY,
        NOT_EMPTY,
        INCLUDES,
        EXCLUDES,
        COUNT
    }

    private final Kind kind;
    private final List<OclExpressionCS> arguments;
    private final List<OclVariableDeclarationCS> iteratorVars;
    private final OclExpressionCS body;

    public OclAggregateCallExpCS(String text, OclExpressionCS source, Kind kind,
            List<OclExpressionCS> arguments, List<OclVariableDeclarationCS> iteratorVars, OclExpressionCS body) {
        super(text, source, keywordOf(kind), false);
        this.kind = kind;
        this.arguments = arguments == null ? Collections.emptyList() : new ArrayList<>(arguments);
        this.iteratorVars = iteratorVars == null ? Collections.emptyList() : new ArrayList<>(iteratorVars);
        this.body = body;
    }

    public Kind getKind() {
        return kind;
    }

    public List<OclExpressionCS> getArguments() {
        return Collections.unmodifiableList(arguments);
    }

    public List<OclVariableDeclarationCS> getIteratorVars() {
        return Collections.unmodifiableList(iteratorVars);
    }

    public OclExpressionCS getBody() {
        return body;
    }

    private static String keywordOf(Kind kind) {
        return switch (kind) {
            case SIZE -> "size";
            case SUM -> "sum";
            case MAX -> "max";
            case MIN -> "min";
            case IS_EMPTY -> "isEmpty";
            case NOT_EMPTY -> "notEmpty";
            case INCLUDES -> "includes";
            case EXCLUDES -> "excludes";
            case COUNT -> "count";
        };
    }
}
