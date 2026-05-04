package org.vnu.sme.goal.ast.ocl;

public final class OclUnaryExpCS extends OclExpressionCS {
    public enum Operator {
        NOT,
        NEGATE
    }

    private final Operator operator;
    private final OclExpressionCS operand;

    public OclUnaryExpCS(String text, Operator operator, OclExpressionCS operand) {
        super(text);
        this.operator = operator;
        this.operand = operand;
    }

    public Operator getOperator() {
        return operator;
    }

    public OclExpressionCS getOperand() {
        return operand;
    }
}
