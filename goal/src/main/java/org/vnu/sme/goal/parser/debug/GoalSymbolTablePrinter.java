package org.vnu.sme.goal.parser.debug;

import org.vnu.sme.goal.parser.semantic.symbols.ActorSymbol;
import org.vnu.sme.goal.parser.semantic.symbols.DependencySymbol;
import org.vnu.sme.goal.parser.semantic.symbols.ElementSymbol;
import org.vnu.sme.goal.parser.semantic.symbols.GoalSymbolTable;
import org.vnu.sme.goal.parser.semantic.symbols.RelationEntry;

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

    private static String pos(org.antlr.v4.runtime.Token token) {
        return token.getLine() + ":" + token.getCharPositionInLine();
    }
}

