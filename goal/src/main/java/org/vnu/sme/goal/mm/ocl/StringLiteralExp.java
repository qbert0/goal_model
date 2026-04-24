package org.vnu.sme.goal.mm.ocl;

public class StringLiteralExp extends LiteralExp {
    private final String value;

    public StringLiteralExp(String text, String value) {
        super(text);
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
