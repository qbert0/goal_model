package org.vnu.sme.goal.ast.ocl;

public final class OclIntegerLiteralExpCS extends OclLiteralExpCS {
    private final int value;

    public OclIntegerLiteralExpCS(String text, int value) {
        super(text);
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
