package org.vnu.sme.goal.parser;

import java.io.PrintWriter;

import org.vnu.sme.goal.mm.Goal;
import org.vnu.sme.goal.mm.GoalModel;
import org.vnu.sme.goal.mm.IntentionalElement;
import org.vnu.sme.goal.mm.Task;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.ocl.value.VarBindings;
import org.tzi.use.uml.sys.MSystemState;

final class GoalOclCompilationValidator {
    private GoalOclCompilationValidator() {
    }

    static int validate(GoalModel goalModel, MModel useModel, MSystemState systemState, VarBindings varBindings, PrintWriter err) {
        if (goalModel == null || useModel == null) {
            return 0;
        }

        GoalOclService oclService = new GoalOclService(useModel, systemState, varBindings);
        int problems = 0;

        for (IntentionalElement element : goalModel.getAllElements()) {
            problems += validateElement(oclService, element, err);
        }

        err.flush();
        return problems;
    }

    private static int validateElement(GoalOclService oclService, IntentionalElement element, PrintWriter err) {
        int problems = 0;

        if (element instanceof Goal goal) {
            problems += validateExpression(oclService, element, "goal", goal.getOclExpression(), err);
        } else if (element instanceof Task task) {
            problems += validateExpression(oclService, element, "pre", task.getPreExpression(), err);
            problems += validateExpression(oclService, element, "post", task.getPostExpression(), err);
        }

        return problems;
    }

    private static int validateExpression(GoalOclService oclService,
                                          IntentionalElement element,
                                          String clauseName,
                                          String expression,
                                          PrintWriter err) {
        if (expression == null || expression.isBlank()) {
            return 0;
        }

        GoalOclService.CompilationResult result =
                oclService.validateExpression(expression, element.getType() + ":" + element.getName() + ":" + clauseName);
        if (result.ok()) {
            return 0;
        }

        err.println("[GOAL][OCL] " + element.getType() + " '" + element.getName()
                + "' has invalid " + clauseName + " expression:");
        err.println("  - original: " + expression);
        if (result.normalizedExpression() != null && !result.normalizedExpression().equals(expression)) {
            err.println("  - normalized: " + result.normalizedExpression());
        }
        for (String line : result.detail().split("\\R")) {
            if (!line.isBlank()) {
                err.println("  - " + line);
            }
        }
        return 1;
    }
}
