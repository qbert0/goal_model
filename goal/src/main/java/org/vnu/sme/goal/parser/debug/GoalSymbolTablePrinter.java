package org.vnu.sme.goal.parser.debug;

import org.vnu.sme.goal.mm.ocl.VariableDeclaration;
import org.vnu.sme.goal.parser.semantic.symbols.ActorSymbol;
import org.vnu.sme.goal.parser.semantic.symbols.DependencySymbol;
import org.vnu.sme.goal.parser.semantic.symbols.ElementSymbol;
import org.vnu.sme.goal.parser.semantic.symbols.GoalContract;
import org.vnu.sme.goal.parser.semantic.symbols.GoalSymbolTable;
import org.vnu.sme.goal.parser.semantic.symbols.RelationEntry;
import org.vnu.sme.goal.parser.semantic.symbols.TaskContract;

public final class GoalSymbolTablePrinter {
    private GoalSymbolTablePrinter() {
    }

    public static String dump(String title, GoalSymbolTable table) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(title).append(" ===\n");
        sb.append("SymbolTable(model=").append(table.getModelName()).append(")\n");

        sb.append("  actorsByName:\n");
        for (ActorSymbol actor : table.getActorsByName().values()) {
            sb.append("    - ").append(actor.getName())
                    .append(" kind=").append(actor.getKind())
                    .append(" @").append(pos(actor.getDeclarationToken()))
                    .append("\n");
            for (ElementSymbol element : actor.getElementTable().values()) {
                sb.append("      * ").append(element.getQualifiedName())
                        .append(" kind=").append(element.getKind())
                        .append(" leaf=").append(element.isLeaf())
                        .append(" @").append(pos(element.getDeclarationToken()))
                        .append("\n");
                for (RelationEntry relation : element.getRelations()) {
                    sb.append("          -> op=").append(relation.getOperator())
                            .append(" targetRaw=").append(relation.getTargetRef().getText())
                            .append(" @").append(pos(relation.getTargetRef()));
                    if (relation.getResolvedTarget() != null) {
                        sb.append(" resolved=").append(relation.getResolvedTarget().getQualifiedName());
                    }
                    sb.append("\n");
                }
                appendGoalContract(sb, element.getGoalContract());
                appendTaskContract(sb, element.getTaskContract());
            }
        }

        sb.append("  elementsByQualifiedName:\n");
        for (String qName : table.getElementsByQualifiedName().keySet()) {
            sb.append("    - ").append(qName).append("\n");
        }

        sb.append("  dependenciesByName:\n");
        for (DependencySymbol dep : table.getDependenciesByName().values()) {
            sb.append("    - ").append(dep.getName())
                    .append(" @").append(pos(dep.getDeclarationToken()))
                    .append("\n");
            sb.append("      dependerRaw=").append(dep.getDependerRawRef()).append("\n");
            sb.append("      dependeeRaw=").append(dep.getDependeeRawRef()).append("\n");
            sb.append("      dependerResolved=")
                    .append(dep.getDepender() == null ? "<unresolved>" : dep.getDepender().getQualifiedName())
                    .append("\n");
            sb.append("      dependeeResolved=")
                    .append(dep.getDependee() == null ? "<unresolved>" : dep.getDependee().getQualifiedName())
                    .append("\n");
            sb.append("      dependum=")
                    .append(dep.getDependum() == null ? "<none>" : dep.getDependum().getQualifiedName())
                    .append("\n");
        }
        return sb.toString();
    }

    private static void appendGoalContract(StringBuilder sb, GoalContract contract) {
        if (contract == null) {
            return;
        }
        sb.append("          [goalContract] type=").append(contract.getType());
        if (!contract.getIterVars().isEmpty()) {
            sb.append(" iterVars=").append(formatVars(contract.getIterVars()));
        }
        if (contract.getSourceExpr() != null) {
            sb.append(" source='").append(contract.getSourceExpr().getText()).append("'");
        }
        sb.append(" body=");
        sb.append(contract.getBodyExpr() == null ? "<empty>" : "'" + contract.getBodyExpr().getText() + "'");
        sb.append("\n");
    }

    private static void appendTaskContract(StringBuilder sb, TaskContract contract) {
        if (contract == null) {
            return;
        }
        sb.append("          [taskContract]");
        sb.append(" pre=");
        sb.append(contract.getPrecondition() == null ? "<none>" : "'" + contract.getPrecondition().getText() + "'");
        sb.append(" post=");
        sb.append(contract.getPostcondition() == null ? "<none>" : "'" + contract.getPostcondition().getText() + "'");
        sb.append("\n");
    }

    private static String formatVars(java.util.List<VariableDeclaration> vars) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < vars.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            VariableDeclaration v = vars.get(i);
            out.append(v.getName());
            if (v.getTypeName() != null) {
                out.append(": ").append(v.getTypeName());
            }
        }
        return out.append("]").toString();
    }

    private static String pos(org.antlr.v4.runtime.Token token) {
        return token.getLine() + ":" + token.getCharPositionInLine();
    }
}
