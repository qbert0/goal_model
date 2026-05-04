package org.vnu.sme.goal.ast.ocl;

public final class OclBinaryExpCS extends OclExpressionCS {
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
    private final OclExpressionCS left;
    private final OclExpressionCS right;

    public OclBinaryExpCS(String text, Operator operator, OclExpressionCS left, OclExpressionCS right) {
        super(text);
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    public Operator getOperator() {
        return operator;
    }

    public OclExpressionCS getLeft() {
        return left;
    }

    public OclExpressionCS getRight() {
        return right;
    }
}
