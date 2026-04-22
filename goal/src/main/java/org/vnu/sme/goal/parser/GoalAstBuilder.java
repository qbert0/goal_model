package org.vnu.sme.goal.parser;

import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.vnu.sme.goal.ast.ActorCS;
import org.vnu.sme.goal.ast.ActorDeclCS;
import org.vnu.sme.goal.ast.AgentCS;
import org.vnu.sme.goal.ast.DependencyCS;
import org.vnu.sme.goal.ast.GoalCS;
import org.vnu.sme.goal.ast.GoalModelCS;
import org.vnu.sme.goal.ast.IntentionalElementCS;
import org.vnu.sme.goal.ast.QualityCS;
import org.vnu.sme.goal.ast.ResourceCS;
import org.vnu.sme.goal.ast.RoleCS;
import org.vnu.sme.goal.ast.TaskCS;

public class GoalAstBuilder extends GOALBaseVisitor<Object> {

    @Override
    public GoalModelCS visitGoalModel(GOALParser.GoalModelContext ctx) {
        GoalModelCS model = new GoalModelCS(ctx.IDENT().getSymbol());

        for (GOALParser.ActorContext actorContext : ctx.actor()) {
            model.addActorDeclCS((ActorDeclCS) visit(actorContext));
        }

        for (GOALParser.DependencyDefinitionContext dependencyContext : ctx.dependencyDefinition()) {
            model.addDependencyDeclCS((DependencyCS) visit(dependencyContext));
        }

        return model;
    }

    @Override
    public ActorDeclCS visitActor(GOALParser.ActorContext ctx) {
        return (ActorDeclCS) visit(ctx.getChild(0));
    }

    @Override
    public ActorCS visitActorDefinition(GOALParser.ActorDefinitionContext ctx) {
        ActorCS actor = new ActorCS(ctx.IDENT(0).getSymbol());
        fillActor(actor, ctx.IDENT().size() > 1 ? ctx.IDENT(1).getSymbol() : null,
                ctx.IDENT().size() > 2 ? ctx.IDENT(2).getSymbol() : null, ctx.actorBody());
        return actor;
    }

    @Override
    public AgentCS visitAgentDefinition(GOALParser.AgentDefinitionContext ctx) {
        AgentCS agent = new AgentCS(ctx.IDENT(0).getSymbol());
        fillActor(agent, ctx.IDENT().size() > 1 ? ctx.IDENT(1).getSymbol() : null,
                ctx.IDENT().size() > 2 ? ctx.IDENT(2).getSymbol() : null, ctx.actorBody());
        return agent;
    }

    @Override
    public RoleCS visitRoleDefinition(GOALParser.RoleDefinitionContext ctx) {
        RoleCS role = new RoleCS(ctx.IDENT(0).getSymbol());
        fillActor(role, ctx.IDENT().size() > 1 ? ctx.IDENT(1).getSymbol() : null,
                ctx.IDENT().size() > 2 ? ctx.IDENT(2).getSymbol() : null, ctx.actorBody());
        return role;
    }

    @Override
    public IntentionalElementCS visitIntentionalElement(GOALParser.IntentionalElementContext ctx) {
        return (IntentionalElementCS) visit(ctx.getChild(0));
    }

    @Override
    public GoalCS visitGoalDecl(GOALParser.GoalDeclContext ctx) {
        GoalCS goal = new GoalCS(ctx.IDENT().getSymbol());
        fillElement(goal, ctx.relationList());

        if (ctx.goalBody().descriptionClause() != null) {
            goal.setDescription(unquote(ctx.goalBody().descriptionClause().STRING().getText()));
        }

        GOALParser.GoalClauseContext clause = ctx.goalBody().goalClause();
        if (clause != null) {
            ParserRuleContext child = (ParserRuleContext) clause.getChild(0);
            goal.setGoalType(goalType(child));
            goal.setOclExpression(expressionText(child));
        }

        return goal;
    }

    @Override
    public TaskCS visitTaskDecl(GOALParser.TaskDeclContext ctx) {
        TaskCS task = new TaskCS(ctx.IDENT().getSymbol());
        fillElement(task, ctx.relationList());

        if (ctx.taskBody().descriptionClause() != null) {
            task.setDescription(unquote(ctx.taskBody().descriptionClause().STRING().getText()));
        }
        if (ctx.taskBody().preClause() != null) {
            task.setPreExpression(ctx.taskBody().preClause().expression().getText());
        }
        if (ctx.taskBody().postClause() != null) {
            task.setPostExpression(ctx.taskBody().postClause().expression().getText());
        }

        return task;
    }

    @Override
    public QualityCS visitQualityDecl(GOALParser.QualityDeclContext ctx) {
        QualityCS quality = new QualityCS(ctx.IDENT().getSymbol());
        fillElement(quality, ctx.relationList());
        fillElementBody(quality, ctx.elementBody());
        return quality;
    }

    @Override
    public ResourceCS visitResourceDecl(GOALParser.ResourceDeclContext ctx) {
        ResourceCS resource = new ResourceCS(ctx.IDENT().getSymbol());
        fillElement(resource, ctx.relationList());
        fillElementBody(resource, ctx.elementBody());
        return resource;
    }

    @Override
    public DependencyCS visitDependencyDefinition(GOALParser.DependencyDefinitionContext ctx) {
        DependencyCS dependency = new DependencyCS(ctx.IDENT().getSymbol());
        dependency.setDependerRef(qualifiedName(ctx.dependerClause().qualifiedName()));
        dependency.setDependeeRef(qualifiedName(ctx.dependeeClause().qualifiedName()));
        dependency.setDependum((IntentionalElementCS) visit(ctx.dependumClause().intentionalElement()));
        return dependency;
    }

    private void fillActor(ActorDeclCS actor, Token parent, Token instanceOf, GOALParser.ActorBodyContext body) {
        actor.setParentRef(parent);
        actor.setInstanceOfRef(instanceOf);
        for (GOALParser.IntentionalElementContext elementContext : body.intentionalElement()) {
            actor.addIntentionalElement((IntentionalElementCS) visit(elementContext));
        }
    }

    private void fillElement(IntentionalElementCS element, GOALParser.RelationListContext relationList) {
        if (relationList == null) {
            return;
        }
        for (GOALParser.RelationContext relation : relationList.relation()) {
            element.addRelation(new IntentionalElementCS.RelationRef(
                    relation.relOp().getStart(),
                    relation.IDENT().getSymbol()));
        }
    }

    private void fillElementBody(IntentionalElementCS element, GOALParser.ElementBodyContext body) {
        if (body.descriptionClause().isEmpty()) {
            return;
        }
        element.setDescription(unquote(body.descriptionClause(0).STRING().getText()));
    }

    private GoalCS.GoalType goalType(ParserRuleContext ctx) {
        if (ctx instanceof GOALParser.MaintainClauseContext) {
            return GoalCS.GoalType.MAINTAIN;
        }
        if (ctx instanceof GOALParser.AvoidClauseContext) {
            return GoalCS.GoalType.AVOID;
        }
        return GoalCS.GoalType.ACHIEVE;
    }

    private String expressionText(ParserRuleContext ctx) {
        if (ctx instanceof GOALParser.AchieveClauseContext) {
            return ((GOALParser.AchieveClauseContext) ctx).expression().getText();
        }
        if (ctx instanceof GOALParser.MaintainClauseContext) {
            return ((GOALParser.MaintainClauseContext) ctx).expression().getText();
        }
        return ((GOALParser.AvoidClauseContext) ctx).expression().getText();
    }

    private String qualifiedName(GOALParser.QualifiedNameContext ctx) {
        return ctx.IDENT().stream().map(Object::toString).collect(Collectors.joining("."));
    }

    private String unquote(String text) {
        if (text == null || text.length() < 2) {
            return text;
        }
        return text.substring(1, text.length() - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
