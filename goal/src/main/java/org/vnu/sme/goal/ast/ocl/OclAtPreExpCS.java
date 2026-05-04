package org.vnu.sme.goal.ast.ocl;

public final class OclAtPreExpCS extends OclExpressionCS {
    private final OclExpressionCS source;

    public OclAtPreExpCS(String text, OclExpressionCS source) {
        super(text);
        this.source = source;
    }

    public OclExpressionCS getSource() {
        return source;
    }
}
