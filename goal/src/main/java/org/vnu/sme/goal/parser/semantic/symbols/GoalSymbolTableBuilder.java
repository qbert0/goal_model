package org.vnu.sme.goal.parser.semantic.symbols;

import java.util.ArrayList;
import java.util.List;
import org.vnu.sme.goal.ast.GoalModelCS;

/**
 * Skeleton for Symbol Table construction in two passes.
 * <p>
 * Pass 1: collect declarations and create symbols.
 * Pass 2: resolve cross references.
 */
public final class GoalSymbolTableBuilder {
    private final List<SemanticIssue> issues = new ArrayList<>();

    public GoalSymbolTable build(GoalModelCS ast) {
        GoalSymbolTable table = new GoalSymbolTable(ast.getfName().getText());
        runDeclarationPass(ast, table);
        runResolutionPass(ast, table);
        return table;
    }

    public List<SemanticIssue> getIssues() {
        return issues;
    }

    public void runDeclarationPass(GoalModelCS ast, GoalSymbolTable table) {
        // TODO: implement Pass 1
        // - register actor symbols
        // - register element symbols in each actor scope
        // - register dependency symbols with raw refs
        // - collect relation entries
        throw new UnsupportedOperationException("TODO: implement declaration pass");
    }

    public void runResolutionPass(GoalModelCS ast, GoalSymbolTable table) {
        // TODO: implement Pass 2
        // - resolve relation targets (local and qualified refs)
        // - resolve dependency refs (depender/dependee)
        // - compute/update leaf flags for refinement targets
        throw new UnsupportedOperationException("TODO: implement resolution pass");
    }

    public void reportDuplicateDeclaration(String name, int line, int column) {
        // TODO: map to your diagnostic strategy
        issues.add(new SemanticIssue("S5", "Duplicate declaration: " + name, line, column));
    }

    public void reportUndeclaredReference(String ref, int line, int column) {
        // TODO: map to your diagnostic strategy
        issues.add(new SemanticIssue("S1", "Undeclared reference: " + ref, line, column));
    }
}

