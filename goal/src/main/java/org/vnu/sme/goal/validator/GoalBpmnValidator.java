package org.vnu.sme.goal.validator;

import java.util.List;

import org.vnu.sme.goal.mm.GoalModel;
import org.vnu.sme.goal.mm.bpmn.BpmnModel;
import org.vnu.sme.goal.parser.GoalOclService;

public final class GoalBpmnValidator {
    public record ProcessTrace(String participant, String process, List<String> taskNames) {
        public boolean containsAll(java.util.Set<String> requiredTasks) {
            return taskNames.containsAll(requiredTasks);
        }

        public String display() {
            if (taskNames.isEmpty()) {
                return participant + "/" + process + ": <no tasks>";
            }
            return participant + "/" + process + ": " + String.join(" -> ", taskNames);
        }
    }

    public record GoalCheckRow(
            String actor,
            String goal,
            String type,
            String status,
            String detail) {
    }

    public record ObligationRow(
            String actor,
            String goal,
            String obligationType,
            String sourceTask,
            String target,
            String expression,
            String status,
            String detail) {
    }

    public record AnalysisReport(
            List<String> structuralIssues,
            List<String> mappingIssues,
            List<GoalCheckRow> goalRows,
            List<ObligationRow> obligationRows,
            int participantCount,
            int traceCount) {
    }

    private final GoalModel goalModel;
    private final BpmnModel bpmnModel;
    private final GoalOclService oclService;

    public GoalBpmnValidator(GoalModel goalModel, BpmnModel bpmnModel, GoalOclService oclService) {
        this.goalModel = goalModel;
        this.bpmnModel = bpmnModel;
        this.oclService = oclService;
    }

    public AnalysisReport analyze() {
        return new GoalBpmnProofEngine(goalModel, bpmnModel, oclService).analyze();
    }

    public String renderReport(String sourceName) {
        AnalysisReport report = analyze();
        StringBuilder text = new StringBuilder();
        text.append("GOAL + BPMN proof analysis\n");
        text.append("Goal model: ").append(goalModel.getName()).append("\n");
        text.append("BPMN model: ").append(bpmnModel.getName());
        if (sourceName != null && !sourceName.isBlank()) {
            text.append(" (").append(sourceName).append(")");
        }
        text.append("\n\n");
        text.append("Proof interpretation\n");
        text.append("- BPMN pool name must match GOAL agent name\n");
        text.append("- BPMN task name must match GOAL task name inside that agent\n");
        text.append("- Goal state is boolean: TRUE only when every required proof obligation is discharged\n");
        text.append("- FALSE means the goal is not proved by the current BPMN + task contracts\n");
        text.append("- OR-refinement is true if one child is operationalized\n");
        text.append("- AND-refinement is true if all children are operationalized on one complete BPMN trace\n");
        text.append("- Sequential obligations are generated as post(Ti) implies pre(Ti+1)\n");
        text.append("- Final obligations are generated as post(Tn) implies goalOcl\n");
        text.append("- The current checker is symbolic and conservative: it uses OCL MM structure, not runtime state evaluation\n\n");

        text.append("Participants: ").append(report.participantCount())
                .append(", traces explored: ").append(report.traceCount()).append("\n\n");

        text.append("Structural issues\n");
        appendLines(text, report.structuralIssues());
        text.append("\nMapping issues\n");
        appendLines(text, report.mappingIssues());

        text.append("\nGoal proof results\n");
        if (report.goalRows().isEmpty()) {
            text.append("- No goals found.\n");
        } else {
            for (GoalCheckRow row : report.goalRows()) {
                text.append("- ").append(row.actor()).append(".").append(row.goal())
                        .append(" [").append(row.type()).append("] => ")
                        .append(row.status()).append(": ")
                        .append(row.detail()).append("\n");
            }
        }

        text.append("\nObligations\n");
        if (report.obligationRows().isEmpty()) {
            text.append("- none\n");
        } else {
            for (ObligationRow row : report.obligationRows()) {
                text.append("- ").append(row.actor()).append(".").append(row.goal())
                        .append(" [").append(row.obligationType()).append("] ")
                        .append(row.sourceTask());
                if (row.target() != null && !row.target().isBlank()) {
                    text.append(" -> ").append(row.target());
                }
                text.append(" => ").append(row.status())
                        .append(": ").append(row.detail()).append("\n");
            }
        }

        return text.toString();
    }

    private static void appendLines(StringBuilder text, List<String> lines) {
        if (lines.isEmpty()) {
            text.append("- none\n");
            return;
        }
        for (String line : lines) {
            text.append("- ").append(line).append("\n");
        }
    }
}
