package org.vnu.sme.goal.mm.ocl;

public class AtPreExp extends Expression {
    private final Expression source;

    public AtPreExp(String text, Expression source) {
        super(text);
        this.source = source;
    }

    public Expression getSource() {
        return source;
    }
}
