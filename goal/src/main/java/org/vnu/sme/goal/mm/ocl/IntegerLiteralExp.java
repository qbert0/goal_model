package org.vnu.sme.goal.mm.ocl;

public class IntegerLiteralExp extends LiteralExp {
    private final int value;

    public IntegerLiteralExp(String text, int value) {
        super(text);
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
