package org.vnu.sme.goal.parser;

import java.util.ArrayList;
import java.util.List;

import org.vnu.sme.goal.ast.ocl.OclAggregateCallExpCS;
import org.vnu.sme.goal.ast.ocl.OclAtPreExpCS;
import org.vnu.sme.goal.ast.ocl.OclBinaryExpCS;
import org.vnu.sme.goal.ast.ocl.OclBooleanLiteralExpCS;
import org.vnu.sme.goal.ast.ocl.OclEnumLiteralExpCS;
import org.vnu.sme.goal.ast.ocl.OclExpressionCS;
import org.vnu.sme.goal.ast.ocl.OclFeatureCallExpCS;
import org.vnu.sme.goal.ast.ocl.OclIntegerLiteralExpCS;
import org.vnu.sme.goal.ast.ocl.OclIteratorExpCS;
import org.vnu.sme.goal.ast.ocl.OclNullLiteralExpCS;
import org.vnu.sme.goal.ast.ocl.OclOperationCallExpCS;
import org.vnu.sme.goal.ast.ocl.OclPropertyCallExpCS;
import org.vnu.sme.goal.ast.ocl.OclRealLiteralExpCS;
import org.vnu.sme.goal.ast.ocl.OclSelfExpCS;
import org.vnu.sme.goal.ast.ocl.OclStringLiteralExpCS;
import org.vnu.sme.goal.ast.ocl.OclUnaryExpCS;
import org.vnu.sme.goal.ast.ocl.OclVariableDeclarationCS;
import org.vnu.sme.goal.ast.ocl.OclVariableExpCS;
import org.vnu.sme.goal.mm.ocl.AggregateCallExp;
import org.vnu.sme.goal.mm.ocl.AtPreExp;
import org.vnu.sme.goal.mm.ocl.BinaryExp;
import org.vnu.sme.goal.mm.ocl.BooleanLiteralExp;
import org.vnu.sme.goal.mm.ocl.EnumLiteralExp;
import org.vnu.sme.goal.mm.ocl.Expression;
import org.vnu.sme.goal.mm.ocl.IntegerLiteralExp;
import org.vnu.sme.goal.mm.ocl.IteratorExp;
import org.vnu.sme.goal.mm.ocl.NullLiteralExp;
import org.vnu.sme.goal.mm.ocl.OperationCallExp;
import org.vnu.sme.goal.mm.ocl.PropertyCallExp;
import org.vnu.sme.goal.mm.ocl.RealLiteralExp;
import org.vnu.sme.goal.mm.ocl.SelfExp;
import org.vnu.sme.goal.mm.ocl.StringLiteralExp;
import org.vnu.sme.goal.mm.ocl.UnaryExp;
import org.vnu.sme.goal.mm.ocl.VariableDeclaration;
import org.vnu.sme.goal.mm.ocl.VariableExp;

public final class OclModelBuilder {
    private OclModelBuilder() {
    }

    public static Expression toRuntime(OclExpressionCS astExpression) {
        if (astExpression == null) {
            return null;
        }
        if (astExpression instanceof OclBinaryExpCS binary) {
            return new BinaryExp(binary.getText(), map(binary.getOperator()),
                    toRuntime(binary.getLeft()), toRuntime(binary.getRight()));
        }
        if (astExpression instanceof OclUnaryExpCS unary) {
            return new UnaryExp(unary.getText(), map(unary.getOperator()), toRuntime(unary.getOperand()));
        }
        if (astExpression instanceof OclSelfExpCS self) {
            return new SelfExp(self.getText());
        }
        if (astExpression instanceof OclVariableExpCS variable) {
            return new VariableExp(variable.getText(), variable.getName());
        }
        if (astExpression instanceof OclPropertyCallExpCS property) {
            return new PropertyCallExp(property.getText(), toRuntime(property.getSource()),
                    property.getFeatureName(), property.isAtPre());
        }
        if (astExpression instanceof OclOperationCallExpCS operation) {
            return new OperationCallExp(operation.getText(), toRuntime(operation.getSource()),
                    operation.getFeatureName(), operation.isAtPre(), toRuntimeExpressions(operation.getArguments()));
        }
        if (astExpression instanceof OclIteratorExpCS iterator) {
            return new IteratorExp(iterator.getText(), toRuntime(iterator.getSource()), map(iterator.getKind()),
                    iterator.getIteratorName(), toRuntimeVariables(iterator.getVariables()), toRuntime(iterator.getBody()));
        }
        if (astExpression instanceof OclAggregateCallExpCS aggregate) {
            return new AggregateCallExp(aggregate.getText(), toRuntime(aggregate.getSource()), map(aggregate.getKind()),
                    toRuntimeExpressions(aggregate.getArguments()), toRuntimeVariables(aggregate.getIteratorVars()),
                    toRuntime(aggregate.getBody()));
        }
        if (astExpression instanceof OclAtPreExpCS atPre) {
            return new AtPreExp(atPre.getText(), toRuntime(atPre.getSource()));
        }
        if (astExpression instanceof OclStringLiteralExpCS stringLiteral) {
            return new StringLiteralExp(stringLiteral.getText(), stringLiteral.getValue());
        }
        if (astExpression instanceof OclIntegerLiteralExpCS integerLiteral) {
            return new IntegerLiteralExp(integerLiteral.getText(), integerLiteral.getValue());
        }
        if (astExpression instanceof OclRealLiteralExpCS realLiteral) {
            return new RealLiteralExp(realLiteral.getText(), realLiteral.getValue());
        }
        if (astExpression instanceof OclBooleanLiteralExpCS booleanLiteral) {
            return new BooleanLiteralExp(booleanLiteral.getText(), booleanLiteral.getValue());
        }
        if (astExpression instanceof OclNullLiteralExpCS nullLiteral) {
            return new NullLiteralExp(nullLiteral.getText());
        }
        if (astExpression instanceof OclEnumLiteralExpCS enumLiteral) {
            return new EnumLiteralExp(enumLiteral.getText(), enumLiteral.getEnumName(), enumLiteral.getLiteralName());
        }
        throw new IllegalArgumentException("Unsupported OCL AST node: " + astExpression.getClass().getName());
    }

    public static List<VariableDeclaration> toRuntimeVariables(List<OclVariableDeclarationCS> astVariables) {
        List<VariableDeclaration> result = new ArrayList<>();
        if (astVariables == null) {
            return result;
        }
        for (OclVariableDeclarationCS astVariable : astVariables) {
            result.add(astVariable.toRuntimeVariable());
        }
        return result;
    }

    private static List<Expression> toRuntimeExpressions(List<OclExpressionCS> astExpressions) {
        List<Expression> result = new ArrayList<>();
        if (astExpressions == null) {
            return result;
        }
        for (OclExpressionCS astExpression : astExpressions) {
            result.add(toRuntime(astExpression));
        }
        return result;
    }

    private static BinaryExp.Operator map(OclBinaryExpCS.Operator operator) {
        return switch (operator) {
            case IMPLIES -> BinaryExp.Operator.IMPLIES;
            case OR -> BinaryExp.Operator.OR;
            case AND -> BinaryExp.Operator.AND;
            case EQUALS -> BinaryExp.Operator.EQUALS;
            case NOT_EQUALS -> BinaryExp.Operator.NOT_EQUALS;
            case LT -> BinaryExp.Operator.LT;
            case LE -> BinaryExp.Operator.LE;
            case GT -> BinaryExp.Operator.GT;
            case GE -> BinaryExp.Operator.GE;
            case ADD -> BinaryExp.Operator.ADD;
            case SUBTRACT -> BinaryExp.Operator.SUBTRACT;
            case MULTIPLY -> BinaryExp.Operator.MULTIPLY;
            case DIVIDE -> BinaryExp.Operator.DIVIDE;
        };
    }

    private static UnaryExp.Operator map(OclUnaryExpCS.Operator operator) {
        return switch (operator) {
            case NOT -> UnaryExp.Operator.NOT;
            case NEGATE -> UnaryExp.Operator.NEGATE;
        };
    }

    private static IteratorExp.IteratorKind map(OclIteratorExpCS.IteratorKind kind) {
        return switch (kind) {
            case EXISTS -> IteratorExp.IteratorKind.EXISTS;
            case FOR_ALL -> IteratorExp.IteratorKind.FOR_ALL;
            case COLLECT -> IteratorExp.IteratorKind.COLLECT;
            case SELECT -> IteratorExp.IteratorKind.SELECT;
            case REJECT -> IteratorExp.IteratorKind.REJECT;
            case ANY -> IteratorExp.IteratorKind.ANY;
            case ONE -> IteratorExp.IteratorKind.ONE;
            case IS_UNIQUE -> IteratorExp.IteratorKind.IS_UNIQUE;
            case SORTED_BY -> IteratorExp.IteratorKind.SORTED_BY;
            case CLOSURE -> IteratorExp.IteratorKind.CLOSURE;
            case UNKNOWN -> IteratorExp.IteratorKind.UNKNOWN;
        };
    }

    private static AggregateCallExp.Kind map(OclAggregateCallExpCS.Kind kind) {
        return switch (kind) {
            case SIZE -> AggregateCallExp.Kind.SIZE;
            case SUM -> AggregateCallExp.Kind.SUM;
            case MAX -> AggregateCallExp.Kind.MAX;
            case MIN -> AggregateCallExp.Kind.MIN;
            case IS_EMPTY -> AggregateCallExp.Kind.IS_EMPTY;
            case NOT_EMPTY -> AggregateCallExp.Kind.NOT_EMPTY;
            case INCLUDES -> AggregateCallExp.Kind.INCLUDES;
            case EXCLUDES -> AggregateCallExp.Kind.EXCLUDES;
            case COUNT -> AggregateCallExp.Kind.COUNT;
        };
    }
}
