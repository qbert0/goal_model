package org.vnu.sme.goal.parser.semantic.pipeline;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.tzi.use.uml.mm.MModel;
import org.vnu.sme.goal.ast.GoalModelCS;
import org.vnu.sme.goal.parser.debug.GoalSymbolTablePrinter;
import org.vnu.sme.goal.parser.semantic.GoalSemanticAnalyzer;
import org.vnu.sme.goal.parser.semantic.symbols.GoalSymbolTableBuilder;
import org.vnu.sme.goal.parser.semantic.symbols.GoalSymbolTable;
import org.vnu.sme.goal.parser.semantic.symbols.SemanticIssue;

/**
 * High-level semantic pipeline skeleton.
 * <p>
 * This class provides an incremental semantic pipeline structure.
 */

// TODO: Recursion tree traverse
public final class GoalSemanticPipelineSkeleton {
    private static final String DUMP_FLAG = "goal.dump.semantic.steps";
    private static final String DUMP_SYMBOLS_FLAG = "goal.dump.symbols";
    private GoalSymbolTableBuilder builder;
    private final GoalSemanticAnalyzer analyzer = new GoalSemanticAnalyzer();

    public List<SemanticIssue> run(GoalModelCS ast, MModel model, PrintWriter err) {
        log(err, "=== GOAL Semantic Pipeline (Skeleton) ===");
        builder = new GoalSymbolTableBuilder();
        GoalSymbolTable table = createEmptySymbolTable(ast);
        declarationPass(ast, table, err);
        resolutionPass(ast, table, err);
        computeDerivedFlags(table, err);
        List<SemanticIssue> issues = validateSemanticRules(ast, table, model, err);
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
        dumpSymbols(err, "GOAL Symbol Table After Pass1", table);
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
        dumpSymbols(err, "GOAL Symbol Table After Pass2", table);
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
    private List<SemanticIssue> validateSemanticRules(GoalModelCS ast, GoalSymbolTable table, MModel model, PrintWriter err) {
        log(err, "[INFO] validateSemanticRules: run semantic checks S1..S10");
        List<SemanticIssue> merged = new ArrayList<>(builder.getIssues());
        merged.addAll(analyzer.validateOperatorMatrix(table));              // S2
        merged.addAll(analyzer.validateActorRelationships(ast, table));     // S4 (+ S1 actor refs)
        merged.addAll(analyzer.validateDependencyOnLeaf(table));            // S3
        merged.addAll(analyzer.validateSelfReference(table));               // S6
        merged.addAll(analyzer.validateQualifySourceIsQuality(table));      // S7
        merged.addAll(analyzer.validateNeededBySourceIsResource(table));    // S8
        merged.addAll(analyzer.validateCircularRefinement(table));          // S9
        merged.addAll(analyzer.validateMixedRefinementType(table));         // S10
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
            err.println("[SEM][" + issue.code() + "] " + issue.line() + ":" + issue.column() + " " + issue.message());
        }
    }

    private static void log(PrintWriter err, String message) {
        if (Boolean.getBoolean(DUMP_FLAG)) {
            err.println(message);
        }
    }

    private static void dumpSymbols(PrintWriter err, String title, GoalSymbolTable table) {
        if (Boolean.getBoolean(DUMP_SYMBOLS_FLAG)) {
            err.println(GoalSymbolTablePrinter.dump(title, table));
        }
    }
}

