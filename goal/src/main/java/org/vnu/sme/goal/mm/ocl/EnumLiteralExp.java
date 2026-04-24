package org.vnu.sme.goal.mm.ocl;

public class EnumLiteralExp extends LiteralExp {
    private final String enumName;
    private final String literalName;

    public EnumLiteralExp(String text, String enumName, String literalName) {
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
