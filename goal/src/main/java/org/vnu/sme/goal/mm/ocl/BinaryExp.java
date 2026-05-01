package org.vnu.sme.goal.mm.ocl;

public class BinaryExp extends Expression {
    public enum Operator {
        IMPLIES,
        OR,
        AND,
        EQUALS,
        NOT_EQUALS,
        LT,
        LE,
        GT,
        GE,
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIVIDE
    }

    private final Operator operator;
    private final Expression left;
    private final Expression right;

    public BinaryExp(String text, Operator operator, Expression left, Expression right) {
        super(text);
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    public Operator getOperator() {
        return operator;
    }

    public Expression getLeft() {
        return left;
    }

    public Expression getRight() {
        return right;
    }
}
