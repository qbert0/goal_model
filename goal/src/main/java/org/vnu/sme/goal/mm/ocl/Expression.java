package org.vnu.sme.goal.mm.ocl;

public abstract class Expression {
    private String text;

    protected Expression(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
