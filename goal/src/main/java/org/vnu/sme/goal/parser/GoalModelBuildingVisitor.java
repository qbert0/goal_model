package org.vnu.sme.goal.parser;

import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.vnu.sme.goal.ast.ActorCS;
import org.vnu.sme.goal.ast.ActorDeclCS;
import org.vnu.sme.goal.ast.AgentCS;
import org.vnu.sme.goal.ast.DependencyCS;
import org.vnu.sme.goal.ast.GoalCS;
import org.vnu.sme.goal.ast.GoalModelCS;
import org.vnu.sme.goal.ast.IntentionalElementCS;
import org.vnu.sme.goal.ast.OutgoingLink;
import org.vnu.sme.goal.ast.QualityCS;
import org.vnu.sme.goal.ast.ResourceCS;
import org.vnu.sme.goal.ast.RoleCS;
import org.vnu.sme.goal.ast.TaskCS;
import org.vnu.sme.goal.ast.ocl.OclExpressionCS;

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
        fillActor(
                actor,
                ctx.COLON() != null ? ctx.IDENT(1).getSymbol() : null,
                ctx.GT() != null ? ctx.IDENT(ctx.COLON() != null ? 2 : 1).getSymbol() : null,
                ctx.actorBody());
        return actor;
    }

    @Override
    public Object visitAgentDefinition(GOALParser.AgentDefinitionContext ctx) {
        AgentCS agent = new AgentCS(ctx.IDENT(0).getSymbol());
        fillActor(
                agent,
                ctx.COLON() != null ? ctx.IDENT(1).getSymbol() : null,
                ctx.GT() != null ? ctx.IDENT(ctx.COLON() != null ? 2 : 1).getSymbol() : null,
                ctx.actorBody());
        return agent;
    }

    @Override
    public Object visitRoleDefinition(GOALParser.RoleDefinitionContext ctx) {
        RoleCS role = new RoleCS(ctx.IDENT(0).getSymbol());
        fillActor(
                role,
                ctx.COLON() != null ? ctx.IDENT(1).getSymbol() : null,
                ctx.GT() != null ? ctx.IDENT(ctx.COLON() != null ? 2 : 1).getSymbol() : null,
                ctx.actorBody());
        return role;
    }

    @Override
    public IntentionalElementCS visitIntentionalElement(GOALParser.IntentionalElementContext ctx) {
        return (IntentionalElementCS) visit(ctx.getChild(0));
    }

    @Override
    public GoalCS visitGoalDecl(GOALParser.GoalDeclContext ctx) {
        GoalCS goal = new GoalCS(ctx.IDENT().getSymbol());
        attachRelations(ctx.relationList(), goal);

        if (ctx.goalBody().descriptionClause() != null) {
            goal.setDescription(unquote(ctx.goalBody().descriptionClause().STRING().getText()));
        }

        GOALParser.GoalClauseContext clause = ctx.goalBody().goalClause();
        if (clause != null) {
            ParserRuleContext child = (ParserRuleContext) clause.getChild(0);
            goal.setGoalType(goalType(child));
            goal.setOclExpression(expression(child));
        }

        return goal;
    }

    @Override
    public TaskCS visitTaskDecl(GOALParser.TaskDeclContext ctx) {
        TaskCS task = new TaskCS(ctx.IDENT().getSymbol());
        attachRelations(ctx.relationList(), task);

        if (ctx.taskBody().descriptionClause() != null) {
            task.setDescription(unquote(ctx.taskBody().descriptionClause().STRING().getText()));
        }
        if (ctx.taskBody().preClause() != null) {
            task.setPreExpression(OclExpressionBuilder.build(ctx.taskBody().preClause().expression()));
        }
        if (ctx.taskBody().postClause() != null) {
            task.setPostExpression(OclExpressionBuilder.build(ctx.taskBody().postClause().expression()));
        }

        return task;
    }

    @Override
    public QualityCS visitQualityDecl(GOALParser.QualityDeclContext ctx) {
        QualityCS quality = new QualityCS(ctx.IDENT().getSymbol());
        attachRelations(ctx.relationList(), quality);
        applyDescription(ctx.elementBody(), quality);
        return quality;
    }

    @Override
    public ResourceCS visitResourceDecl(GOALParser.ResourceDeclContext ctx) {
        ResourceCS resource = new ResourceCS(ctx.IDENT().getSymbol());
        attachRelations(ctx.relationList(), resource);
        applyDescription(ctx.elementBody(), resource);
        return resource;
    }

    @Override
    public DependencyCS visitDependencyDefinition(GOALParser.DependencyDefinitionContext ctx) {
        DependencyCS dep = new DependencyCS(ctx.IDENT().getSymbol());
        dep.setDependerQualifiedName(qualifiedName(ctx.dependerClause().qualifiedName()));
        dep.setDependeeQualifiedName(qualifiedName(ctx.dependeeClause().qualifiedName()));
        dep.setDependumElement(visitIntentionalElement(ctx.dependumClause().intentionalElement()));
        return dep;
    }

    private void fillActor(ActorDeclCS actor, Token parent, Token instanceOf, GOALParser.ActorBodyContext body) {
        actor.setParentRef(parent);
        actor.setInstanceOfRef(instanceOf);
        for (GOALParser.IntentionalElementContext elementContext : body.intentionalElement()) {
            actor.addIntentionalElement((IntentionalElementCS) visit(elementContext));
        }
    }

    private static void attachRelations(GOALParser.RelationListContext list, IntentionalElementCS source) {
        if (list == null) {
            return;
        }
        for (GOALParser.RelationContext rel : list.relation()) {
            source.addOutgoingLink(new OutgoingLink(mapKind(rel.relOp().getStart().getText()), rel.IDENT().getSymbol()));
        }
    }

    private static OutgoingLink.Kind mapKind(String operatorText) {
        return switch (operatorText) {
            case "&>" -> OutgoingLink.Kind.REFINE_AND;
            case "|>" -> OutgoingLink.Kind.REFINE_OR;
            case "++>" -> OutgoingLink.Kind.CONTRIB_MAKE;
            case "+>" -> OutgoingLink.Kind.CONTRIB_HELP;
            case "->" -> OutgoingLink.Kind.CONTRIB_HURT;
            case "-->" -> OutgoingLink.Kind.CONTRIB_BREAK;
            case "=>" -> OutgoingLink.Kind.QUALIFY;
            case "neededBy" -> OutgoingLink.Kind.NEEDED_BY;
            default -> throw new IllegalStateException("Unknown relOp: " + operatorText);
        };
    }

    private static void applyDescription(GOALParser.ElementBodyContext body, org.vnu.sme.goal.ast.DescriptionContainerCS target) {
        if (body == null || body.descriptionClause().isEmpty()) {
            return;
        }
        target.setDescription(unquote(body.descriptionClause(0).STRING().getText()));
    }

    private static GoalCS.GoalType goalType(ParserRuleContext ctx) {
        if (ctx instanceof GOALParser.MaintainClauseContext) {
            return GoalCS.GoalType.MAINTAIN;
        }
        if (ctx instanceof GOALParser.AvoidClauseContext) {
            return GoalCS.GoalType.AVOID;
        }
        return GoalCS.GoalType.ACHIEVE;
    }

    private static OclExpressionCS expression(ParserRuleContext ctx) {
        if (ctx instanceof GOALParser.AchieveClauseContext) {
            return OclExpressionBuilder.build(((GOALParser.AchieveClauseContext) ctx).body);
        }
        if (ctx instanceof GOALParser.MaintainClauseContext) {
            return OclExpressionBuilder.build(((GOALParser.MaintainClauseContext) ctx).expression());
        }
        return OclExpressionBuilder.build(((GOALParser.AvoidClauseContext) ctx).expression());
    }

    private static String qualifiedName(GOALParser.QualifiedNameContext ctx) {
        return ctx.IDENT().stream().map(Object::toString).collect(Collectors.joining("."));
    }

    private static String unquote(String raw) {
        if (raw == null || raw.length() < 2) {
            return raw;
        }
        return raw.substring(1, raw.length() - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
