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
        return buildImplies(ctx.impliesExpr());
    }

    private static Expression buildImplies(GOALParser.ImpliesExprContext ctx) {
        Expression current = buildOr(ctx.orExpr(0));
        for (int i = 1; i < ctx.orExpr().size(); i++) {
            current = new BinaryExp(sliceText(ctx, current, ctx.orExpr(i).getText()), BinaryExp.Operator.IMPLIES,
                    current, buildOr(ctx.orExpr(i)));
        }
        return current;
    }

    private static Expression buildOr(GOALParser.OrExprContext ctx) {
        Expression current = buildAnd(ctx.andExpr(0));
        for (int i = 1; i < ctx.andExpr().size(); i++) {
            current = new BinaryExp(sliceText(ctx, current, ctx.andExpr(i).getText()), BinaryExp.Operator.OR,
                    current, buildAnd(ctx.andExpr(i)));
        }
        return current;
    }

    private static Expression buildAnd(GOALParser.AndExprContext ctx) {
        Expression current = buildEquality(ctx.equalityExpr(0));
        for (int i = 1; i < ctx.equalityExpr().size(); i++) {
            current = new BinaryExp(sliceText(ctx, current, ctx.equalityExpr(i).getText()), BinaryExp.Operator.AND,
                    current, buildEquality(ctx.equalityExpr(i)));
        }
        return current;
    }

    private static Expression buildEquality(GOALParser.EqualityExprContext ctx) {
        Expression current = buildRelational(ctx.relationalExpr(0));
        for (int i = 1; i < ctx.relationalExpr().size(); i++) {
            Token operator = (Token) ctx.getChild(2 * i - 1).getPayload();
            BinaryExp.Operator mapped = "=".equals(operator.getText()) ? BinaryExp.Operator.EQUALS : BinaryExp.Operator.NOT_EQUALS;
            current = new BinaryExp(sliceText(ctx, current, ctx.relationalExpr(i).getText()), mapped,
                    current, buildRelational(ctx.relationalExpr(i)));
        }
        return current;
    }

    private static Expression buildRelational(GOALParser.RelationalExprContext ctx) {
        Expression current = buildAdditive(ctx.additiveExpr(0));
        for (int i = 1; i < ctx.additiveExpr().size(); i++) {
            Token operator = (Token) ctx.getChild(2 * i - 1).getPayload();
            current = new BinaryExp(sliceText(ctx, current, ctx.additiveExpr(i).getText()), mapRelational(operator),
                    current, buildAdditive(ctx.additiveExpr(i)));
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

    private static Expression buildAdditive(GOALParser.AdditiveExprContext ctx) {
        Expression current = buildMultiplicative(ctx.multiplicativeExpr(0));
        for (int i = 1; i < ctx.multiplicativeExpr().size(); i++) {
            Token operator = (Token) ctx.getChild(2 * i - 1).getPayload();
            BinaryExp.Operator mapped = "+".equals(operator.getText()) ? BinaryExp.Operator.ADD : BinaryExp.Operator.SUBTRACT;
            current = new BinaryExp(sliceText(ctx, current, ctx.multiplicativeExpr(i).getText()), mapped,
                    current, buildMultiplicative(ctx.multiplicativeExpr(i)));
        }
        return current;
    }

    private static Expression buildMultiplicative(GOALParser.MultiplicativeExprContext ctx) {
        Expression current = buildUnary(ctx.unaryExpr(0));
        for (int i = 1; i < ctx.unaryExpr().size(); i++) {
            Token operator = (Token) ctx.getChild(2 * i - 1).getPayload();
            BinaryExp.Operator mapped = "*".equals(operator.getText()) ? BinaryExp.Operator.MULTIPLY : BinaryExp.Operator.DIVIDE;
            current = new BinaryExp(sliceText(ctx, current, ctx.unaryExpr(i).getText()), mapped,
                    current, buildUnary(ctx.unaryExpr(i)));
        }
        return current;
    }

    private static Expression buildUnary(GOALParser.UnaryExprContext ctx) {
        if (ctx.NOT() != null) {
            return new UnaryExp(ctx.getText(), UnaryExp.Operator.NOT, buildUnary(ctx.unaryExpr()));
        }
        if (ctx.MINUS() != null) {
            return new UnaryExp(ctx.getText(), UnaryExp.Operator.NEGATE, buildUnary(ctx.unaryExpr()));
        }
        return buildPrimary(ctx.primaryExpr());
    }

    private static Expression buildPrimary(GOALParser.PrimaryExprContext ctx) {
        if (ctx.literal() != null) {
            return buildLiteral(ctx.literal());
        }
        if (ctx.SELF() != null) {
            return new SelfExp(ctx.getText());
        }
        if (ctx.expression() != null) {
            return build(ctx.expression());
        }
        return buildPath(ctx.pathExpr());
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
        return new EnumLiteralExp(enumLiteral.getText(), enumLiteral.IDENT(0).getText(), enumLiteral.IDENT(1).getText());
    }

    private static Expression buildPath(GOALParser.PathExprContext ctx) {
        Expression current = buildPrimaryAtom(ctx.primaryAtom());
        for (GOALParser.PathSuffixContext suffix : ctx.pathSuffix()) {
            current = applySuffix(current, suffix);
        }
        return current;
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
            return new IteratorExp(text, source, mapIteratorKind(iterator.IDENT().getText()), iterator.IDENT().getText(),
                    buildIteratorVars(iterator.iteratorVars()), build(iterator.expression()));
        }
        return buildSimpleCall(source, ctx.simpleCall(), text, false);
    }

    private static OperationCallExp buildSimpleCall(Expression source, GOALParser.SimpleCallContext ctx, String text, boolean atPre) {
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

    private static String sliceText(org.antlr.v4.runtime.ParserRuleContext parent, Expression left, String rightText) {
        return left.getText() + parent.getText().substring(left.getText().length(), parent.getText().length() - rightText.length()) + rightText;
    }
}
