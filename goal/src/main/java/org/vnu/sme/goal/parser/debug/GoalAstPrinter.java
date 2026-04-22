package org.vnu.sme.goal.parser.debug;

import org.vnu.sme.goal.ast.ActorCS;
import org.vnu.sme.goal.ast.ActorDeclCS;
import org.vnu.sme.goal.ast.AgentCS;
import org.vnu.sme.goal.ast.DependencyCS;
import org.vnu.sme.goal.ast.GoalCS;
import org.vnu.sme.goal.ast.GoalModelCS;
import org.vnu.sme.goal.ast.IntentionalElementCS;
import org.vnu.sme.goal.ast.OutgoingLink;
import org.vnu.sme.goal.ast.QualityCS;
import org.vnu.sme.goal.ast.RelationCS;
import org.vnu.sme.goal.ast.ResourceCS;
import org.vnu.sme.goal.ast.RoleCS;
import org.vnu.sme.goal.ast.TaskCS;

public final class GoalAstPrinter {
    private GoalAstPrinter() {
    }

    public static String dump(GoalModelCS ast) {
        StringBuilder sb = new StringBuilder();
        sb.append("GoalModel(name=").append(ast.getfName().getText()).append(")\n");

        sb.append("  Actors:\n");
        for (ActorDeclCS actor : ast.getActorDeclsCS()) {
            dumpActor(actor, sb);
        }

        sb.append("  Dependencies:\n");
        for (RelationCS rel : ast.getRelationDeclsCS()) {
            if (rel instanceof DependencyCS dep) {
                dumpDependency(dep, sb);
            } else {
                sb.append("    - Relation(name=").append(rel.getfName().getText()).append(")\n");
            }
        }
        return sb.toString();
    }

    private static void dumpActor(ActorDeclCS actor, StringBuilder sb) {
        sb.append("    - ").append(actorKind(actor))
                .append("(name=").append(actor.getfName().getText()).append(")\n");

        if (!actor.getIsARefs().isEmpty()) {
            sb.append("      is-a: ").append(tokensToCsv(actor.getIsARefs().stream().map(t -> t.getText()).toList())).append("\n");
        }
        if (!actor.getParticipatesInRefs().isEmpty()) {
            sb.append("      participates-in: ")
                    .append(tokensToCsv(actor.getParticipatesInRefs().stream().map(t -> t.getText()).toList()))
                    .append("\n");
        }

        for (IntentionalElementCS e : actor.getIntentionalElements()) {
            sb.append("      * ").append(elementKind(e))
                    .append("(name=").append(e.getfName().getText()).append(")");
            if (e.getDescription() != null) {
                sb.append(" desc=\"").append(e.getDescription()).append("\"");
            }
            sb.append("\n");

            for (OutgoingLink link : e.getOutgoingLinks()) {
                sb.append("          -> ").append(link.kind())
                        .append(" target=").append(link.target().getText()).append("\n");
            }
        }
    }

    private static void dumpDependency(DependencyCS dep, StringBuilder sb) {
        sb.append("    - Dependency(name=").append(dep.getfName().getText()).append(")\n");
        sb.append("      depender=").append(dep.getDependerQualifiedName()).append("\n");
        sb.append("      dependee=").append(dep.getDependeeQualifiedName()).append("\n");
        IntentionalElementCS dependum = dep.getDependumElement();
        if (dependum != null) {
            sb.append("      dependum=").append(elementKind(dependum))
                    .append("(name=").append(dependum.getfName().getText()).append(")");
            if (dependum.getDescription() != null) {
                sb.append(" desc=\"").append(dependum.getDescription()).append("\"");
            }
            sb.append("\n");
        }
    }

    private static String actorKind(ActorDeclCS actor) {
        if (actor instanceof ActorCS) {
            return "Actor";
        }
        if (actor instanceof AgentCS) {
            return "Agent";
        }
        if (actor instanceof RoleCS) {
            return "Role";
        }
        return "ActorDecl";
    }

    private static String elementKind(IntentionalElementCS e) {
        if (e instanceof GoalCS) {
            return "Goal";
        }
        if (e instanceof TaskCS) {
            return "Task";
        }
        if (e instanceof QualityCS) {
            return "Quality";
        }
        if (e instanceof ResourceCS) {
            return "Resource";
        }
        return "IntentionalElement";
    }

    private static String tokensToCsv(java.util.List<String> values) {
        return String.join(", ", values);
    }
}

