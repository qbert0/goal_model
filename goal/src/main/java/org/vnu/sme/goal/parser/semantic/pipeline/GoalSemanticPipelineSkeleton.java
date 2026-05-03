package org.vnu.sme.goal.parser.semantic.pipeline;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.tzi.use.uml.mm.MModel;
import org.vnu.sme.goal.ast.GoalModelCS;
import org.vnu.sme.goal.parser.debug.GoalSymbolTablePrinter;
import org.vnu.sme.goal.parser.semantic.GoalSemanticAnalyzer;
import org.vnu.sme.goal.parser.semantic.OclSemanticAnalyzer;
import org.vnu.sme.goal.parser.semantic.symbols.GoalSymbolTableBuilder;
import org.vnu.sme.goal.parser.semantic.symbols.GoalSymbolTable;
import org.vnu.sme.goal.parser.semantic.symbols.SemanticIssue;

/**
 * High-level semantic pipeline skeleton.
 * <p>The pipeline is a fixed-shape sequence of passes; each pass delegates to
 * a single component (builder or analyzer) so that adding a new error group
 * means adding a new traversal call, not editing the orchestration logic.</p>
 *
 * <p>Pipeline (top-down):</p>
 * <ol>
 *   <li>Pass 1 — declaration: scan the AST and populate {@link GoalSymbolTable}
 *       with actors, elements, dependencies and OCL contracts.</li>
 *   <li>Pass 2 — resolution: bind raw references to symbols and recompute
 *       derived flags (e.g., leaf).</li>
 *   <li>Validation — run S1–S10 (v1) via {@link GoalSemanticAnalyzer}, then
 *       E1–E7 (v2 OCL extension) via {@link OclSemanticAnalyzer}.</li>
 * </ol>
 */
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
     * - lift OCL contracts (goalContract / taskContract) onto element symbols
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
     * Run all semantic checks: S1–S10 (v1, no OCL) followed by E1–E7 (v2 OCL).
     * Each {@code traverseXxx()} call returns its own issue list which is
     * merged into the final result; they are intentionally independent so a
     * later check does not depend on an earlier one having run.
     */
    private List<SemanticIssue> validateSemanticRules(GoalModelCS ast, GoalSymbolTable table, MModel model, PrintWriter err) {
        log(err, "[INFO] validateSemanticRules: run semantic checks S1..S10, then E1..E7");
        List<SemanticIssue> merged = new ArrayList<>(builder.getIssues());

        // ---- v1: GOAL core (no OCL) ----
        merged.addAll(analyzer.traverseActorReferenceTree(ast, table));      // S1, S4
        GoalSemanticAnalyzer.RelationTraversalContext relationContext =
                analyzer.traverseElementRelationTree(table);                  // S2, S6, S7, S8 (+ collect S9, S10)
        merged.addAll(relationContext.getIssues());
        merged.addAll(analyzer.traverseRefinementTargetMap(relationContext)); // S9, S10
        merged.addAll(analyzer.traverseDependencyTree(table));               // S3

        // ---- v2: OCL extension (sections 2..6) ----
        OclSemanticAnalyzer ocl = new OclSemanticAnalyzer(model);
        merged.addAll(ocl.traverseGoalContractCardinality(table));           // E5
        merged.addAll(ocl.traverseAchieveContracts(table));                  // E1, E2
        merged.addAll(ocl.traverseContractBodies(table));                    // E4
        merged.addAll(ocl.traverseOclExpressions(table));                    // E3, E6, E7

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
