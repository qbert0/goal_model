package org.vnu.sme.goal.ast.ocl;

public final class OclBooleanLiteralExpCS extends OclLiteralExpCS {
    private final boolean value;

    public OclBooleanLiteralExpCS(String text, boolean value) {
        super(text);
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }
}
