package org.vnu.sme.goal.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.vnu.sme.goal.ast.ocl.OclAggregateCallExpCS;
import org.vnu.sme.goal.ast.ocl.OclAtPreExpCS;
import org.vnu.sme.goal.ast.ocl.OclBinaryExpCS;
import org.vnu.sme.goal.ast.ocl.OclBooleanLiteralExpCS;
import org.vnu.sme.goal.ast.ocl.OclEnumLiteralExpCS;
import org.vnu.sme.goal.ast.ocl.OclExpressionCS;
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

/**
 * Translate OCL parse-tree nodes (defined by GOAL.g4 sections 4–6) into the
 * syntax-level OCL AST under {@code org.vnu.sme.goal.ast.ocl}.
 *
 * <p>The mapping follows grammar rules verbatim:
 * <ul>
 *   <li>{@code impliesExpr / orExpr / andExpr / notExpr / equalityExpr / relationalExpr /
 *       additiveExpr / multiplicativeExpr / unaryExpr} → nested AST binary/unary nodes
 *       chains.</li>
 *   <li>{@code pathExpr} → chain of AST property/operation/iterator/aggregate nodes.</li>
 *   <li>{@code iteratorCall} (ARROW after a collection) → iterator AST node.</li>
 *   <li>{@code aggregateCall} (ARROW after a collection) → aggregate AST node;
 *       this distinguishes scalar/boolean returning ops from chainable {@code collect}.</li>
 * </ul>
 */
final class OclExpressionBuilder {
    private OclExpressionBuilder() {
    }

    static OclExpressionCS build(GOALParser.ExpressionContext ctx) {
        return buildConditionalImplies(ctx.conditionalImpliesExpression());
    }

    private static OclExpressionCS buildConditionalImplies(GOALParser.ConditionalImpliesExpressionContext ctx) {
        OclExpressionCS current = buildConditionalOr(ctx.conditionalOrExpression(0));
        for (int i = 1; i < ctx.conditionalOrExpression().size(); i++) {
            OclExpressionCS right = buildConditionalOr(ctx.conditionalOrExpression(i));
            current = new OclBinaryExpCS(binaryText(current, "implies", right), OclBinaryExpCS.Operator.IMPLIES,
                    current, right);
        }
        return current;
    }

    private static OclExpressionCS buildConditionalOr(GOALParser.ConditionalOrExpressionContext ctx) {
        OclExpressionCS current = buildConditionalAnd(ctx.conditionalAndExpression(0));
        for (int i = 1; i < ctx.conditionalAndExpression().size(); i++) {
            OclExpressionCS right = buildConditionalAnd(ctx.conditionalAndExpression(i));
            current = new OclBinaryExpCS(binaryText(current, "or", right), OclBinaryExpCS.Operator.OR,
                    current, right);
        }
        return current;
    }

    private static OclExpressionCS buildConditionalAnd(GOALParser.ConditionalAndExpressionContext ctx) {
        OclExpressionCS current = buildEquality(ctx.equalityExpression(0));
        for (int i = 1; i < ctx.equalityExpression().size(); i++) {
            OclExpressionCS right = buildEquality(ctx.equalityExpression(i));
            current = new OclBinaryExpCS(binaryText(current, "and", right), OclBinaryExpCS.Operator.AND,
                    current, right);
        }
        return current;
    }

    private static OclExpressionCS buildEquality(GOALParser.EqualityExpressionContext ctx) {
        OclExpressionCS current = buildRelational(ctx.relationalExpression(0));
        for (int i = 1; i < ctx.relationalExpression().size(); i++) {
            Token operator = (Token) ctx.getChild(2 * i - 1).getPayload();
            OclBinaryExpCS.Operator mapped = "=".equals(operator.getText())
                    ? OclBinaryExpCS.Operator.EQUALS
                    : OclBinaryExpCS.Operator.NOT_EQUALS;
            OclExpressionCS right = buildRelational(ctx.relationalExpression(i));
            current = new OclBinaryExpCS(binaryText(current, operator.getText(), right), mapped,
                    current, right);
        }
        return current;
    }

    private static OclExpressionCS buildRelational(GOALParser.RelationalExpressionContext ctx) {
        OclExpressionCS current = buildAdditive(ctx.additiveExpression(0));
        for (int i = 1; i < ctx.additiveExpression().size(); i++) {
            Token operator = (Token) ctx.getChild(2 * i - 1).getPayload();
            OclExpressionCS right = buildAdditive(ctx.additiveExpression(i));
            current = new OclBinaryExpCS(binaryText(current, operator.getText(), right), mapRelational(operator),
                    current, right);
        }
        return current;
    }

    private static OclBinaryExpCS.Operator mapRelational(Token operator) {
        return switch (operator.getText()) {
            case "<" -> OclBinaryExpCS.Operator.LT;
            case "<=" -> OclBinaryExpCS.Operator.LE;
            case ">" -> OclBinaryExpCS.Operator.GT;
            default -> OclBinaryExpCS.Operator.GE;
        };
    }

    private static OclExpressionCS buildAdditive(GOALParser.AdditiveExpressionContext ctx) {
        OclExpressionCS current = buildMultiplicative(ctx.multiplicativeExpression(0));
        for (int i = 1; i < ctx.multiplicativeExpression().size(); i++) {
            Token operator = (Token) ctx.getChild(2 * i - 1).getPayload();
            OclBinaryExpCS.Operator mapped = "+".equals(operator.getText())
                    ? OclBinaryExpCS.Operator.ADD
                    : OclBinaryExpCS.Operator.SUBTRACT;
            OclExpressionCS right = buildMultiplicative(ctx.multiplicativeExpression(i));
            current = new OclBinaryExpCS(binaryText(current, operator.getText(), right), mapped,
                    current, right);
        }
        return current;
    }

    private static OclExpressionCS buildMultiplicative(GOALParser.MultiplicativeExpressionContext ctx) {
        OclExpressionCS current = buildUnary(ctx.unaryExpression(0));
        for (int i = 1; i < ctx.unaryExpression().size(); i++) {
            Token operator = (Token) ctx.getChild(2 * i - 1).getPayload();
            OclBinaryExpCS.Operator mapped = "*".equals(operator.getText())
                    ? OclBinaryExpCS.Operator.MULTIPLY
                    : OclBinaryExpCS.Operator.DIVIDE;
            OclExpressionCS right = buildUnary(ctx.unaryExpression(i));
            current = new OclBinaryExpCS(binaryText(current, operator.getText(), right), mapped,
                    current, right);
        }
        return current;
    }

    private static OclExpressionCS buildUnary(GOALParser.UnaryExpressionContext ctx) {
        if (ctx.NOT() != null) {
            OclExpressionCS operand = buildUnary(ctx.unaryExpression());
            return new OclUnaryExpCS("not " + operand.getText(), OclUnaryExpCS.Operator.NOT, operand);
        }
        if (ctx.MINUS() != null) {
            OclExpressionCS operand = buildUnary(ctx.unaryExpression());
            return new OclUnaryExpCS("-" + operand.getText(), OclUnaryExpCS.Operator.NEGATE, operand);
        }
        if (ctx.PLUS() != null) {
            return buildUnary(ctx.unaryExpression());
        }
        return buildPostfix(ctx.postfixExpression());
    }

    private static OclExpressionCS buildPostfix(GOALParser.PostfixExpressionContext ctx) {
        OclExpressionCS current = buildPrimary(ctx.primaryExpression());
        for (GOALParser.PathSuffixContext suffix : ctx.pathSuffix()) {
            current = applySuffix(current, suffix);
        }
        return current;
    }

    private static OclExpressionCS buildPrimary(GOALParser.PrimaryExpressionContext ctx) {
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

    private static OclExpressionCS buildLiteral(GOALParser.LiteralContext ctx) {
        if (ctx.STRING() != null) {
            return new OclStringLiteralExpCS(ctx.getText(), ctx.getText().substring(1, ctx.getText().length() - 1));
        }
        if (ctx.INT() != null) {
            return new OclIntegerLiteralExpCS(ctx.getText(), Integer.parseInt(ctx.getText()));
        }
        if (ctx.REAL() != null) {
            return new OclRealLiteralExpCS(ctx.getText(), Double.parseDouble(ctx.getText()));
        }
        if (ctx.TRUE() != null) {
            return new OclBooleanLiteralExpCS(ctx.getText(), true);
        }
        if (ctx.FALSE() != null) {
            return new OclBooleanLiteralExpCS(ctx.getText(), false);
        }
        if (ctx.NULL() != null) {
            return new OclNullLiteralExpCS(ctx.getText());
        }
        GOALParser.EnumLiteralContext enumLiteral = ctx.enumLiteral();
        return new OclEnumLiteralExpCS(enumLiteral.getText(), enumLiteral.IDENT(0).getText(),
                enumLiteral.IDENT(1).getText());
    }

    private static OclExpressionCS buildPrimaryAtom(GOALParser.PrimaryAtomContext ctx) {
        if (ctx.SELF() != null) {
            return new OclSelfExpCS(ctx.getText());
        }
        return new OclVariableExpCS(ctx.getText(), ctx.IDENT().getText());
    }

    private static OclExpressionCS applySuffix(OclExpressionCS source, GOALParser.PathSuffixContext suffix) {
        // .ident@pre  or  .ident  → property access
        if (suffix.IDENT() != null) {
            boolean atPre = suffix.AT_PRE() != null;
            return new OclPropertyCallExpCS(propertySuffixText(source, suffix.IDENT().getText(), atPre),
                    source,
                    suffix.IDENT().getText(),
                    atPre);
        }
        // .name(args)
        if (suffix.simpleCall() != null) {
            return buildSimpleCall(source, suffix.simpleCall(), dottedCallText(source, suffix.simpleCall()), false);
        }
        // ->iteratorCall|aggregateCall|simpleCall
        if (suffix.collectionCall() != null) {
            return buildCollectionCall(source, suffix.collectionCall(), collectionCallText(source, suffix.collectionCall()));
        }
        return new OclAtPreExpCS(source.getText() + "@pre", source);
    }

    private static OclExpressionCS buildCollectionCall(OclExpressionCS source, GOALParser.CollectionCallContext ctx, String text) {
        if (ctx.iteratorCall() != null) {
            GOALParser.IteratorCallContext iterator = ctx.iteratorCall();
            String iteratorName = iterator.iteratorOp().getText();
            return new OclIteratorExpCS(text, source, mapIteratorKind(iteratorName), iteratorName,
                    buildIteratorVars(iterator.iteratorVars()), build(iterator.expression()));
        }
        if (ctx.aggregateCall() != null) {
            return buildAggregateCall(source, ctx.aggregateCall(), text);
        }
        return buildSimpleCall(source, ctx.simpleCall(), text, false);
    }

    private static OclAggregateCallExpCS buildAggregateCall(
            OclExpressionCS source, GOALParser.AggregateCallContext ctx, String text) {
        if (ctx.COUNT() != null) {
            return new OclAggregateCallExpCS(
                    text,
                    source,
                    OclAggregateCallExpCS.Kind.COUNT,
                    null,
                    buildIteratorVars(ctx.iteratorVars()),
                    build(ctx.expression()));
        }
        OclAggregateCallExpCS.Kind kind = mapAggregateKind(ctx.aggregateOp().getText());
        List<OclExpressionCS> args = new ArrayList<>();
        if (ctx.argumentList() != null) {
            for (GOALParser.ExpressionContext argument : ctx.argumentList().expression()) {
                args.add(build(argument));
            }
        }
        return new OclAggregateCallExpCS(text, source, kind, args, null, null);
    }

    private static OclOperationCallExpCS buildSimpleCall(OclExpressionCS source, GOALParser.SimpleCallContext ctx, String text,
            boolean atPre) {
        List<OclExpressionCS> arguments = new ArrayList<>();
        if (ctx.argumentList() != null) {
            for (GOALParser.ExpressionContext argument : ctx.argumentList().expression()) {
                arguments.add(build(argument));
            }
        }
        return new OclOperationCallExpCS(text, source, ctx.IDENT().getText(), atPre, arguments);
    }

    /**
     * Build {@link VariableDeclaration} list from {@code iteratorVars} parse tree.
     * <p>The grammar permits 1 or 2 names with optional types — see
     * {@code iteratorVars} rule in GOAL.g4.</p>
     */
    static List<OclVariableDeclarationCS> buildIteratorVars(GOALParser.IteratorVarsContext ctx) {
        List<OclVariableDeclarationCS> variables = new ArrayList<>();
        List<TerminalNode> names = ctx.IDENT();
        List<GOALParser.QualifiedNameContext> types = ctx.qualifiedName();
        for (int i = 0; i < names.size(); i++) {
            String typeName = i < types.size() ? types.get(i).getText() : null;
            variables.add(new OclVariableDeclarationCS(names.get(i).getText(), typeName));
        }
        return variables;
    }

    /**
     * Build typed variable list from {@code achieve for unique (...)} clause.
     */
    static List<OclVariableDeclarationCS> buildTypedVarList(GOALParser.TypedVarListContext ctx) {
        List<OclVariableDeclarationCS> variables = new ArrayList<>();
        for (GOALParser.TypedVarContext typedVar : ctx.typedVar()) {
            variables.add(new OclVariableDeclarationCS(typedVar.IDENT().getText(), typedVar.qualifiedName().getText()));
        }
        return variables;
    }

    private static OclIteratorExpCS.IteratorKind mapIteratorKind(String name) {
        return switch (name) {
            case "exists" -> OclIteratorExpCS.IteratorKind.EXISTS;
            case "exist" -> OclIteratorExpCS.IteratorKind.EXISTS;
            case "forAll" -> OclIteratorExpCS.IteratorKind.FOR_ALL;
            case "collect" -> OclIteratorExpCS.IteratorKind.COLLECT;
            case "select" -> OclIteratorExpCS.IteratorKind.SELECT;
            case "reject" -> OclIteratorExpCS.IteratorKind.REJECT;
            case "any" -> OclIteratorExpCS.IteratorKind.ANY;
            case "one" -> OclIteratorExpCS.IteratorKind.ONE;
            case "isUnique" -> OclIteratorExpCS.IteratorKind.IS_UNIQUE;
            case "sortedBy" -> OclIteratorExpCS.IteratorKind.SORTED_BY;
            case "closure" -> OclIteratorExpCS.IteratorKind.CLOSURE;
            default -> OclIteratorExpCS.IteratorKind.UNKNOWN;
        };
    }

    private static String propertySuffixText(OclExpressionCS source, String property, boolean atPre) {
        return source.getText() + "." + property + (atPre ? "@pre" : "");
    }

    private static OclAggregateCallExpCS.Kind mapAggregateKind(String name) {
        return switch (name) {
            case "size" -> OclAggregateCallExpCS.Kind.SIZE;
            case "sum" -> OclAggregateCallExpCS.Kind.SUM;
            case "max" -> OclAggregateCallExpCS.Kind.MAX;
            case "min" -> OclAggregateCallExpCS.Kind.MIN;
            case "isEmpty" -> OclAggregateCallExpCS.Kind.IS_EMPTY;
            case "notEmpty" -> OclAggregateCallExpCS.Kind.NOT_EMPTY;
            case "includes" -> OclAggregateCallExpCS.Kind.INCLUDES;
            case "excludes" -> OclAggregateCallExpCS.Kind.EXCLUDES;
            default -> throw new IllegalArgumentException("Unknown aggregate operator: " + name);
        };
    }

    private static String binaryText(OclExpressionCS left, String operator, OclExpressionCS right) {
        return left.getText() + " " + operator + " " + right.getText();
    }

    private static String dottedCallText(OclExpressionCS source, GOALParser.SimpleCallContext ctx) {
        return source.getText() + "." + simpleCallText(ctx);
    }

    private static String collectionCallText(OclExpressionCS source, GOALParser.CollectionCallContext ctx) {
        if (ctx.iteratorCall() != null) {
            return source.getText() + "->" + iteratorCallText(ctx.iteratorCall());
        }
        if (ctx.aggregateCall() != null) {
            return source.getText() + "->" + aggregateCallText(ctx.aggregateCall());
        }
        return source.getText() + "->" + simpleCallText(ctx.simpleCall());
    }

    private static String simpleCallText(GOALParser.SimpleCallContext ctx) {
        return ctx.IDENT().getText() + "(" + argumentListText(ctx.argumentList()) + ")";
    }

    private static String iteratorCallText(GOALParser.IteratorCallContext ctx) {
        return ctx.iteratorOp().getText()
                + "("
                + iteratorVarsText(ctx.iteratorVars())
                + " | "
                + build(ctx.expression()).getText()
                + ")";
    }

    private static String aggregateCallText(GOALParser.AggregateCallContext ctx) {
        if (ctx.aggregateOp() != null) {
            return ctx.aggregateOp().getText() + "(" + argumentListText(ctx.argumentList()) + ")";
        }
        return ctx.COUNT().getText()
                + "("
                + iteratorVarsText(ctx.iteratorVars())
                + " | "
                + build(ctx.expression()).getText()
                + ")";
    }

    private static String argumentListText(GOALParser.ArgumentListContext ctx) {
        if (ctx == null) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(", ");
        for (GOALParser.ExpressionContext expression : ctx.expression()) {
            joiner.add(build(expression).getText());
        }
        return joiner.toString();
    }

    private static String iteratorVarsText(GOALParser.IteratorVarsContext ctx) {
        List<TerminalNode> names = ctx.IDENT();
        List<GOALParser.QualifiedNameContext> types = ctx.qualifiedName();
        if (names.size() == 1) {
            if (types.isEmpty()) {
                return names.get(0).getText();
            }
            return names.get(0).getText() + " : " + types.get(0).getText();
        }

        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i).getText();
            if (i < types.size()) {
                joiner.add(name + " : " + types.get(i).getText());
            } else {
                joiner.add(name);
            }
        }
        return joiner.toString();
    }

}
