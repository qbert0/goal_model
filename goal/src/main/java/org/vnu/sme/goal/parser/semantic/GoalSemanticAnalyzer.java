package org.vnu.sme.goal.parser.semantic;

import java.util.ArrayList;
import java.util.List;
import org.vnu.sme.goal.ast.GoalModelCS;
import org.vnu.sme.goal.parser.semantic.symbols.GoalSymbolTable;
import org.vnu.sme.goal.parser.semantic.symbols.GoalSymbolTableBuilder;
import org.vnu.sme.goal.parser.semantic.symbols.SemanticIssue;

/**
 * Skeleton semantic analyzer for GOAL core.
 */
public final class GoalSemanticAnalyzer {
    public List<SemanticIssue> analyze(GoalModelCS ast) {
        GoalSymbolTableBuilder builder = new GoalSymbolTableBuilder();
        GoalSymbolTable table = builder.build(ast);

        List<SemanticIssue> issues = new ArrayList<>(builder.getIssues());
        issues.addAll(validateOperatorMatrix(table));
        issues.addAll(validateActorRelationships(table));
        issues.addAll(validateDependencyOnLeaf(table));
        issues.addAll(validateSelfReference(table));
        return issues;
    }

    public List<SemanticIssue> validateOperatorMatrix(GoalSymbolTable table) {
        // TODO: validate (sourceKind, targetKind, operator) tuples
        return List.of();
    }

    public List<SemanticIssue> validateActorRelationships(GoalSymbolTable table) {
        // TODO: validate ':' and '>' constraints for actor kinds
        return List.of();
    }

    public List<SemanticIssue> validateDependencyOnLeaf(GoalSymbolTable table) {
        // TODO: enforce depender/dependee must be leaf elements
        return List.of();
    }

    public List<SemanticIssue> validateSelfReference(GoalSymbolTable table) {
        // TODO: detect source == target relation
        return List.of();
    }
}

