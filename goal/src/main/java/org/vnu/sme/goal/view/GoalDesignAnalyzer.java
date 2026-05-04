package org.vnu.sme.goal.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.vnu.sme.goal.mm.Actor;
import org.vnu.sme.goal.mm.Goal;
import org.vnu.sme.goal.mm.GoalModel;
import org.vnu.sme.goal.mm.GoalTaskElement;
import org.vnu.sme.goal.mm.IntentionalElement;
import org.vnu.sme.goal.mm.Refinement;
import org.vnu.sme.goal.mm.Task;
import org.vnu.sme.goal.parser.GoalOclService;

final class GoalDesignAnalyzer {
    enum Severity {
        ERROR,
        WARNING,
        INFO
    }

    record DesignIssue(Severity severity, String location, String message) {
    }

    private final GoalModel goalModel;
    private final GoalOclService oclService;

    GoalDesignAnalyzer(GoalModel goalModel, GoalOclService oclService) {
        this.goalModel = goalModel;
        this.oclService = oclService;
    }

    List<DesignIssue> analyze() {
        List<DesignIssue> issues = new ArrayList<>();
        for (Actor actor : goalModel.getActors()) {
            for (IntentionalElement element : actor.getElements()) {
                if (element instanceof Goal goal) {
                    analyzeGoal(actor, goal, issues);
                } else if (element instanceof Task task) {
                    analyzeTask(actor, task, issues);
                }
            }
        }

        issues.sort(Comparator.comparing(DesignIssue::severity)
                .thenComparing(DesignIssue::location));
        return issues;
    }

    private void analyzeGoal(Actor actor, Goal goal, List<DesignIssue> issues) {
        String location = actor.getName() + "." + goal.getName();
        if (goal.getGoalClause() == null) {
            issues.add(new DesignIssue(Severity.WARNING, location,
                    "Goal has no formal clause (achieve/maintain/avoid)."));
        } else {
            GoalOclService.CompilationResult syntax =
                    oclService.validateExpression(goal.getOclExpression(), "goal-design:" + location);
            if (!syntax.ok()) {
                issues.add(new DesignIssue(Severity.ERROR, location,
                        "Goal OCL is not valid for the current USE context."));
            }
        }

        boolean hasOperationalization = hasTaskDescendant(goal, new HashSet<>());
        if (!hasOperationalization) {
            issues.add(new DesignIssue(Severity.WARNING, location,
                    "Goal has no task operationalization path. It is specified, but not operationalized."));
        }

        if (goal.getParentRefinements().isEmpty()) {
            issues.add(new DesignIssue(Severity.INFO, location,
                    "Leaf goal: satisfaction depends directly on its OCL clause and current state."));
        }
    }

    private void analyzeTask(Actor actor, Task task, List<DesignIssue> issues) {
        String location = actor.getName() + "." + task.getName();
        if (task.getPreExpression() == null || task.getPreExpression().isBlank()) {
            issues.add(new DesignIssue(Severity.WARNING, location,
                    "Task has no pre-condition."));
        }
        if (task.getPostExpression() == null || task.getPostExpression().isBlank()) {
            issues.add(new DesignIssue(Severity.ERROR, location,
                    "Task has no post-condition, so it cannot prove progress toward a goal."));
        }

        if (task.getPreExpression() != null && !task.getPreExpression().isBlank()) {
            GoalOclService.CompilationResult pre =
                    oclService.validateExpression(task.getPreExpression(), "task-pre-design:" + location);
            if (!pre.ok()) {
                issues.add(new DesignIssue(Severity.ERROR, location,
                        "Task pre-condition is not valid for the current USE context."));
            }
        }

        if (task.getPostExpression() != null && !task.getPostExpression().isBlank()) {
            GoalOclService.CompilationResult post =
                    oclService.validateExpression(task.getPostExpression(), "task-post-design:" + location);
            if (!post.ok()) {
                issues.add(new DesignIssue(Severity.ERROR, location,
                        "Task post-condition is not valid for the current USE context."));
            }
        }
    }

    private boolean hasTaskDescendant(GoalTaskElement element, Set<GoalTaskElement> visited) {
        if (!visited.add(element)) {
            return false;
        }

        if (element instanceof Task) {
            return true;
        }

        for (Refinement refinement : element.getParentRefinements()) {
            for (GoalTaskElement child : refinement.getChildren()) {
                if (child instanceof Task) {
                    return true;
                }
                if (hasTaskDescendant(child, visited)) {
                    return true;
                }
            }
        }
        return false;
    }
}
