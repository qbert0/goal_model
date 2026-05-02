package org.vnu.sme.goal.parser;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
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

final class OclExpressionBuilder {
    private OclExpressionBuilder() {
    }

    static Expression build(GOALParser.ExpressionContext ctx) {
        return buildConditionalImplies(ctx.conditionalImpliesExpression());
    }

    private static Expression buildConditionalImplies(GOALParser.ConditionalImpliesExpressionContext ctx) {
        Expression current = buildConditionalOr(ctx.conditionalOrExpression(0));
        for (int i = 1; i < ctx.conditionalOrExpression().size(); i++) {
            Expression right = buildConditionalOr(ctx.conditionalOrExpression(i));
            current = new BinaryExp(binaryText(current, "implies", right), BinaryExp.Operator.IMPLIES,
                    current, right);
        }
        return current;
    }

    private static Expression buildConditionalOr(GOALParser.ConditionalOrExpressionContext ctx) {
        Expression current = buildConditionalXor(ctx.conditionalXOrExpression(0));
        for (int i = 1; i < ctx.conditionalXOrExpression().size(); i++) {
            Expression right = buildConditionalXor(ctx.conditionalXOrExpression(i));
            current = new BinaryExp(binaryText(current, "or", right), BinaryExp.Operator.OR,
                    current, right);
        }
        return current;
    }

    private static Expression buildConditionalXor(GOALParser.ConditionalXOrExpressionContext ctx) {
        Expression current = buildConditionalAnd(ctx.conditionalAndExpression(0));
        for (int i = 1; i < ctx.conditionalAndExpression().size(); i++) {
            Expression right = buildConditionalAnd(ctx.conditionalAndExpression(i));
            current = new BinaryExp(binaryText(current, "xor", right), BinaryExp.Operator.OR,
                    current, right);
        }
        return current;
    }

    private static Expression buildConditionalAnd(GOALParser.ConditionalAndExpressionContext ctx) {
        Expression current = buildEquality(ctx.equalityExpression(0));
        for (int i = 1; i < ctx.equalityExpression().size(); i++) {
            Expression right = buildEquality(ctx.equalityExpression(i));
            current = new BinaryExp(binaryText(current, "and", right), BinaryExp.Operator.AND,
                    current, right);
        }
        return current;
    }

    private static Expression buildEquality(GOALParser.EqualityExpressionContext ctx) {
        Expression current = buildRelational(ctx.relationalExpression(0));
        for (int i = 1; i < ctx.relationalExpression().size(); i++) {
            Token operator = (Token) ctx.getChild(2 * i - 1).getPayload();
            BinaryExp.Operator mapped = "=".equals(operator.getText())
                    ? BinaryExp.Operator.EQUALS
                    : BinaryExp.Operator.NOT_EQUALS;
            Expression right = buildRelational(ctx.relationalExpression(i));
            current = new BinaryExp(binaryText(current, operator.getText(), right), mapped,
                    current, right);
        }
        return current;
    }

    private static Expression buildRelational(GOALParser.RelationalExpressionContext ctx) {
        Expression current = buildAdditive(ctx.additiveExpression(0));
        for (int i = 1; i < ctx.additiveExpression().size(); i++) {
            Token operator = (Token) ctx.getChild(2 * i - 1).getPayload();
            Expression right = buildAdditive(ctx.additiveExpression(i));
            current = new BinaryExp(binaryText(current, operator.getText(), right), mapRelational(operator),
                    current, right);
        }
        return current;
    }

    private static BinaryExp.Operator mapRelational(Token operator) {
        return switch (operator.getText()) {
            case "<" -> BinaryExp.Operator.LT;
            case "<=" -> BinaryExp.Operator.LE;
            case ">" -> BinaryExp.Operator.GT;
            default -> BinaryExp.Operator.GE;
        };
    }

    private static Expression buildAdditive(GOALParser.AdditiveExpressionContext ctx) {
        Expression current = buildMultiplicative(ctx.multiplicativeExpression(0));
        for (int i = 1; i < ctx.multiplicativeExpression().size(); i++) {
            Token operator = (Token) ctx.getChild(2 * i - 1).getPayload();
            BinaryExp.Operator mapped = "+".equals(operator.getText())
                    ? BinaryExp.Operator.ADD
                    : BinaryExp.Operator.SUBTRACT;
            Expression right = buildMultiplicative(ctx.multiplicativeExpression(i));
            current = new BinaryExp(binaryText(current, operator.getText(), right), mapped,
                    current, right);
        }
        return current;
    }

    private static Expression buildMultiplicative(GOALParser.MultiplicativeExpressionContext ctx) {
        Expression current = buildUnary(ctx.unaryExpression(0));
        for (int i = 1; i < ctx.unaryExpression().size(); i++) {
            Token operator = (Token) ctx.getChild(2 * i - 1).getPayload();
            BinaryExp.Operator mapped = "*".equals(operator.getText())
                    ? BinaryExp.Operator.MULTIPLY
                    : BinaryExp.Operator.DIVIDE;
            Expression right = buildUnary(ctx.unaryExpression(i));
            current = new BinaryExp(binaryText(current, operator.getText(), right), mapped,
                    current, right);
        }
        return current;
    }

    private static Expression buildUnary(GOALParser.UnaryExpressionContext ctx) {
        if (ctx.NOT() != null) {
            return new UnaryExp(ctx.getText(), UnaryExp.Operator.NOT, buildUnary(ctx.unaryExpression()));
        }
        if (ctx.MINUS() != null) {
            return new UnaryExp(ctx.getText(), UnaryExp.Operator.NEGATE, buildUnary(ctx.unaryExpression()));
        }
        if (ctx.PLUS() != null) {
            return buildUnary(ctx.unaryExpression());
        }
        return buildPostfix(ctx.postfixExpression());
    }

    private static Expression buildPostfix(GOALParser.PostfixExpressionContext ctx) {
        Expression current = buildPrimary(ctx.primaryExpression());
        for (GOALParser.PathSuffixContext suffix : ctx.pathSuffix()) {
            current = applySuffix(current, suffix);
        }
        return current;
    }

    private static Expression buildPrimary(GOALParser.PrimaryExpressionContext ctx) {
        if (ctx.literal() != null) {
            return buildLiteral(ctx.literal());
        }
        if (ctx.primaryAtom() != null) {
            return buildPrimaryAtom(ctx.primaryAtom());
        }
        if (ctx.expression() != null) {
            return build(ctx.expression());
        }
        throw new IllegalStateException("Unsupported primary expression: " + ctx.getText());
    }

    private static Expression buildLiteral(GOALParser.LiteralContext ctx) {
        if (ctx.STRING() != null) {
            return new StringLiteralExp(ctx.getText(), ctx.getText().substring(1, ctx.getText().length() - 1));
        }
        if (ctx.INT() != null) {
            return new IntegerLiteralExp(ctx.getText(), Integer.parseInt(ctx.getText()));
        }
        if (ctx.REAL() != null) {
            return new RealLiteralExp(ctx.getText(), Double.parseDouble(ctx.getText()));
        }
        if (ctx.TRUE() != null) {
            return new BooleanLiteralExp(ctx.getText(), true);
        }
        if (ctx.FALSE() != null) {
            return new BooleanLiteralExp(ctx.getText(), false);
        }
        if (ctx.NULL() != null) {
            return new NullLiteralExp(ctx.getText());
        }
        GOALParser.EnumLiteralContext enumLiteral = ctx.enumLiteral();
        return new EnumLiteralExp(enumLiteral.getText(), enumLiteral.IDENT(0).getText(),
                enumLiteral.IDENT(1).getText());
    }

    private static Expression buildPrimaryAtom(GOALParser.PrimaryAtomContext ctx) {
        if (ctx.SELF() != null) {
            return new SelfExp(ctx.getText());
        }
        return new VariableExp(ctx.getText(), ctx.IDENT().getText());
    }

    private static Expression applySuffix(Expression source, GOALParser.PathSuffixContext suffix) {
        if (suffix.IDENT() != null) {
            boolean atPre = suffix.AT_PRE() != null;
            return new PropertyCallExp(suffixText(source, suffix), source, suffix.IDENT().getText(), atPre);
        }
        if (suffix.simpleCall() != null) {
            return buildSimpleCall(source, suffix.simpleCall(), suffixText(source, suffix), false);
        }
        if (suffix.collectionCall() != null) {
            return buildCollectionCall(source, suffix.collectionCall(), suffixText(source, suffix));
        }
        return new AtPreExp(suffixText(source, suffix), source);
    }

    private static Expression buildCollectionCall(Expression source, GOALParser.CollectionCallContext ctx, String text) {
        if (ctx.iteratorCall() != null) {
            GOALParser.IteratorCallContext iterator = ctx.iteratorCall();
            String iteratorName = iterator.iteratorOp().getText();
            return new IteratorExp(text, source, mapIteratorKind(iteratorName), iteratorName,
                    buildIteratorVars(iterator.iteratorVars()), build(iterator.expression()));
        }
        if (ctx.aggregateCall() != null) {
            return buildAggregateCall(source, ctx.aggregateCall(), text);
        }
        return buildSimpleCall(source, ctx.simpleCall(), text, false);
    }

    private static Expression buildAggregateCall(Expression source, GOALParser.AggregateCallContext ctx, String text) {
        if (ctx.aggregateOp() != null) {
            List<Expression> arguments = new ArrayList<>();
            if (ctx.argumentList() != null) {
                for (GOALParser.ExpressionContext argument : ctx.argumentList().expression()) {
                    arguments.add(build(argument));
                }
            }
            return new OperationCallExp(text, source, ctx.aggregateOp().getText(), false, arguments);
        }

        return new IteratorExp(text, source, IteratorExp.IteratorKind.UNKNOWN, ctx.COUNT().getText(),
                buildIteratorVars(ctx.iteratorVars()), build(ctx.expression()));
    }

    private static OperationCallExp buildSimpleCall(Expression source, GOALParser.SimpleCallContext ctx, String text,
            boolean atPre) {
        List<Expression> arguments = new ArrayList<>();
        if (ctx.argumentList() != null) {
            for (GOALParser.ExpressionContext argument : ctx.argumentList().expression()) {
                arguments.add(build(argument));
            }
        }
        return new OperationCallExp(text, source, ctx.IDENT().getText(), atPre, arguments);
    }

    private static List<VariableDeclaration> buildIteratorVars(GOALParser.IteratorVarsContext ctx) {
        List<VariableDeclaration> variables = new ArrayList<>();
        List<TerminalNode> names = ctx.IDENT();
        List<GOALParser.QualifiedNameContext> types = ctx.qualifiedName();
        if (types.isEmpty()) {
            for (TerminalNode name : names) {
                variables.add(new VariableDeclaration(name.getText(), null));
            }
            return variables;
        }
        for (int i = 0; i < names.size(); i++) {
            String typeName = i < types.size() ? types.get(i).getText() : null;
            variables.add(new VariableDeclaration(names.get(i).getText(), typeName));
        }
        return variables;
    }

    private static IteratorExp.IteratorKind mapIteratorKind(String name) {
        return switch (name) {
            case "exists" -> IteratorExp.IteratorKind.EXISTS;
            case "forAll" -> IteratorExp.IteratorKind.FOR_ALL;
            case "collect" -> IteratorExp.IteratorKind.COLLECT;
            case "select" -> IteratorExp.IteratorKind.SELECT;
            case "reject" -> IteratorExp.IteratorKind.REJECT;
            case "any" -> IteratorExp.IteratorKind.ANY;
            case "one" -> IteratorExp.IteratorKind.ONE;
            case "isUnique" -> IteratorExp.IteratorKind.IS_UNIQUE;
            case "sortedBy" -> IteratorExp.IteratorKind.SORTED_BY;
            case "closure" -> IteratorExp.IteratorKind.CLOSURE;
            default -> IteratorExp.IteratorKind.UNKNOWN;
        };
    }

    private static String suffixText(Expression source, GOALParser.PathSuffixContext suffix) {
        return source.getText() + suffix.getText();
    }

    private static String binaryText(Expression left, String operator, Expression right) {
        return left.getText() + operator + right.getText();
    }
}
