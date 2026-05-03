package org.vnu.sme.goal.parser;

import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
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
        fillActor(actor, extractRefAfterToken(ctx, GOALParser.COLON), extractRefAfterToken(ctx, GOALParser.GT), ctx.actorBody());
        return actor;
    }

    @Override
    public AgentCS visitAgentDefinition(GOALParser.AgentDefinitionContext ctx) {
        AgentCS agent = new AgentCS(ctx.IDENT(0).getSymbol());
        fillActor(agent, extractRefAfterToken(ctx, GOALParser.COLON), extractRefAfterToken(ctx, GOALParser.GT), ctx.actorBody());
        return agent;
    }

    @Override
    public RoleCS visitRoleDefinition(GOALParser.RoleDefinitionContext ctx) {
        RoleCS role = new RoleCS(ctx.IDENT(0).getSymbol());
        fillActor(role, extractRefAfterToken(ctx, GOALParser.COLON), extractRefAfterToken(ctx, GOALParser.GT), ctx.actorBody());
        return role;
    }

    /**
     * Extract the IDENT token immediately following a marker token (e.g. COLON or GT)
     * inside actor/agent/role definition contexts.
     */
    private Token extractRefAfterToken(ParserRuleContext ctx, int markerTokenType) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (!(child instanceof TerminalNode marker)) {
                continue;
            }
            if (marker.getSymbol().getType() != markerTokenType) {
                continue;
            }
            for (int j = i + 1; j < ctx.getChildCount(); j++) {
                ParseTree next = ctx.getChild(j);
                if (next instanceof TerminalNode terminal
                        && terminal.getSymbol().getType() == GOALParser.IDENT) {
                    return terminal.getSymbol();
                }
            }
            return null;
        }
        return null;
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
            if (child instanceof GOALParser.AchieveClauseContext achieveClause && achieveClause.FOR() != null) {
                populateGoalContract(goal, child);
            } else {
                goal.setGoalType(goalType(child));
                goal.setOclExpression(expression(child));
            }
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
            element.addOutgoingLink(new OutgoingLink(
                    mapKind(relation.relOp().getStart().getText()),
                    relation.IDENT().getSymbol()));
        }
    }

    private void fillElementBody(IntentionalElementCS element, GOALParser.ElementBodyContext body) {
        if (body.descriptionClause().isEmpty()) {
            return;
        }
        element.setDescription(unquote(body.descriptionClause(0).STRING().getText()));
    }

    /**
     * Map an {@code achieveClause / maintainClause / avoidClause} parse-tree node onto the
     * {@link GoalCS} contract slots. Handles both achieve forms:
     *
     * <pre>
     * Form 1: achieve|maintain|avoid : &lt;expression&gt;
     *           → goalType ∈ {ACHIEVE, MAINTAIN, AVOID}, oclExpression = body
     *
     * Form 2: achieve for unique (s: T, ...) in &lt;sourceExpr&gt; : &lt;bodyExpr&gt;
     *           → goalType = ACHIEVE_UNIQUE,
     *             iterVars   = parsed typedVarList,
     *             sourceExpression = sourceExpr,
     *             oclExpression    = bodyExpr
     * </pre>
     */
    private void populateGoalContract(GoalCS goal, ParserRuleContext clause) {
        if (clause instanceof GOALParser.AchieveClauseContext achieveClause) {
            goal.setClauseToken(achieveClause.ACHIEVE().getSymbol());
            goal.setGoalType(GoalCS.GoalType.ACHIEVE_UNIQUE);
            goal.setIterVars(OclExpressionBuilder.buildTypedVarList(achieveClause.typedVarList()));
            goal.setSourceExpression(OclExpressionBuilder.build(achieveClause.expression(0)));
            goal.setOclExpression(OclExpressionBuilder.build(achieveClause.expression(1)));
            return;
        }
        if (clause instanceof GOALParser.MaintainClauseContext maintainClause) {
            goal.setGoalType(GoalCS.GoalType.MAINTAIN);
            goal.setClauseToken(maintainClause.MAINTAIN().getSymbol());
            goal.setOclExpression(OclExpressionBuilder.build(maintainClause.expression()));
            return;
        }
        GOALParser.AvoidClauseContext avoidClause = (GOALParser.AvoidClauseContext) clause;
        goal.setGoalType(GoalCS.GoalType.AVOID);
        goal.setClauseToken(avoidClause.AVOID().getSymbol());
        goal.setOclExpression(OclExpressionBuilder.build(avoidClause.expression()));
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

    private org.vnu.sme.goal.mm.ocl.Expression expression(ParserRuleContext ctx) {
        if (ctx instanceof GOALParser.AchieveClauseContext achieveClause) {
            return OclExpressionBuilder.build(achieveClause.expression(0));
        }
        if (ctx instanceof GOALParser.MaintainClauseContext) {
            return OclExpressionBuilder.build(((GOALParser.MaintainClauseContext) ctx).expression());
        }
        return OclExpressionBuilder.build(((GOALParser.AvoidClauseContext) ctx).expression());
    }

    private String qualifiedName(GOALParser.QualifiedNameContext ctx) {
        return ctx.IDENT().stream().map(Object::toString).collect(Collectors.joining("."));
    }

    private OutgoingLink.Kind mapKind(String operatorText) {
        return switch (operatorText) {
            case "&>" -> OutgoingLink.Kind.REFINE_AND;
            case "|>" -> OutgoingLink.Kind.REFINE_OR;
            case "++>" -> OutgoingLink.Kind.CONTRIB_MAKE;
            case "+>" -> OutgoingLink.Kind.CONTRIB_HELP;
            case "->" -> OutgoingLink.Kind.CONTRIB_HURT;
            case "-->" -> OutgoingLink.Kind.CONTRIB_BREAK;
            case "=>" -> OutgoingLink.Kind.QUALIFY;
            case "<>" -> OutgoingLink.Kind.NEEDED_BY;
            default -> throw new IllegalArgumentException("Unknown relation operator: " + operatorText);
        };
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
