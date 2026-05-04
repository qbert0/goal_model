package org.vnu.sme.goal.ast.ocl;

/**
 * Syntax-level OCL AST node.
 * <p>
 * This layer represents parsed structure only. Runtime OCL semantics are built
 * later by {@code OclModelBuilder} into {@code mm.ocl}.
 */
public abstract class OclExpressionCS {
    private final String text;

    protected OclExpressionCS(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
