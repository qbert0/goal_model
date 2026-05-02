package org.vnu.sme.goal.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vnu.sme.goal.mm.Actor;
import org.vnu.sme.goal.mm.Contribution;
import org.vnu.sme.goal.mm.ContributionType;
import org.vnu.sme.goal.mm.Goal;
import org.vnu.sme.goal.mm.GoalModel;
import org.vnu.sme.goal.mm.GoalTaskElement;
import org.vnu.sme.goal.mm.IntentionalElement;
import org.vnu.sme.goal.mm.Refinement;
import org.vnu.sme.goal.mm.Task;

final class GoalStatusAnalyzer {
    enum SyntaxStatus {
        OK,
        ERROR,
        MISSING
    }

    enum SupportStatus {
        SUPPORTED,
        PARTIAL,
        BLOCKED,
        UNKNOWN
    }

    record GoalStatusRow(
            String actorName,
            String goalName,
            String goalType,
            SyntaxStatus syntaxStatus,
            SupportStatus supportStatus,
            String evidence) {
    }

    private static final class AnalysisResult {
        private final SupportStatus status;
        private final String reason;

        private AnalysisResult(SupportStatus status, String reason) {
            this.status = status;
            this.reason = reason;
        }
    }

    private final GoalModel goalModel;
    private final Map<IntentionalElement, OclValidationReport> validationReports;
    private final Map<GoalTaskElement, AnalysisResult> cache = new HashMap<>();

    GoalStatusAnalyzer(GoalModel goalModel, Map<IntentionalElement, OclValidationReport> validationReports) {
        this.goalModel = goalModel;
        this.validationReports = validationReports;
    }

    List<GoalStatusRow> analyzeGoals() {
        List<GoalStatusRow> rows = new ArrayList<>();
        for (Actor actor : goalModel.getActors()) {
            for (IntentionalElement element : actor.getElements()) {
                if (element instanceof Goal goal) {
                    AnalysisResult result = analyze(goal);
                    rows.add(new GoalStatusRow(
                            actor.getName(),
                            goal.getName(),
                            goal.getGoalType() == null ? "unspecified" : goal.getGoalType().name().toLowerCase(),
                            syntaxStatus(goal),
                            result.status,
                            result.reason));
                }
            }
        }

        rows.sort(Comparator.comparing(GoalStatusRow::actorName)
                .thenComparing(GoalStatusRow::goalName));
        return rows;
    }

    private AnalysisResult analyze(GoalTaskElement element) {
        AnalysisResult cached = cache.get(element);
        if (cached != null) {
            return cached;
        }

        AnalysisResult base = baseStatus(element);
        AnalysisResult refinement = refinementStatus(element);
        SupportStatus combinedStatus = combine(base.status, refinement.status);

        StringBuilder reason = new StringBuilder(base.reason);
        if (!refinement.reason.isBlank()) {
            if (reason.length() > 0) {
                reason.append(" ");
            }
            reason.append(refinement.reason);
        }

        String contributionSummary = contributionSummary(element);
        if (!contributionSummary.isBlank()) {
            if (reason.length() > 0) {
                reason.append(" ");
            }
            reason.append(contributionSummary);
        }

        AnalysisResult result = new AnalysisResult(combinedStatus, reason.toString().trim());
        cache.put(element, result);
        return result;
    }

    private AnalysisResult baseStatus(GoalTaskElement element) {
        if (element instanceof Task task) {
            SyntaxStatus syntax = syntaxStatus(task);
            if (syntax == SyntaxStatus.ERROR) {
                return new AnalysisResult(SupportStatus.BLOCKED,
                        "Task OCL is invalid against the current USE model.");
            }
            if (task.getPostExpression() != null && !task.getPostExpression().isBlank()) {
                return new AnalysisResult(SupportStatus.SUPPORTED,
                        "Task has a post-condition, so it can operationalize its parent goal.");
            }
            if (task.getPreExpression() != null && !task.getPreExpression().isBlank()) {
                return new AnalysisResult(SupportStatus.PARTIAL,
                        "Task has only a pre-condition; effect on goal satisfaction is underspecified.");
            }
            return new AnalysisResult(SupportStatus.UNKNOWN,
                    "Task has no pre/post condition to infer operational effect.");
        }

        Goal goal = (Goal) element;
        SyntaxStatus syntax = syntaxStatus(goal);
        if (syntax == SyntaxStatus.ERROR) {
            return new AnalysisResult(SupportStatus.BLOCKED,
                    "Goal expression is invalid against the current USE model.");
        }
        if (syntax == SyntaxStatus.MISSING) {
            return new AnalysisResult(SupportStatus.UNKNOWN,
                    "Goal has no formal OCL clause.");
        }
        return new AnalysisResult(SupportStatus.UNKNOWN,
                "Goal clause is well-formed, but truth cannot be decided without state exploration.");
    }

    private AnalysisResult refinementStatus(GoalTaskElement element) {
        List<Refinement> refinements = element.getParentRefinements();
        if (refinements.isEmpty()) {
            return new AnalysisResult(SupportStatus.UNKNOWN, "");
        }

        SupportStatus best = SupportStatus.BLOCKED;
        List<String> reasons = new ArrayList<>();

        for (Refinement refinement : refinements) {
            List<SupportStatus> childStatuses = new ArrayList<>();
            for (GoalTaskElement child : refinement.getChildren()) {
                childStatuses.add(analyze(child).status);
            }

            SupportStatus aggregate = aggregateRefinement(refinement, childStatuses);
            if (rank(aggregate) > rank(best)) {
                best = aggregate;
            }
            reasons.add(refinementReason(refinement, childStatuses, aggregate));
        }

        return new AnalysisResult(best, String.join(" ", reasons));
    }

    private SupportStatus aggregateRefinement(Refinement refinement, List<SupportStatus> childStatuses) {
        if (childStatuses.isEmpty()) {
            return SupportStatus.UNKNOWN;
        }

        if (refinement.getRefinementType() == Refinement.RefinementType.AND) {
            boolean allSupported = childStatuses.stream().allMatch(status -> status == SupportStatus.SUPPORTED);
            if (allSupported) {
                return SupportStatus.SUPPORTED;
            }
            boolean anyBlocked = childStatuses.stream().anyMatch(status -> status == SupportStatus.BLOCKED);
            if (anyBlocked) {
                return SupportStatus.BLOCKED;
            }
            boolean anySupported = childStatuses.stream().anyMatch(status -> status == SupportStatus.SUPPORTED);
            boolean anyPartial = childStatuses.stream().anyMatch(status -> status == SupportStatus.PARTIAL);
            if (anySupported || anyPartial) {
                return SupportStatus.PARTIAL;
            }
            return SupportStatus.UNKNOWN;
        }

        boolean anySupported = childStatuses.stream().anyMatch(status -> status == SupportStatus.SUPPORTED);
        if (anySupported) {
            return SupportStatus.SUPPORTED;
        }
        boolean anyPartial = childStatuses.stream().anyMatch(status -> status == SupportStatus.PARTIAL);
        if (anyPartial) {
            return SupportStatus.PARTIAL;
        }
        boolean allBlocked = childStatuses.stream().allMatch(status -> status == SupportStatus.BLOCKED);
        if (allBlocked) {
            return SupportStatus.BLOCKED;
        }
        return SupportStatus.UNKNOWN;
    }

    private String refinementReason(Refinement refinement, List<SupportStatus> childStatuses, SupportStatus aggregate) {
        Map<SupportStatus, Integer> counts = new EnumMap<>(SupportStatus.class);
        for (SupportStatus status : childStatuses) {
            counts.merge(status, 1, Integer::sum);
        }

        return "Refinement " + refinement.getName() + " (" + refinement.getRefinementType()
                + ") => " + aggregate.name().toLowerCase()
                + " from children [supported=" + counts.getOrDefault(SupportStatus.SUPPORTED, 0)
                + ", partial=" + counts.getOrDefault(SupportStatus.PARTIAL, 0)
                + ", blocked=" + counts.getOrDefault(SupportStatus.BLOCKED, 0)
                + ", unknown=" + counts.getOrDefault(SupportStatus.UNKNOWN, 0) + "].";
    }

    private String contributionSummary(GoalTaskElement element) {
        if (!(element instanceof Goal goal) || goal.getIncomingContributions().isEmpty()) {
            return "";
        }

        int positive = 0;
        int negative = 0;
        for (Contribution contribution : goal.getIncomingContributions()) {
            if (contribution.getSource() instanceof GoalTaskElement source) {
                SupportStatus status = analyze(source).status;
                if (status != SupportStatus.SUPPORTED && status != SupportStatus.PARTIAL) {
                    continue;
                }
            }

            ContributionType type = contribution.getContributionType();
            if (type == ContributionType.MAKE || type == ContributionType.HELP || type == ContributionType.SOME_PLUS) {
                positive++;
            } else if (type == ContributionType.HURT || type == ContributionType.BREAK || type == ContributionType.SOME_MINUS) {
                negative++;
            }
        }

        if (positive == 0 && negative == 0) {
            return "";
        }
        return "Active contributions: +" + positive + ", -" + negative + ".";
    }

    private SyntaxStatus syntaxStatus(IntentionalElement element) {
        OclValidationReport report = validationReports.get(element);
        if (element instanceof Goal goal) {
            String expr = goal.getOclExpression();
            if (expr == null || expr.isBlank()) {
                return SyntaxStatus.MISSING;
            }
            return report != null && report.hasProblems() ? SyntaxStatus.ERROR : SyntaxStatus.OK;
        }
        if (element instanceof Task task) {
            boolean hasExpr = (task.getPreExpression() != null && !task.getPreExpression().isBlank())
                    || (task.getPostExpression() != null && !task.getPostExpression().isBlank());
            if (!hasExpr) {
                return SyntaxStatus.MISSING;
            }
            return report != null && report.hasProblems() ? SyntaxStatus.ERROR : SyntaxStatus.OK;
        }
        return SyntaxStatus.MISSING;
    }

    private SupportStatus combine(SupportStatus base, SupportStatus propagated) {
        if (base == SupportStatus.BLOCKED || propagated == SupportStatus.BLOCKED) {
            return SupportStatus.BLOCKED;
        }
        if (rank(propagated) > rank(base)) {
            return propagated;
        }
        return base;
    }

    private int rank(SupportStatus status) {
        return switch (status) {
            case BLOCKED -> 0;
            case UNKNOWN -> 1;
            case PARTIAL -> 2;
            case SUPPORTED -> 3;
        };
    }
}
