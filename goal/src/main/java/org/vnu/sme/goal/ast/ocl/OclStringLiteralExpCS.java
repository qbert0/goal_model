package org.vnu.sme.goal.ast.ocl;

public final class OclStringLiteralExpCS extends OclLiteralExpCS {
    private final String value;

    public OclStringLiteralExpCS(String text, String value) {
        super(text);
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
