package org.vnu.sme.goal.mm.ocl;

public class VariableDeclaration {
    private final String name;
    private final String typeName;

    public VariableDeclaration(String name, String typeName) {
        this.name = name;
        this.typeName = typeName;
    }

    public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }
}
