package org.vnu.sme.goal.parser.semantic.symbols;

import java.util.Collections;
import java.util.List;
import org.antlr.v4.runtime.Token;
import org.vnu.sme.goal.ast.ocl.OclExpressionCS;
import org.vnu.sme.goal.ast.ocl.OclVariableDeclarationCS;
import org.vnu.sme.goal.mm.ocl.Expression;
import org.vnu.sme.goal.mm.ocl.VariableDeclaration;
import org.vnu.sme.goal.parser.OclModelBuilder;
import org.vnu.sme.goal.parser.semantic.enums.GoalContractType;

/**
 * OCL contract attached to a goal {@link ElementSymbol}.
 * <p>Mirrors spec v2 section 3 (goal predicate kinds) and section 3.3
 * ({@code achieve for unique}).</p>
 *
 * <ul>
 *   <li>{@code type} — which goal predicate this contract carries.</li>
 *   <li>{@code bodyExpr} — the constraint expression after the {@code :} colon.</li>
 *   <li>{@code iterVars} / {@code sourceExpr} — only populated for
 *       {@link GoalContractType#ACHIEVE_UNIQUE}; spec v2 §3.3.</li>
 *   <li>{@code keywordToken} — token of the clause keyword
 *       ({@code achieve}/{@code maintain}/{@code avoid}) used when reporting
 *       semantic issues against this contract (E1, E2, E4, E5).</li>
 * </ul>
 */
public final class GoalContract {
    private final GoalContractType type;
    private final Token keywordToken;
    private final OclExpressionCS bodyExpr;
    private final OclExpressionCS sourceExpr;
    private final List<OclVariableDeclarationCS> iterVars;

    public GoalContract(
            GoalContractType type,
            Token keywordToken,
            OclExpressionCS bodyExpr,
            OclExpressionCS sourceExpr,
            List<OclVariableDeclarationCS> iterVars) {
        this.type = type;
        this.keywordToken = keywordToken;
        this.bodyExpr = bodyExpr;
        this.sourceExpr = sourceExpr;
        this.iterVars = iterVars == null ? Collections.emptyList() : List.copyOf(iterVars);
    }

    public GoalContractType getType() {
        return type;
    }

    public Token getKeywordToken() {
        return keywordToken;
    }

    public OclExpressionCS getBodyExpr() {
        return bodyExpr;
    }

    public Expression getBodyExprRuntime() {
        return OclModelBuilder.toRuntime(bodyExpr);
    }

    public OclExpressionCS getSourceExpr() {
        return sourceExpr;
    }

    public Expression getSourceExprRuntime() {
        return OclModelBuilder.toRuntime(sourceExpr);
    }

    public List<OclVariableDeclarationCS> getIterVars() {
        return iterVars;
    }

    public List<VariableDeclaration> getRuntimeIterVars() {
        return iterVars.stream().map(OclVariableDeclarationCS::toRuntimeVariable).toList();
    }
}
