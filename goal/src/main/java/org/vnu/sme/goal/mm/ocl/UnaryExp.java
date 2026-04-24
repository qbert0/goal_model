package org.vnu.sme.goal.mm.ocl;

public class UnaryExp extends Expression {
    public enum Operator {
        NOT,
        NEGATE
    }

    private final Operator operator;
    private final Expression operand;

    public UnaryExp(String text, Operator operator, Expression operand) {
        super(text);
        this.operator = operator;
        this.operand = operand;
    }

    public Operator getOperator() {
        return operator;
    }

    public Expression getOperand() {
        return operand;
    }
}
