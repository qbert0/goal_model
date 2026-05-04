package org.vnu.sme.goal.parser;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tzi.use.parser.ocl.OCLCompiler;
import org.tzi.use.uml.mm.MClass;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.ocl.expr.Evaluator;
import org.tzi.use.uml.ocl.expr.Expression;
import org.tzi.use.uml.ocl.value.BooleanValue;
import org.tzi.use.uml.ocl.value.UndefinedValue;
import org.tzi.use.uml.ocl.value.Value;
import org.tzi.use.uml.ocl.value.VarBindings;
import org.tzi.use.uml.sys.MSystemState;

public final class GoalOclService {
    private static final Pattern SELF_ALL_INSTANCES_PATTERN =
            Pattern.compile("\\bself\\.all[Ii]nstances\\b");
    private static final Pattern SELF_PATTERN =
            Pattern.compile("\\bself\\b");

    public enum EvalKind {
        TRUE,
        FALSE,
        UNDEFINED,
        ERROR
    }

    public record EvalResult(EvalKind kind, String normalizedExpression, String detail) {
    }

    public record CompilationResult(boolean ok, String normalizedExpression, String detail) {
    }

    private final MModel model;
    private final MSystemState systemState;
    private final VarBindings varBindings;

    public GoalOclService(MModel model, MSystemState systemState, VarBindings varBindings) {
        this.model = model;
        this.systemState = systemState;
        this.varBindings = varBindings;
    }

    public CompilationResult validateExpression(String expression, String label) {
        if (expression == null || expression.isBlank()) {
            return new CompilationResult(false, expression, "Missing OCL expression.");
        }

        String normalized = normalize(expression);
        StringWriter buffer = new StringWriter();
        PrintWriter err = new PrintWriter(buffer);
        Expression compiled = OCLCompiler.compileExpression(model, systemState, normalized, label, err, varBindings);
        err.flush();

        String detail = buffer.toString().trim();
        if (compiled == null) {
            if (detail.isBlank()) {
                detail = "USE OCL compiler rejected the expression.";
            }
            return new CompilationResult(false, normalized, detail);
        }
        return new CompilationResult(true, normalized, detail);
    }

    public EvalResult evaluateBooleanExpression(String expression, String label) {
        CompilationResult compilation = validateExpression(expression, label);
        if (!compilation.ok()) {
            return new EvalResult(EvalKind.ERROR, compilation.normalizedExpression(), compilation.detail());
        }

        StringWriter buffer = new StringWriter();
        PrintWriter err = new PrintWriter(buffer);
        Expression compiled = OCLCompiler.compileExpression(
                model,
                systemState,
                compilation.normalizedExpression(),
                label,
                err,
                varBindings);
        err.flush();

        if (compiled == null) {
            String detail = buffer.toString().trim();
            if (detail.isBlank()) {
                detail = "USE OCL compiler rejected the expression.";
            }
            return new EvalResult(EvalKind.ERROR, compilation.normalizedExpression(), detail);
        }

        try {
            Evaluator evaluator = new Evaluator();
            Value value = evaluator.eval(compiled, systemState, varBindings);
            if (value == null) {
                return new EvalResult(EvalKind.ERROR, compilation.normalizedExpression(),
                        "Evaluation returned no result.");
            }
            if (value == UndefinedValue.instance || value.isUndefined()) {
                return new EvalResult(EvalKind.UNDEFINED, compilation.normalizedExpression(), value.toStringWithType());
            }
            if (value instanceof BooleanValue bool) {
                return new EvalResult(bool.isTrue() ? EvalKind.TRUE : EvalKind.FALSE,
                        compilation.normalizedExpression(),
                        value.toStringWithType());
            }
            return new EvalResult(EvalKind.ERROR, compilation.normalizedExpression(),
                    "Expression did not evaluate to Boolean but to " + value.toStringWithType());
        } catch (RuntimeException ex) {
            return new EvalResult(EvalKind.ERROR, compilation.normalizedExpression(), ex.getMessage());
        }
    }

    private String normalize(String expression) {
        String trimmed = expression.trim();
        String rootInstanceExpression = resolveRootInstanceExpression();
        String rootCollectionExpression = resolveRootCollectionExpression();
        if (rootInstanceExpression == null || rootCollectionExpression == null) {
            return trimmed;
        }

        String normalized = SELF_ALL_INSTANCES_PATTERN
                .matcher(trimmed)
                .replaceAll(Matcher.quoteReplacement(rootCollectionExpression));
        return SELF_PATTERN
                .matcher(normalized)
                .replaceAll(Matcher.quoteReplacement(rootInstanceExpression));
    }

    private String resolveRootCollectionExpression() {
        if (model == null) {
            return null;
        }

        MClass rootClass = model.getClass("SystemState");
        if (rootClass == null) {
            return null;
        }

        return rootClass.name() + ".allInstances";
    }

    private String resolveRootInstanceExpression() {
        String rootCollectionExpression = resolveRootCollectionExpression();
        if (rootCollectionExpression == null) {
            return null;
        }
        return rootCollectionExpression + "->any(true)";
    }
}
