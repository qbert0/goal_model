package org.vnu.sme.goal.ast.ocl;

public final class OclVariableExpCS extends OclExpressionCS {
    private final String name;

    public OclVariableExpCS(String text, String name) {
        super(text);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
