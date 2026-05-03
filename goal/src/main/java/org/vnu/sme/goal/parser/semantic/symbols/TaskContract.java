package org.vnu.sme.goal.parser.semantic.symbols;

import org.antlr.v4.runtime.Token;
import org.vnu.sme.goal.mm.ocl.Expression;

/**
 * OCL contract attached to a task {@link ElementSymbol}.
 * <p>The grammar defines {@code preClause?} and {@code postClause?} independently
 * (spec v2 §4); either, both, or neither may be present.</p>
 */
public final class TaskContract {
    private final Token preToken;
    private final Token postToken;
    private final Expression precondition;
    private final Expression postcondition;

    public TaskContract(Token preToken, Expression precondition, Token postToken, Expression postcondition) {
        this.preToken = preToken;
        this.postToken = postToken;
        this.precondition = precondition;
        this.postcondition = postcondition;
    }

    public Token getPreToken() {
        return preToken;
    }

    public Token getPostToken() {
        return postToken;
    }

    public Expression getPrecondition() {
        return precondition;
    }

    public Expression getPostcondition() {
        return postcondition;
    }

    public boolean isEmpty() {
        return precondition == null && postcondition == null;
    }
}
