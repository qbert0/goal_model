package org.vnu.sme.goal.ast.ocl;

public final class OclRealLiteralExpCS extends OclLiteralExpCS {
    private final double value;

    public OclRealLiteralExpCS(String text, double value) {
        super(text);
        this.value = value;
    }

    public double getValue() {
        return value;
    }
}
