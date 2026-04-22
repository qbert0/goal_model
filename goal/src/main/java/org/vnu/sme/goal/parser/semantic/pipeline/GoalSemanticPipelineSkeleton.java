package org.vnu.sme.goal.parser.semantic.pipeline;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.tzi.use.uml.mm.MModel;
import org.vnu.sme.goal.ast.GoalModelCS;
import org.vnu.sme.goal.parser.semantic.symbols.DependencySymbol;
import org.vnu.sme.goal.parser.semantic.symbols.GoalSymbolTableBuilder;
import org.vnu.sme.goal.parser.semantic.symbols.GoalSymbolTable;
import org.vnu.sme.goal.parser.semantic.symbols.SemanticIssue;

/**
 * High-level semantic pipeline skeleton.
 * <p>
 * This class provides an incremental semantic pipeline structure.
 */
public final class GoalSemanticPipelineSkeleton {
    private static final String DUMP_FLAG = "goal.dump.semantic.steps";
    private GoalSymbolTableBuilder builder;

    public List<SemanticIssue> run(GoalModelCS ast, MModel model, PrintWriter err) {
        log(err, "=== GOAL Semantic Pipeline (Skeleton) ===");
        builder = new GoalSymbolTableBuilder();
        GoalSymbolTable table = createEmptySymbolTable(ast);
        declarationPass(ast, table, err);
        resolutionPass(ast, table, err);
        computeDerivedFlags(table, err);
        List<SemanticIssue> issues = validateSemanticRules(table, model, err);
        printSemanticIssues(issues, err);
        return issues;
    }

    /**
     * Step 0 - create symbol table root.
     */
    private GoalSymbolTable createEmptySymbolTable(GoalModelCS ast) {
        // Model-level root scope.
        return new GoalSymbolTable(ast.getfName().getText());
    }

    /**
     * Pass 1 - declaration traversal over AST.
     * - register actors (actor/agent/role)
     * - register elements per actor (goal/task/quality/resource)
     * - register dependency declarations with raw refs
     * - collect outgoing relation raw targets
     */
    private void declarationPass(GoalModelCS ast, GoalSymbolTable table, PrintWriter err) {
        log(err, "[PASS1] declarationPass: traverse AST and collect declarations");
        builder.runDeclarationPass(ast, table);
    }

    /**
     * Pass 2 - reference resolution.
     * - resolve relation targets (local scope first, then qualified refs)
     * - resolve dependency depender/dependee
     * - attach resolved symbols
     */
    private void resolutionPass(GoalModelCS ast, GoalSymbolTable table, PrintWriter err) {
        log(err, "[PASS2] resolutionPass: resolve refs against symbol tables");
        builder.runResolutionPass(ast, table);
    }

    /**
     * Compute derived semantic flags.
     * - e.g., isLeaf from refinement incoming edges
     */
    private void computeDerivedFlags(GoalSymbolTable table, PrintWriter err) {
        log(err, "[INFO] computeDerivedFlags: done in runResolutionPass (leaf recompute)");
    }

    /**
     * Semantic validations after pass2.
     * Current implementation collects base builder issues and leaf dependency checks.
     */
    private List<SemanticIssue> validateSemanticRules(GoalSymbolTable table, MModel model, PrintWriter err) {
        log(err, "[INFO] validateSemanticRules: collect base issues + leaf dependency check");
        List<SemanticIssue> merged = new ArrayList<>(builder.getIssues());

        for (DependencySymbol dep : table.getDependenciesByName().values()) {
            if (dep.getDepender() != null && !dep.getDepender().isLeaf()) {
                merged.add(new SemanticIssue(
                        "S3",
                        "Dependency depender must be leaf: " + dep.getDepender().getQualifiedName(),
                        dep.getDeclarationToken().getLine(),
                        dep.getDeclarationToken().getCharPositionInLine()));
            }
            if (dep.getDependee() != null && !dep.getDependee().isLeaf()) {
                merged.add(new SemanticIssue(
                        "S3",
                        "Dependency dependee must be leaf: " + dep.getDependee().getQualifiedName(),
                        dep.getDeclarationToken().getLine(),
                        dep.getDeclarationToken().getCharPositionInLine()));
            }
        }
        return merged;
    }

    /**
     * Reporting utility for semantic issues.
     */
    private void printSemanticIssues(List<SemanticIssue> issues, PrintWriter err) {
        if (issues.isEmpty()) {
            log(err, "[SEM] issues: none");
            return;
        }
        for (SemanticIssue issue : issues) {
            log(err, "[SEM][" + issue.code() + "] " + issue.line() + ":" + issue.column() + " " + issue.message());
        }
    }

    private static void log(PrintWriter err, String message) {
        if (Boolean.getBoolean(DUMP_FLAG)) {
            err.println(message);
        }
    }
}

