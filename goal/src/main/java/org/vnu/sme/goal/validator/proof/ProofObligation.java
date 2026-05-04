package org.vnu.sme.goal.validator.proof;

import org.vnu.sme.goal.mm.ocl.Expression;

public record ProofObligation(
        String actor,
        String goal,
        ProofObligationKind kind,
        String source,
        String target,
        Expression antecedent,
        Expression consequent) {

    public String displayExpression() {
        if (antecedent == null || consequent == null) {
            return "";
        }
        return "(" + antecedent.getText() + ") implies (" + consequent.getText() + ")";
    }
}
