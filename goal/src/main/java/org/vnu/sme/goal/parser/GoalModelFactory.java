package org.vnu.sme.goal.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.Token;
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
import org.vnu.sme.goal.mm.Achieve;
import org.vnu.sme.goal.mm.Actor;
import org.vnu.sme.goal.mm.Agent;
import org.vnu.sme.goal.mm.AndRefinement;
import org.vnu.sme.goal.mm.Avoid;
import org.vnu.sme.goal.mm.ConcreteIntentionalElement;
import org.vnu.sme.goal.mm.Contribution;
import org.vnu.sme.goal.mm.ContributionType;
import org.vnu.sme.goal.mm.Dependency;
import org.vnu.sme.goal.mm.Goal;
import org.vnu.sme.goal.mm.GoalModel;
import org.vnu.sme.goal.mm.GoalTaskElement;
import org.vnu.sme.goal.mm.IntentionalElement;
import org.vnu.sme.goal.mm.Maintain;
import org.vnu.sme.goal.mm.OrRefinement;
import org.vnu.sme.goal.mm.Post;
import org.vnu.sme.goal.mm.Pre;
import org.vnu.sme.goal.mm.Quality;
import org.vnu.sme.goal.mm.Refinement;
import org.vnu.sme.goal.mm.Resource;
import org.vnu.sme.goal.mm.Role;
import org.vnu.sme.goal.mm.Task;
import org.vnu.sme.goal.mm.ocl.Expression;

public class GoalModelFactory {
    private final Map<ActorDeclCS, Actor> actorMap = new HashMap<>();
    private final Map<IntentionalElementCS, IntentionalElement> elementMap = new HashMap<>();
    private final Map<String, Actor> actorsByName = new HashMap<>();
    private final Map<String, IntentionalElement> elementsByName = new HashMap<>();
    private final List<PendingRelation> pendingRelations = new ArrayList<>();

    public GoalModel create(GoalModelCS cs) {
        GoalModel model = new GoalModel(cs.getfName().getText());

        for (ActorDeclCS actorCS : cs.getActorDeclsCS()) {
            Actor actor = createActor(actorCS);
            actor.setDescription(actorCS.getDescription());
            actorMap.put(actorCS, actor);
            actorsByName.put(actor.getName(), actor);
        }

        for (ActorDeclCS actorCS : cs.getActorDeclsCS()) {
            Actor actor = actorMap.get(actorCS);
            actor.setIsAActor(resolveActor(actorCS.getParentRef()));
            actor.setParticipatesInActor(resolveActor(actorCS.getInstanceOfRef()));

            for (IntentionalElementCS elementCS : actorCS.getIntentionalElements()) {
                IntentionalElement element = createElement(elementCS);
                actor.addWantedElement(element);
                elementMap.put(elementCS, element);
                model.registerElement(actor, element);
                registerElement(actor, element);
            }

            model.addActor(actor);
        }

        for (ActorDeclCS actorCS : cs.getActorDeclsCS()) {
            for (IntentionalElementCS elementCS : actorCS.getIntentionalElements()) {
                collectPendingRelations(elementMap.get(elementCS), elementCS);
            }
        }

        materializePendingRelations();

        for (DependencyCS dependencyCS : cs.getDependencyDeclsCS()) {
            Dependency dependency = createDependency(dependencyCS, model);
            if (dependency != null) {
                model.addDependency(dependency);
            }
        }

        return model;
    }

    private Actor createActor(ActorDeclCS cs) {
        String name = cs.getfName().getText();
        if (cs instanceof AgentCS) {
            return new Agent(name);
        }
        if (cs instanceof RoleCS) {
            return new Role(name);
        }
        if (cs instanceof ActorCS) {
            return new Actor(name);
        }
        throw new IllegalArgumentException("Unsupported actor type: " + cs.getClass().getSimpleName());
    }

    private IntentionalElement createElement(IntentionalElementCS cs) {
        IntentionalElement element;
        String name = cs.getfName().getText();

        if (cs instanceof GoalCS) {
            GoalCS goalCS = (GoalCS) cs;
            Goal goal = new Goal(name);
            goal.setDescription(goalCS.getDescription());
            if (goalCS.getGoalType() != null) {
                switch (goalCS.getGoalType()) {
                    case ACHIEVE:
                        goal.setGoalClause(new Achieve());
                        break;
                    case MAINTAIN:
                        goal.setGoalClause(new Maintain());
                        break;
                    case AVOID:
                        goal.setGoalClause(new Avoid());
                        break;
                    default:
                        break;
                }
            }
            if (goalCS.getOclExpression() != null && goal.getGoalClause() != null) {
                goal.getGoalClause().addExpression(goalCS.getOclExpression());
            }
            element = goal;
        } else if (cs instanceof TaskCS) {
            TaskCS taskCS = (TaskCS) cs;
            Task task = new Task(name);
            task.setDescription(taskCS.getDescription());
            if (taskCS.getPreExpression() != null) {
                Pre pre = new Pre();
                pre.addExpression(taskCS.getPreExpression());
                task.setPre(pre);
            }
            if (taskCS.getPostExpression() != null) {
                Post post = new Post();
                post.addExpression(taskCS.getPostExpression());
                task.setPost(post);
            }
            element = task;
        } else if (cs instanceof QualityCS) {
            Quality quality = new Quality(name);
            quality.setDescription(cs.getDescription());
            element = quality;
        } else if (cs instanceof ResourceCS) {
            Resource resource = new Resource(name);
            resource.setDescription(cs.getDescription());
            element = resource;
        } else {
            throw new IllegalArgumentException("Unsupported intentional element type: " + cs.getClass().getSimpleName());
        }

        element.setDescription(cs.getDescription());
        return element;
    }

    private void collectPendingRelations(IntentionalElement source, IntentionalElementCS sourceCS) {
        for (OutgoingLink link : sourceCS.getOutgoingLinks()) {
            pendingRelations.add(new PendingRelation(source, link.target().getText(), operatorText(link.kind())));
        }
    }

    private void materializePendingRelations() {
        Map<String, Refinement> refinementGroups = new HashMap<>();

        for (PendingRelation pending : pendingRelations) {
            IntentionalElement target = resolveElement(pending.targetName);
            if (target == null) {
                throw new IllegalArgumentException("Unknown relation target: " + pending.targetName);
            }

            switch (pending.operator) {
                case "&>":
                case "|>":
                    if (!(pending.source instanceof GoalTaskElement) || !(target instanceof GoalTaskElement)) {
                        throw new IllegalArgumentException("Refinement requires Goal/Task elements: " + pending.source.getName() + " -> " + target.getName());
                    }
                    String key = pending.operator + "::" + target.getName();
                    Refinement refinement = refinementGroups.get(key);
                    if (refinement == null) {
                        refinement = "&>".equals(pending.operator)
                                ? new AndRefinement(target.getName() + "_and_refinement")
                                : new OrRefinement(target.getName() + "_or_refinement");
                        refinement.setParent((GoalTaskElement) target);
                        refinementGroups.put(key, refinement);
                    }
                    refinement.addChild((GoalTaskElement) pending.source);
                    break;
                case "=>":
                    if (!(pending.source instanceof Quality) || !(target instanceof ConcreteIntentionalElement)) {
                        throw new IllegalArgumentException("Qualification requires Quality -> ConcreteIntentionalElement: "
                                + pending.source.getName() + " -> " + target.getName());
                    }
                    ((Quality) pending.source).addQualifiedElement((ConcreteIntentionalElement) target);
                    break;
                case "<>":
                    if (!(pending.source instanceof Resource) || !(target instanceof Task)) {
                        throw new IllegalArgumentException("Needed-by requires Resource -> Task: "
                                + pending.source.getName() + " -> " + target.getName());
                    }
                    ((Resource) pending.source).addNeededByTask((Task) target);
                    break;
                default:
                    Contribution contribution = new Contribution(pending.source.getName() + "_contributes_" + target.getName());
                    contribution.setContributionType(contributionType(pending.operator));
                    pending.source.addOutgoingContribution(contribution);
                    target.addIncomingContribution(contribution);
                    break;
            }
        }
    }

    private ContributionType contributionType(String operator) {
        switch (operator) {
            case "++>":
                return ContributionType.MAKE;
            case "+>":
                return ContributionType.HELP;
            case "->":
                return ContributionType.HURT;
            case "-->":
                return ContributionType.BREAK;
            default:
                return ContributionType.UNKNOWN;
        }
    }

    private String operatorText(OutgoingLink.Kind kind) {
        return switch (kind) {
            case REFINE_AND -> "&>";
            case REFINE_OR -> "|>";
            case CONTRIB_MAKE -> "++>";
            case CONTRIB_HELP -> "+>";
            case CONTRIB_HURT -> "->";
            case CONTRIB_BREAK -> "-->";
            case QUALIFY -> "=>";
            case NEEDED_BY -> "<>";
        };
    }

    private Dependency createDependency(DependencyCS cs, GoalModel model) {
        Actor depender = resolveActor(cs.getDependerQualifiedName());
        Actor dependee = resolveActor(cs.getDependeeQualifiedName());
        IntentionalElement dependum = createElement(cs.getDependum());

        if (depender == null || dependee == null) {
            throw new IllegalArgumentException("Unknown dependency actor in " + cs.getfName().getText());
        }

        Dependency dependency = new Dependency(cs.getfName().getText());
        dependency.setDescription(cs.getDescription());
        dependency.setDepender(depender);
        dependency.setDependee(dependee);
        dependency.setDependerElement(resolveQualifiedElement(cs.getDependerQualifiedName()));
        dependency.setDependeeElement(resolveQualifiedElement(cs.getDependeeQualifiedName()));
        dependency.setDependumElement(dependum);
        model.registerElement(null, dependum);
        registerElement(null, dependum);
        return dependency;
    }

    private Actor resolveActor(Token token) {
        return token == null ? null : resolveActor(token.getText());
    }

    private Actor resolveActor(String name) {
        if (name == null) {
            return null;
        }
        int dot = name.indexOf('.');
        return actorsByName.get(dot >= 0 ? name.substring(0, dot) : name);
    }

    private IntentionalElement resolveQualifiedElement(String name) {
        return name == null || name.indexOf('.') < 0 ? null : elementsByName.get(name);
    }

    private IntentionalElement resolveElement(String name) {
        return elementsByName.get(name);
    }

    private void registerElement(Actor owner, IntentionalElement element) {
        if (element == null) {
            return;
        }
        if (owner != null) {
            elementsByName.put(owner.getName() + "." + element.getName(), element);
        }
        elementsByName.putIfAbsent(element.getName(), element);
    }

    private static final class PendingRelation {
        private final IntentionalElement source;
        private final String targetName;
        private final String operator;

        private PendingRelation(IntentionalElement source, String targetName, String operator) {
            this.source = source;
            this.targetName = targetName;
            this.operator = operator;
        }
    }
}
