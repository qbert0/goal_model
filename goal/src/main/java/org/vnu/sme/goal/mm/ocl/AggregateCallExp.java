package org.vnu.sme.goal.mm.ocl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collection aggregate call invoked through {@code ->} (e.g. {@code coll->size()},
 * {@code coll->includes(x)}, {@code coll->count(x | x > 0)}).
 *
 * <p>The {@link Kind} of an aggregate determines whether the call yields a
 * scalar/boolean (which therefore cannot be chained further with {@code ->}) or
 * a collection. See spec v2 section 6.3.</p>
 */
public class AggregateCallExp extends FeatureCallExp {

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
    private final List<Expression> arguments;
    private final List<VariableDeclaration> iteratorVars;
    private final Expression body;

    public AggregateCallExp(
            String text,
            Expression source,
            Kind kind,
            List<Expression> arguments,
            List<VariableDeclaration> iteratorVars,
            Expression body) {
        super(text, source, keywordOf(kind), false);
        this.kind = kind;
        this.arguments = arguments == null ? Collections.emptyList() : new ArrayList<>(arguments);
        this.iteratorVars = iteratorVars == null ? Collections.emptyList() : new ArrayList<>(iteratorVars);
        this.body = body;
    }

    public Kind getKind() {
        return kind;
    }

    public List<Expression> getArguments() {
        return Collections.unmodifiableList(arguments);
    }

    public List<VariableDeclaration> getIteratorVars() {
        return Collections.unmodifiableList(iteratorVars);
    }

    public Expression getBody() {
        return body;
    }

    /**
     * Aggregates that always return a scalar or boolean — chaining {@code ->}
     * after them is invalid (E3 in spec v2).
     */
    public boolean returnsScalarOrBoolean() {
        return true;
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
