package org.vnu.sme.goal.ast.ocl;

import org.vnu.sme.goal.mm.ocl.VariableDeclaration;

public final class OclVariableDeclarationCS {
    private final String name;
    private final String typeName;

    public OclVariableDeclarationCS(String name, String typeName) {
        this.name = name;
        this.typeName = typeName;
    }

    public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }

    public VariableDeclaration toRuntimeVariable() {
        return new VariableDeclaration(name, typeName);
    }
}
