package org.vnu.sme.goal.mm.ocl;

public class VariableExp extends Expression {
    private final String name;

    public VariableExp(String text, String name) {
        super(text);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
