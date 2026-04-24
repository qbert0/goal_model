package org.vnu.sme.goal.mm.ocl;

public class RealLiteralExp extends LiteralExp {
    private final double value;

    public RealLiteralExp(String text, double value) {
        super(text);
        this.value = value;
    }

    public double getValue() {
        return value;
    }
}
