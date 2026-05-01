package org.vnu.sme.goal.parser;

import org.vnu.sme.goal.ast.GoalModelCS;

/**
 * Compatibility wrapper for the semantic/debug pipeline.
 * The canonical AST construction lives in {@link GoalAstBuilder}.
 */
public final class GoalModelBuildingVisitor {
    private GoalModelBuildingVisitor() {
    }

    public static GoalModelCS build(GOALParser.GoalModelContext ctx) {
        return (GoalModelCS) new GoalAstBuilder().visitGoalModel(ctx);
    }
}
