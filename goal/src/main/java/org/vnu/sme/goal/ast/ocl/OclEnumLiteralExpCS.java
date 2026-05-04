package org.vnu.sme.goal.ast.ocl;

public final class OclEnumLiteralExpCS extends OclLiteralExpCS {
    private final String enumName;
    private final String literalName;

    public OclEnumLiteralExpCS(String text, String enumName, String literalName) {
        super(text);
        this.enumName = enumName;
        this.literalName = literalName;
    }

    public String getEnumName() {
        return enumName;
    }

    public String getLiteralName() {
        return literalName;
    }
}
