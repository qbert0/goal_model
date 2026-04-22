package org.vnu.sme.goal.parser;

import java.util.stream.Collectors;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.vnu.sme.goal.ast.ActorCS;
import org.vnu.sme.goal.ast.ActorDeclCS;
import org.vnu.sme.goal.ast.AgentCS;
import org.vnu.sme.goal.ast.DependencyCS;
import org.vnu.sme.goal.ast.DescriptionContainerCS;
import org.vnu.sme.goal.ast.GoalCS;
import org.vnu.sme.goal.ast.GoalModelCS;
import org.vnu.sme.goal.ast.IntentionalElementCS;
import org.vnu.sme.goal.ast.OutgoingLink;
import org.vnu.sme.goal.ast.QualityCS;
import org.vnu.sme.goal.ast.ResourceCS;
import org.vnu.sme.goal.ast.RoleCS;
import org.vnu.sme.goal.ast.TaskCS;

/**
 * Mẫu visitor: parse tree GOAL → {@link GoalModelCS}. Mở rộng / tách semantic sau.
 */
public final class GoalModelBuildingVisitor extends GOALBaseVisitor<Object> {

    private GoalModelCS model;

    public static GoalModelCS build(GOALParser.GoalModelContext ctx) {
        GoalModelBuildingVisitor v = new GoalModelBuildingVisitor();
        v.visitGoalModel(ctx);
        return v.model;
    }

    @Override
    public Object visitGoalModel(GOALParser.GoalModelContext ctx) {
        model = new GoalModelCS(ctx.IDENT().getSymbol());
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree ch = ctx.getChild(i);
            if (ch instanceof GOALParser.ActorContext ac) {
                model.addActorDeclCS((ActorDeclCS) visit(ac));
            } else if (ch instanceof GOALParser.DependencyDefinitionContext dc) {
                model.addRelationDeclCS(visitDependencyDefinition(dc));
            }
        }
        return model;
    }

    @Override
    public Object visitActor(GOALParser.ActorContext ctx) {
        if (ctx.actorDefinition() != null) {
            return visitActorDefinition(ctx.actorDefinition());
        }
        if (ctx.agentDefinition() != null) {
            return visitAgentDefinition(ctx.agentDefinition());
        }
        return visitRoleDefinition(ctx.roleDefinition());
    }

    @Override
    public Object visitActorDefinition(GOALParser.ActorDefinitionContext ctx) {
        ActorCS actor = new ActorCS(ctx.IDENT(0).getSymbol());
        fillActorInheritance(ctx.COLON() != null, ctx.GT() != null, ctx, actor);
        fillActorBody(ctx.actorBody(), actor);
        return actor;
    }

    @Override
    public Object visitAgentDefinition(GOALParser.AgentDefinitionContext ctx) {
        AgentCS agent = new AgentCS(ctx.IDENT(0).getSymbol());
        fillActorInheritance(ctx.COLON() != null, ctx.GT() != null, ctx, agent);
        fillActorBody(ctx.actorBody(), agent);
        return agent;
    }

    @Override
    public Object visitRoleDefinition(GOALParser.RoleDefinitionContext ctx) {
        RoleCS role = new RoleCS(ctx.IDENT(0).getSymbol());
        fillActorInheritance(ctx.COLON() != null, ctx.GT() != null, ctx, role);
        fillActorBody(ctx.actorBody(), role);
        return role;
    }

    private static void fillActorInheritance(
            boolean hasColon,
            boolean hasGt,
            GOALParser.ActorDefinitionContext ctx,
            ActorDeclCS actor) {
        int idx = 1;
        if (hasColon) {
            actor.addIsARef(ctx.IDENT(idx++).getSymbol());
        }
        if (hasGt) {
            actor.addParticipatesInRef(ctx.IDENT(idx).getSymbol());
        }
    }

    private static void fillActorInheritance(
            boolean hasColon,
            boolean hasGt,
            GOALParser.AgentDefinitionContext ctx,
            ActorDeclCS actor) {
        int idx = 1;
        if (hasColon) {
            actor.addIsARef(ctx.IDENT(idx++).getSymbol());
        }
        if (hasGt) {
            actor.addParticipatesInRef(ctx.IDENT(idx).getSymbol());
        }
    }

    private static void fillActorInheritance(
            boolean hasColon,
            boolean hasGt,
            GOALParser.RoleDefinitionContext ctx,
            ActorDeclCS actor) {
        int idx = 1;
        if (hasColon) {
            actor.addIsARef(ctx.IDENT(idx++).getSymbol());
        }
        if (hasGt) {
            actor.addParticipatesInRef(ctx.IDENT(idx).getSymbol());
        }
    }

    private void fillActorBody(GOALParser.ActorBodyContext body, ActorDeclCS actor) {
        if (body == null) {
            return;
        }
        for (GOALParser.IntentionalElementContext ie : body.intentionalElement()) {
            actor.addIntentionalElement(visitIntentionalElement(ie));
        }
    }

    @Override
    public IntentionalElementCS visitIntentionalElement(GOALParser.IntentionalElementContext ctx) {
        if (ctx.goalDecl() != null) {
            return visitGoalDecl(ctx.goalDecl());
        }
        if (ctx.taskDecl() != null) {
            return visitTaskDecl(ctx.taskDecl());
        }
        if (ctx.qualityDecl() != null) {
            return visitQualityDecl(ctx.qualityDecl());
        }
        return visitResourceDecl(ctx.resourceDecl());
    }

    @Override
    public GoalCS visitGoalDecl(GOALParser.GoalDeclContext ctx) {
        GoalCS g = new GoalCS(ctx.IDENT().getSymbol());
        attachRelations(ctx.relationList(), g);
        applyDescription(ctx.elementBody(), g);
        return g;
    }

    @Override
    public TaskCS visitTaskDecl(GOALParser.TaskDeclContext ctx) {
        TaskCS t = new TaskCS(ctx.IDENT().getSymbol());
        attachRelations(ctx.relationList(), t);
        applyDescription(ctx.elementBody(), t);
        return t;
    }

    @Override
    public QualityCS visitQualityDecl(GOALParser.QualityDeclContext ctx) {
        QualityCS q = new QualityCS(ctx.IDENT().getSymbol());
        attachRelations(ctx.relationList(), q);
        applyDescription(ctx.elementBody(), q);
        return q;
    }

    @Override
    public ResourceCS visitResourceDecl(GOALParser.ResourceDeclContext ctx) {
        ResourceCS r = new ResourceCS(ctx.IDENT().getSymbol());
        attachRelations(ctx.relationList(), r);
        applyDescription(ctx.elementBody(), r);
        return r;
    }

    @Override
    public DependencyCS visitDependencyDefinition(GOALParser.DependencyDefinitionContext ctx) {
        DependencyCS dep = new DependencyCS(ctx.IDENT().getSymbol());
        dep.setDependerQualifiedName(qualifiedName(ctx.dependerClause().qualifiedName()));
        dep.setDependeeQualifiedName(qualifiedName(ctx.dependeeClause().qualifiedName()));
        dep.setDependumElement(visitIntentionalElement(ctx.dependumClause().intentionalElement()));
        return dep;
    }

    private static void attachRelations(GOALParser.RelationListContext list, IntentionalElementCS source) {
        if (list == null) {
            return;
        }
        for (GOALParser.RelationContext rel : list.relation()) {
            source.addOutgoingLink(new OutgoingLink(mapKind(rel.relOp()), rel.IDENT().getSymbol()));
        }
    }

    private static OutgoingLink.Kind mapKind(GOALParser.RelOpContext op) {
        int t = op.getStart().getType();
        if (t == GOALParser.AND_REFINE) {
            return OutgoingLink.Kind.REFINE_AND;
        }
        if (t == GOALParser.OR_REFINE) {
            return OutgoingLink.Kind.REFINE_OR;
        }
        if (t == GOALParser.CONTRIB_MAKE) {
            return OutgoingLink.Kind.CONTRIB_MAKE;
        }
        if (t == GOALParser.CONTRIB_HELP) {
            return OutgoingLink.Kind.CONTRIB_HELP;
        }
        if (t == GOALParser.CONTRIB_HURT) {
            return OutgoingLink.Kind.CONTRIB_HURT;
        }
        if (t == GOALParser.CONTRIB_BREAK) {
            return OutgoingLink.Kind.CONTRIB_BREAK;
        }
        if (t == GOALParser.QUALIFY) {
            return OutgoingLink.Kind.QUALIFY;
        }
        if (t == GOALParser.NEEDED_BY) {
            return OutgoingLink.Kind.NEEDED_BY;
        }
        throw new IllegalStateException("Unknown relOp: " + op.getText());
    }

    private static void applyDescription(GOALParser.ElementBodyContext body, DescriptionContainerCS target) {
        String text = mergeDescriptions(body);
        if (text != null) {
            target.setDescription(text);
        }
    }

    private static String mergeDescriptions(GOALParser.ElementBodyContext body) {
        if (body == null || body.descriptionClause().isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (GOALParser.DescriptionClauseContext dc : body.descriptionClause()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(unquote(dc.STRING().getText()));
        }
        return sb.toString();
    }

    private static String unquote(String raw) {
        if (raw.length() >= 2 && raw.charAt(0) == '"' && raw.charAt(raw.length() - 1) == '"') {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }

    private static String qualifiedName(GOALParser.QualifiedNameContext ctx) {
        return ctx.getTokens(GOALParser.IDENT).stream()
                .map(TerminalNode::getText)
                .collect(Collectors.joining("."));
    }
}
