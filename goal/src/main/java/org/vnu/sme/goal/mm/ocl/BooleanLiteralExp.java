package org.vnu.sme.goal.mm.ocl;

public class BooleanLiteralExp extends LiteralExp {
    private final boolean value;

    public BooleanLiteralExp(String text, boolean value) {
        super(text);
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }
}
