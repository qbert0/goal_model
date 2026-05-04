package org.vnu.sme.goal.parser.semantic.symbols;

import org.antlr.v4.runtime.Token;
import org.vnu.sme.goal.ast.ocl.OclExpressionCS;
import org.vnu.sme.goal.mm.ocl.Expression;
import org.vnu.sme.goal.parser.OclModelBuilder;

/**
 * OCL contract attached to a task {@link ElementSymbol}.
 * <p>The grammar defines {@code preClause?} and {@code postClause?} independently
 * (spec v2 §4); either, both, or neither may be present.</p>
 */
public final class TaskContract {
    private final Token preToken;
    private final Token postToken;
    private final OclExpressionCS precondition;
    private final OclExpressionCS postcondition;

    public TaskContract(Token preToken, OclExpressionCS precondition, Token postToken, OclExpressionCS postcondition) {
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

    public OclExpressionCS getPrecondition() {
        return precondition;
    }

    public Expression getPreconditionRuntime() {
        return OclModelBuilder.toRuntime(precondition);
    }

    public OclExpressionCS getPostcondition() {
        return postcondition;
    }

    public Expression getPostconditionRuntime() {
        return OclModelBuilder.toRuntime(postcondition);
    }

    public boolean isEmpty() {
        return precondition == null && postcondition == null;
    }
}
