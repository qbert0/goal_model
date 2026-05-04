package org.vnu.sme.goal.validator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.vnu.sme.goal.mm.Actor;
import org.vnu.sme.goal.mm.Agent;
import org.vnu.sme.goal.mm.Dependency;
import org.vnu.sme.goal.mm.Goal;
import org.vnu.sme.goal.mm.GoalModel;
import org.vnu.sme.goal.mm.GoalTaskElement;
import org.vnu.sme.goal.mm.OrRefinement;
import org.vnu.sme.goal.mm.Refinement;
import org.vnu.sme.goal.mm.Task;
import org.vnu.sme.goal.mm.ocl.Expression;
import org.vnu.sme.goal.mm.ocl.UnaryExp;
import org.vnu.sme.goal.mm.bpmn.BpmnCollaboration;
import org.vnu.sme.goal.mm.bpmn.BpmnEndEvent;
import org.vnu.sme.goal.mm.bpmn.BpmnFlowNode;
import org.vnu.sme.goal.mm.bpmn.BpmnMessageFlow;
import org.vnu.sme.goal.mm.bpmn.BpmnModel;
import org.vnu.sme.goal.mm.bpmn.BpmnParticipant;
import org.vnu.sme.goal.mm.bpmn.BpmnProcess;
import org.vnu.sme.goal.mm.bpmn.BpmnStartEvent;
import org.vnu.sme.goal.mm.bpmn.BpmnTask;
import org.vnu.sme.goal.parser.GoalOclService;
import org.vnu.sme.goal.validator.proof.OclEntailmentChecker;
import org.vnu.sme.goal.validator.proof.ProofCheckResult;
import org.vnu.sme.goal.validator.proof.ProofObligation;
import org.vnu.sme.goal.validator.proof.ProofObligationKind;

final class GoalBpmnProofEngine {
    private enum ProofStatus {
        PROVED,
        PARTIAL,
        INCOMPLETE,
        UNPROVEN,
        UNMAPPED
    }

    private record ProofResult(ProofStatus status, Set<String> requiredTasks, String detail) {
        static ProofResult proved(Set<String> tasks, String detail) {
            return new ProofResult(ProofStatus.PROVED, tasks, detail);
        }

        static ProofResult partial(Set<String> tasks, String detail) {
            return new ProofResult(ProofStatus.PARTIAL, tasks, detail);
        }

        static ProofResult incomplete(String detail) {
            return new ProofResult(ProofStatus.INCOMPLETE, Set.of(), detail);
        }

        static ProofResult unproven(String detail) {
            return new ProofResult(ProofStatus.UNPROVEN, Set.of(), detail);
        }

        static ProofResult unmapped(String detail) {
            return new ProofResult(ProofStatus.UNMAPPED, Set.of(), detail);
        }
    }

    private final GoalModel goalModel;
    private final BpmnModel bpmnModel;
    private final OclEntailmentChecker entailmentChecker = new OclEntailmentChecker();
    private final Map<String, BpmnParticipant> participantsByName = new LinkedHashMap<>();
    private final Map<String, List<GoalBpmnValidator.ProcessTrace>> tracesByParticipant = new LinkedHashMap<>();
    private final List<String> structuralIssues = new ArrayList<>();
    private final List<String> mappingIssues = new ArrayList<>();
    private final List<GoalBpmnValidator.ObligationRow> obligationRows = new ArrayList<>();

    GoalBpmnProofEngine(GoalModel goalModel, BpmnModel bpmnModel, GoalOclService oclService) {
        this.goalModel = goalModel;
        this.bpmnModel = bpmnModel;
        if (bpmnModel.getCollaboration() != null) {
            for (BpmnParticipant participant : bpmnModel.getCollaboration().getParticipants()) {
                participantsByName.put(participant.getName(), participant);
            }
        }
    }

    GoalBpmnValidator.AnalysisReport analyze() {
        tracesByParticipant.clear();
        structuralIssues.clear();
        mappingIssues.clear();
        obligationRows.clear();

        BpmnCollaboration collaboration = bpmnModel.getCollaboration();
        if (collaboration == null) {
            structuralIssues.add("BPMN model has no collaboration.");
        } else {
            for (BpmnParticipant participant : collaboration.getParticipants()) {
                tracesByParticipant.put(participant.getName(), enumerateTraces(participant));
            }
            analyzeDependencyCoverage(collaboration);
            analyzeTaskCoverage(collaboration);
        }

        List<GoalBpmnValidator.GoalCheckRow> goalRows = new ArrayList<>();
        for (Actor actor : goalModel.getActors()) {
            for (var element : actor.getElements()) {
                if (element instanceof Goal goal) {
                    goalRows.add(analyzeGoal(actor, goal));
                }
            }
        }

        int traceCount = tracesByParticipant.values().stream().mapToInt(List::size).sum();
        return new GoalBpmnValidator.AnalysisReport(
                List.copyOf(structuralIssues),
                List.copyOf(mappingIssues),
                List.copyOf(goalRows),
                List.copyOf(obligationRows),
                participantsByName.size(),
                traceCount);
    }

    private void analyzeDependencyCoverage(BpmnCollaboration collaboration) {
        for (Dependency dependency : goalModel.getDependencies()) {
            Actor depender = dependency.getDepender();
            Actor dependee = dependency.getDependee();
            if (!(depender instanceof Agent) || !(dependee instanceof Agent)) {
                continue;
            }
            boolean covered = false;
            for (BpmnMessageFlow messageFlow : collaboration.getMessageFlows()) {
                String source = messageFlow.getSourceParticipant().getName();
                String target = messageFlow.getTargetParticipant().getName();
                if ((source.equals(depender.getName()) && target.equals(dependee.getName()))
                        || (source.equals(dependee.getName()) && target.equals(depender.getName()))) {
                    covered = true;
                    break;
                }
            }
            if (!covered) {
                mappingIssues.add("Dependency '" + dependency.getName() + "' between actors '"
                        + depender.getName() + "' and '" + dependee.getName()
                        + "' has no BPMN messageFlow between the corresponding pools.");
            }
        }
    }

    private void analyzeTaskCoverage(BpmnCollaboration collaboration) {
        Set<String> seenGoalTasks = new LinkedHashSet<>();
        for (Actor actor : goalModel.getActors()) {
            if (!(actor instanceof Agent)) {
                continue;
            }
            for (var element : actor.getElements()) {
                if (element instanceof Task task) {
                    seenGoalTasks.add(actor.getName() + "::" + task.getName());
                    BpmnParticipant participant = participantsByName.get(actor.getName());
                    if (participant == null) {
                        mappingIssues.add("Actor '" + actor.getName() + "' has task '" + task.getName()
                                + "' but no BPMN pool with the same name.");
                        continue;
                    }
                    boolean found = participant.getProcess().getTasks().stream()
                            .anyMatch(bpmnTask -> bpmnTask.getName().equals(task.getName()));
                    if (!found) {
                        mappingIssues.add("GOAL task '" + actor.getName() + "." + task.getName()
                                + "' does not appear in BPMN pool '" + participant.getName() + "'.");
                    }
                }
            }
        }

        for (BpmnParticipant participant : collaboration.getParticipants()) {
            Actor actor = goalModel.getActor(participant.getName());
            if (!(actor instanceof Agent)) {
                mappingIssues.add("BPMN pool '" + participant.getName() + "' must match a GOAL agent.");
            }
            for (BpmnTask task : participant.getProcess().getTasks()) {
                String key = participant.getName() + "::" + task.getName();
                if (!seenGoalTasks.contains(key)) {
                    mappingIssues.add("BPMN task '" + participant.getName() + "." + task.getName()
                            + "' has no matching GOAL task in the same actor.");
                }
            }
        }
    }

    private GoalBpmnValidator.GoalCheckRow analyzeGoal(Actor actor, Goal goal) {
        String type = goal.getGoalType() == null ? "none" : goal.getGoalType().name().toLowerCase();
        ProofResult result = proveGoal(actor, goal, new HashSet<>());
        return new GoalBpmnValidator.GoalCheckRow(
                actor.getName(),
                goal.getName(),
                type,
                result.status() == ProofStatus.PROVED ? "TRUE" : "FALSE",
                result.detail());
    }

    private ProofResult proveGoal(Actor actor, Goal goal, Set<String> visitingGoals) {
        String key = actor.getName() + "::" + goal.getName();
        if (!visitingGoals.add(key)) {
            return ProofResult.unproven("Detected recursive refinement while proving goal '" + goal.getName() + "'.");
        }
        try {
            if (goal.getParentRefinements().isEmpty()) {
                if (goal.getOclExpression() == null || goal.getOclExpression().isBlank()) {
                    return ProofResult.incomplete("Goal has no refinement and no OCL clause.");
                }
                if (actor instanceof Agent) {
                    BpmnParticipant participant = participantsByName.get(actor.getName());
                    if (participant == null) {
                        return ProofResult.unmapped("Agent '" + actor.getName()
                                + "' has no BPMN pool, so this goal is not operationalized by any process.");
                    }
                    List<GoalBpmnValidator.ProcessTrace> traces = tracesByParticipant.getOrDefault(actor.getName(), List.of());
                    if (traces.isEmpty()) {
                        return ProofResult.unmapped("Agent '" + actor.getName()
                                + "' has a BPMN pool but no complete process trace.");
                    }
                    return proveLeafGoalAgainstTraces(actor, goal, traces);
                }
                return ProofResult.proved(Set.of(),
                        "Leaf goal is formally specified by OCL. BPMN proof is not required because the owner is not an agent.");
            }

            List<GoalBpmnValidator.ProcessTrace> traces = actor instanceof Agent
                    ? tracesByParticipant.getOrDefault(actor.getName(), List.of())
                    : List.of();
            ProofResult bestPartial = null;
            for (Refinement refinement : goal.getParentRefinements()) {
                ProofResult candidate = proveRefinement(actor, goal, refinement, traces, visitingGoals);
                if (candidate.status() == ProofStatus.PROVED) {
                    return candidate;
                }
                if (bestPartial == null || rank(candidate.status()) > rank(bestPartial.status())) {
                    bestPartial = candidate;
                }
            }
            return bestPartial == null
                    ? ProofResult.unproven("No BPMN-supported refinement could prove this goal.")
                    : bestPartial;
        } finally {
            visitingGoals.remove(key);
        }
    }

    private ProofResult proveRefinement(Actor actor,
                                        Goal goal,
                                        Refinement refinement,
                                        List<GoalBpmnValidator.ProcessTrace> traces,
                                        Set<String> visitingGoals) {
        if (refinement instanceof OrRefinement) {
            ProofResult best = ProofResult.unproven("No OR-branch is operationalized by BPMN.");
            for (GoalTaskElement child : refinement.getChildren()) {
                ProofResult childResult = proveElement(actor, goal, child, visitingGoals);
                if (childResult.status() == ProofStatus.PROVED) {
                    return ProofResult.proved(
                            childResult.requiredTasks(),
                            "Proved by OR-refinement via child '" + child.getName() + "'. " + childResult.detail());
                }
                if (rank(childResult.status()) > rank(best.status())) {
                    best = childResult;
                }
            }
            return best.status() == ProofStatus.UNPROVEN
                    ? ProofResult.unproven("No OR-branch of goal '" + goal.getName() + "' is provable from BPMN.")
                    : best;
        }

        Set<String> requiredTasks = new LinkedHashSet<>();
        List<String> unresolvedChildren = new ArrayList<>();
        boolean hasGoalChildren = false;
        for (GoalTaskElement child : refinement.getChildren()) {
            ProofResult childResult = proveElement(actor, goal, child, visitingGoals);
            if (child instanceof Goal) {
                hasGoalChildren = true;
            }
            if (childResult.status() != ProofStatus.PROVED) {
                unresolvedChildren.add(child.getName() + " => " + childResult.status().name().toLowerCase());
            }
            requiredTasks.addAll(childResult.requiredTasks());
        }

        if (!unresolvedChildren.isEmpty()) {
            return ProofResult.partial(
                    requiredTasks,
                    "AND-refinement of goal '" + goal.getName()
                            + "' is only partially operationalized. Unresolved children: "
                            + String.join(", ", unresolvedChildren));
        }

        if (requiredTasks.isEmpty() && hasGoalChildren) {
            return ProofResult.proved(
                    Set.of(),
                    "All sub-goals of AND-refinement are proved true.");
        }
        if (requiredTasks.isEmpty() && !(actor instanceof Agent)) {
            return ProofResult.proved(
                    Set.of(),
                    "Refinement is discharged through sub-goals; no BPMN task witness is required for non-agent owner.");
        }
        for (GoalBpmnValidator.ProcessTrace trace : traces) {
            if (trace.containsAll(requiredTasks)) {
                ProofResult semanticProof = validateTaskSequence(actor, goal, trace, requiredTasks);
                if (semanticProof.status() != ProofStatus.PROVED) {
                    continue;
                }
                return ProofResult.proved(
                        requiredTasks,
                        "Proved by AND-refinement on BPMN trace: " + trace.display()
                                + " | " + semanticProof.detail());
            }
        }
        return ProofResult.unproven(
                "All children are mapped, but no single BPMN trace of pool '" + actor.getName()
                        + "' covers the full AND-refinement task set " + requiredTasks + ".");
    }

    private ProofResult proveElement(Actor actor, Goal parentGoal, GoalTaskElement element, Set<String> visitingGoals) {
        if (element instanceof Task task) {
            return proveTask(actor, parentGoal, task);
        }
        if (element instanceof Goal childGoal) {
            Actor owner = childGoal.getOwner() == null ? actor : childGoal.getOwner();
            return proveGoal(owner, childGoal, visitingGoals);
        }
        return ProofResult.unproven("Unsupported goal element '" + element.getName() + "'.");
    }

    private ProofResult proveTask(Actor actor, Goal parentGoal, Task task) {
        if (!(actor instanceof Agent)) {
            return ProofResult.proved(
                    Set.of(),
                    "Task '" + task.getName() + "' is treated as an auxiliary state-change constraint outside BPMN agent scope.");
        }
        BpmnParticipant participant = participantsByName.get(actor.getName());
        if (participant == null) {
            obligationRows.add(new GoalBpmnValidator.ObligationRow(
                    actor.getName(), parentGoal.getName(), "task-coverage", task.getName(), "",
                    task.getName(), "FALSE",
                    "Missing BPMN pool for agent '" + actor.getName() + "'."));
            return ProofResult.unmapped("Task '" + task.getName() + "' belongs to actor '" + actor.getName()
                    + "' but there is no BPMN pool with that name.");
        }

        List<GoalBpmnValidator.ProcessTrace> traces = tracesByParticipant.getOrDefault(actor.getName(), List.of());
        for (GoalBpmnValidator.ProcessTrace trace : traces) {
            if (trace.taskNames().contains(task.getName())) {
                obligationRows.add(new GoalBpmnValidator.ObligationRow(
                        actor.getName(), parentGoal.getName(), "task-coverage", task.getName(), trace.process(),
                        task.getName(), "TRUE",
                        "Task occurs on BPMN trace " + trace.display() + "."));
                return ProofResult.proved(Set.of(task.getName()),
                        "Task '" + task.getName() + "' is executed in BPMN trace " + trace.display() + ".");
            }
        }
        obligationRows.add(new GoalBpmnValidator.ObligationRow(
                actor.getName(), parentGoal.getName(), "task-coverage", task.getName(), "",
                task.getName(), "FALSE",
                "Task does not occur on any complete BPMN trace."));
        return ProofResult.unproven("Task '" + task.getName() + "' is not reachable on any complete BPMN trace of pool '"
                + actor.getName() + "'.");
    }

    private ProofResult validateTaskSequence(Actor actor,
                                             Goal goal,
                                             GoalBpmnValidator.ProcessTrace trace,
                                             Set<String> requiredTasks) {
        List<Task> orderedTasks = new ArrayList<>();
        Map<String, Task> taskByName = taskMap(actor);
        for (String taskName : trace.taskNames()) {
            if (!requiredTasks.contains(taskName)) {
                continue;
            }
            Task task = taskByName.get(taskName);
            if (task == null) {
                obligationRows.add(new GoalBpmnValidator.ObligationRow(
                        actor.getName(), goal.getName(), "sequence-resolution", taskName, "",
                        taskName, "FALSE",
                        "Task cannot be resolved in agent '" + actor.getName() + "'."));
                return ProofResult.unproven("Trace references task '" + taskName
                        + "', but that task cannot be resolved in agent '" + actor.getName() + "'.");
            }
            orderedTasks.add(task);
        }

        if (orderedTasks.isEmpty()) {
            return ProofResult.proved(Set.of(), "No task obligation was required on this trace.");
        }

        List<String> obligations = new ArrayList<>();
        for (int i = 0; i < orderedTasks.size() - 1; i++) {
            Task current = orderedTasks.get(i);
            Task next = orderedTasks.get(i + 1);
            Expression post = postExpression(current);
            Expression pre = preExpression(next);
            if (post == null) {
                obligationRows.add(new GoalBpmnValidator.ObligationRow(
                    actor.getName(), goal.getName(), "post-implies-pre", current.getName(), next.getName(),
                    "", "FALSE",
                    "Missing post-condition on source task."));
                return ProofResult.unproven("Task '" + current.getName() + "' has no post-condition for proving transition to '"
                        + next.getName() + "'.");
            }
            if (pre == null) {
                obligationRows.add(new GoalBpmnValidator.ObligationRow(
                    actor.getName(), goal.getName(), "post-implies-pre", current.getName(), next.getName(),
                    "", "FALSE",
                    "Missing pre-condition on target task."));
                return ProofResult.unproven("Task '" + next.getName() + "' has no pre-condition for proving sequence semantics.");
            }
            ProofObligation obligation = new ProofObligation(
                    actor.getName(),
                    goal.getName(),
                    ProofObligationKind.POST_IMPLIES_PRE,
                    current.getName(),
                    next.getName(),
                    post,
                    pre);
            ProofCheckResult result = entailmentChecker.check(obligation);
            obligationRows.add(new GoalBpmnValidator.ObligationRow(
                    actor.getName(), goal.getName(), "post-implies-pre", current.getName(), next.getName(),
                    obligation.displayExpression(), result.externalStatus(), result.detail()));
            if (!result.proved()) {
                return ProofResult.unproven("Sequence obligation failed: " + obligation.displayExpression()
                        + " => " + result.detail());
            }
            obligations.add(current.getName() + "=>" + next.getName());
        }

        Task lastTask = orderedTasks.get(orderedTasks.size() - 1);
        Expression goalExpr = goalExpression(goal);
        if (goalExpr != null) {
            Expression post = postExpression(lastTask);
            if (post == null) {
                obligationRows.add(new GoalBpmnValidator.ObligationRow(
                    actor.getName(), goal.getName(), "post-implies-goal", lastTask.getName(), goal.getName(),
                    "", "FALSE",
                    "Missing post-condition on final task."));
                return ProofResult.unproven("Task '" + lastTask.getName()
                        + "' has no post-condition for proving the goal '" + goal.getName() + "'.");
            }
            ProofObligation obligation = new ProofObligation(
                    actor.getName(),
                    goal.getName(),
                    ProofObligationKind.POST_IMPLIES_GOAL,
                    lastTask.getName(),
                    goal.getName(),
                    post,
                    goalExpr);
            ProofCheckResult result = entailmentChecker.check(obligation);
            obligationRows.add(new GoalBpmnValidator.ObligationRow(
                    actor.getName(), goal.getName(), "post-implies-goal", lastTask.getName(), goal.getName(),
                    obligation.displayExpression(), result.externalStatus(), result.detail()));
            if (!result.proved()) {
                return ProofResult.unproven("Goal obligation failed: " + obligation.displayExpression()
                        + " => " + result.detail());
            }
            obligations.add(lastTask.getName() + "=>goal");
        }

        return ProofResult.proved(
                requiredTasks,
                obligations.isEmpty()
                        ? "Task sequence proof discharged."
                        : "Semantic obligations hold: " + String.join(", ", obligations) + ".");
    }

    private ProofResult proveLeafGoalAgainstTraces(Actor actor,
                                                   Goal goal,
                                                   List<GoalBpmnValidator.ProcessTrace> traces) {
        Map<String, Task> taskByName = taskMap(actor);
        List<GoalBpmnValidator.ObligationRow> localRows = new ArrayList<>();

        return switch (goal.getGoalType()) {
            case ACHIEVE -> proveAchieveLeaf(actor, goal, traces, taskByName, localRows);
            case MAINTAIN -> proveMaintainLeaf(actor, goal, traces, taskByName, localRows);
            case AVOID -> proveAvoidLeaf(actor, goal, traces, taskByName, localRows);
            case null -> ProofResult.unproven("Goal has OCL but no goal type.");
        };
    }

    private ProofResult proveAchieveLeaf(Actor actor,
                                         Goal goal,
                                         List<GoalBpmnValidator.ProcessTrace> traces,
                                         Map<String, Task> taskByName,
                                         List<GoalBpmnValidator.ObligationRow> localRows) {
        for (GoalBpmnValidator.ProcessTrace trace : traces) {
            Task lastTask = resolveLastTask(trace, taskByName, actor, goal, localRows);
            if (lastTask == null) {
                continue;
            }
            ProofCheckResult proof = evaluateGoalObligation(actor, goal, lastTask, false);
            localRows.add(obligationRow(actor, goal, ProofObligationKind.POST_IMPLIES_GOAL,
                    lastTask.getName(), goal.getName(), buildGoalObligation(lastTask, goal, false), proof));
            if (proof.proved()) {
                obligationRows.addAll(localRows);
                return ProofResult.proved(Set.of(),
                        "Achieve-goal is satisfied on BPMN trace " + trace.display() + ".");
            }
        }
        obligationRows.addAll(localRows);
        return ProofResult.unproven("No BPMN trace has a final task whose post-condition implies achieve-goal '"
                + goal.getName() + "'.");
    }

    private ProofResult proveMaintainLeaf(Actor actor,
                                          Goal goal,
                                          List<GoalBpmnValidator.ProcessTrace> traces,
                                          Map<String, Task> taskByName,
                                          List<GoalBpmnValidator.ObligationRow> localRows) {
        boolean sawTrace = false;
        for (GoalBpmnValidator.ProcessTrace trace : traces) {
            sawTrace = true;
            for (String taskName : trace.taskNames()) {
                Task task = taskByName.get(taskName);
                if (task == null || postExpression(task) == null) {
                    localRows.add(new GoalBpmnValidator.ObligationRow(
                            actor.getName(), goal.getName(), "post-preserves-goal",
                            taskName, goal.getName(), "", "FALSE",
                            "Missing task or post-condition for maintain proof."));
                    obligationRows.addAll(localRows);
                    return ProofResult.unproven("Maintain-goal '" + goal.getName()
                            + "' cannot be proved because task '" + taskName + "' has no usable post-condition.");
                }
                ProofCheckResult proof = evaluateGoalObligation(actor, goal, task, false);
                localRows.add(obligationRow(actor, goal, ProofObligationKind.POST_PRESERVES_GOAL,
                        task.getName(), goal.getName(), buildGoalObligation(task, goal, false), proof));
                if (!proof.proved()) {
                    obligationRows.addAll(localRows);
                    return ProofResult.unproven("Maintain-goal '" + goal.getName()
                            + "' is violated because task '" + task.getName() + "' does not preserve it.");
                }
            }
        }
        obligationRows.addAll(localRows);
        if (!sawTrace) {
            return ProofResult.unproven("No BPMN trace is available to prove maintain-goal '" + goal.getName() + "'.");
        }
        return ProofResult.proved(Set.of(), "Maintain-goal is preserved by every task on every BPMN trace.");
    }

    private ProofResult proveAvoidLeaf(Actor actor,
                                       Goal goal,
                                       List<GoalBpmnValidator.ProcessTrace> traces,
                                       Map<String, Task> taskByName,
                                       List<GoalBpmnValidator.ObligationRow> localRows) {
        boolean sawTrace = false;
        for (GoalBpmnValidator.ProcessTrace trace : traces) {
            sawTrace = true;
            for (String taskName : trace.taskNames()) {
                Task task = taskByName.get(taskName);
                if (task == null || postExpression(task) == null) {
                    localRows.add(new GoalBpmnValidator.ObligationRow(
                            actor.getName(), goal.getName(), "post-avoids-goal",
                            taskName, goal.getName(), "", "FALSE",
                            "Missing task or post-condition for avoid proof."));
                    obligationRows.addAll(localRows);
                    return ProofResult.unproven("Avoid-goal '" + goal.getName()
                            + "' cannot be proved because task '" + taskName + "' has no usable post-condition.");
                }
                ProofCheckResult proof = evaluateGoalObligation(actor, goal, task, true);
                localRows.add(obligationRow(actor, goal, ProofObligationKind.POST_AVOIDS_GOAL,
                        task.getName(), goal.getName(), buildGoalObligation(task, goal, true), proof));
                if (!proof.proved()) {
                    obligationRows.addAll(localRows);
                    return ProofResult.unproven("Avoid-goal '" + goal.getName()
                            + "' is violated because task '" + task.getName() + "' may establish the forbidden condition.");
                }
            }
        }
        obligationRows.addAll(localRows);
        if (!sawTrace) {
            return ProofResult.unproven("No BPMN trace is available to prove avoid-goal '" + goal.getName() + "'.");
        }
        return ProofResult.proved(Set.of(), "Avoid-goal is preserved by every task on every BPMN trace.");
    }

    private Task resolveLastTask(GoalBpmnValidator.ProcessTrace trace,
                                 Map<String, Task> taskByName,
                                 Actor actor,
                                 Goal goal,
                                 List<GoalBpmnValidator.ObligationRow> localRows) {
        if (trace.taskNames().isEmpty()) {
            localRows.add(new GoalBpmnValidator.ObligationRow(
                    actor.getName(), goal.getName(), "post-implies-goal",
                    "", goal.getName(), "", "FALSE",
                    "Trace has no task to discharge the goal."));
            return null;
        }
        String lastTaskName = trace.taskNames().get(trace.taskNames().size() - 1);
        Task lastTask = taskByName.get(lastTaskName);
        if (lastTask == null || postExpression(lastTask) == null) {
            localRows.add(new GoalBpmnValidator.ObligationRow(
                    actor.getName(), goal.getName(), "post-implies-goal",
                    lastTaskName, goal.getName(), "", "FALSE",
                    "Final task is missing or has no post-condition."));
            return null;
        }
        return lastTask;
    }

    private ProofCheckResult evaluateGoalObligation(Actor actor, Goal goal, Task task, boolean negateGoal) {
        ProofObligation obligation = buildGoalObligation(task, goal, negateGoal);
        return entailmentChecker.check(obligation);
    }

    private ProofObligation buildGoalObligation(Task task, Goal goal, boolean negateGoal) {
        Expression consequent = negateGoal
                ? new UnaryExp("not (" + goal.getOclExpression() + ")", UnaryExp.Operator.NOT, goalExpression(goal))
                : goalExpression(goal);
        return new ProofObligation(
                task.getOwner() == null ? "" : task.getOwner().getName(),
                goal.getName(),
                negateGoal ? ProofObligationKind.POST_AVOIDS_GOAL : ProofObligationKind.POST_IMPLIES_GOAL,
                task.getName(),
                goal.getName(),
                postExpression(task),
                consequent);
    }

    private GoalBpmnValidator.ObligationRow obligationRow(Actor actor,
                                                          Goal goal,
                                                          ProofObligationKind kind,
                                                          String source,
                                                          String target,
                                                          ProofObligation obligation,
                                                          ProofCheckResult result) {
        return new GoalBpmnValidator.ObligationRow(
                actor.getName(),
                goal.getName(),
                kind.externalName(),
                source,
                target,
                obligation.displayExpression(),
                result.externalStatus(),
                result.detail());
    }

    private Expression goalExpression(Goal goal) {
        return goal.getGoalClause() == null || goal.getGoalClause().getExpressions().isEmpty()
                ? null
                : goal.getGoalClause().getExpressions().get(0);
    }

    private Expression preExpression(Task task) {
        return task.getPre() == null || task.getPre().getExpressions().isEmpty()
                ? null
                : task.getPre().getExpressions().get(0);
    }

    private Expression postExpression(Task task) {
        return task.getPost() == null || task.getPost().getExpressions().isEmpty()
                ? null
                : task.getPost().getExpressions().get(0);
    }

    private Map<String, Task> taskMap(Actor actor) {
        Map<String, Task> taskByName = new LinkedHashMap<>();
        for (var element : actor.getElements()) {
            if (element instanceof Task task) {
                taskByName.put(task.getName(), task);
            }
        }
        return taskByName;
    }

    private List<GoalBpmnValidator.ProcessTrace> enumerateTraces(BpmnParticipant participant) {
        List<GoalBpmnValidator.ProcessTrace> traces = new ArrayList<>();
        BpmnProcess process = participant.getProcess();
        List<BpmnStartEvent> starts = process.getFlowNodesByType(BpmnStartEvent.class);
        List<BpmnEndEvent> ends = process.getFlowNodesByType(BpmnEndEvent.class);
        if (starts.isEmpty()) {
            structuralIssues.add("Process '" + process.getName() + "' has no startEvent.");
            return traces;
        }
        if (ends.isEmpty()) {
            structuralIssues.add("Process '" + process.getName() + "' has no endEvent.");
            return traces;
        }

        boolean[] found = { false };
        boolean[] cycle = { false };
        for (BpmnStartEvent start : starts) {
            dfs(participant, process, start, new HashSet<>(), new ArrayDeque<>(), traces, found, cycle);
        }
        if (!found[0]) {
            structuralIssues.add("Process '" + process.getName() + "' has no complete start-to-end trace.");
        }
        if (cycle[0]) {
            structuralIssues.add("Process '" + process.getName()
                    + "' contains a cycle. Proof is currently bounded to acyclic exploration.");
        }
        return traces;
    }

    private void dfs(BpmnParticipant participant,
                     BpmnProcess process,
                     BpmnFlowNode current,
                     Set<String> visiting,
                     Deque<String> taskPath,
                     List<GoalBpmnValidator.ProcessTrace> traces,
                     boolean[] found,
                     boolean[] cycle) {
        if (!visiting.add(current.getName())) {
            cycle[0] = true;
            return;
        }
        if (current instanceof BpmnTask task) {
            taskPath.addLast(task.getName());
        }

        if (current instanceof BpmnEndEvent) {
            found[0] = true;
            traces.add(new GoalBpmnValidator.ProcessTrace(participant.getName(), process.getName(), List.copyOf(taskPath)));
        } else {
            for (BpmnFlowNode next : process.getOutgoing(current)) {
                dfs(participant, process, next, visiting, taskPath, traces, found, cycle);
            }
        }

        if (current instanceof BpmnTask) {
            taskPath.removeLast();
        }
        visiting.remove(current.getName());
    }

    private int rank(ProofStatus status) {
        return switch (status) {
            case PROVED -> 5;
            case PARTIAL -> 4;
            case INCOMPLETE -> 3;
            case UNPROVEN -> 2;
            case UNMAPPED -> 1;
        };
    }
}
