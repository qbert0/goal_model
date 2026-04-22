package org.vnu.sme.goal.parser;

import java.util.HashMap;
import java.util.Map;

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
import org.vnu.sme.goal.mm.Actor;
import org.vnu.sme.goal.mm.Agent;
import org.vnu.sme.goal.mm.ContributionRelation;
import org.vnu.sme.goal.mm.Dependency;
import org.vnu.sme.goal.mm.Goal;
import org.vnu.sme.goal.mm.GoalModel;
import org.vnu.sme.goal.mm.IntentionalElement;
import org.vnu.sme.goal.mm.QualificationRelation;
import org.vnu.sme.goal.mm.Quality;
import org.vnu.sme.goal.mm.RefinementRelation;
import org.vnu.sme.goal.mm.Relation;
import org.vnu.sme.goal.mm.Resource;
import org.vnu.sme.goal.mm.Role;
import org.vnu.sme.goal.mm.SpecificActor;
import org.vnu.sme.goal.mm.Task;

public class GoalModelFactory {
    private final Map<ActorDeclCS, Actor> actorMap = new HashMap<>();
    private final Map<IntentionalElementCS, IntentionalElement> elementMap = new HashMap<>();
    private final Map<String, Actor> actorsByName = new HashMap<>();
    private final Map<String, IntentionalElement> elementsByName = new HashMap<>();

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
            actor.setParent(resolveActor(actorCS.getParentRef()));
            actor.setInstanceOf(resolveActor(actorCS.getInstanceOfRef()));

            for (IntentionalElementCS elementCS : actorCS.getIntentionalElements()) {
                IntentionalElement element = createElement(elementCS);
                element.setOwner(actor);
                actor.addElement(element);
                elementMap.put(elementCS, element);
                registerElement(actor, element);
            }

            model.addActor(actor);
        }

        for (ActorDeclCS actorCS : cs.getActorDeclsCS()) {
            for (IntentionalElementCS elementCS : actorCS.getIntentionalElements()) {
                createRelations(elementMap.get(elementCS), elementCS);
            }
        }

        for (DependencyCS dependencyCS : cs.getDependencyDeclsCS()) {
            Dependency dependency = createDependency(dependencyCS);
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
            return new SpecificActor(name);
        }
        throw new IllegalArgumentException("Unsupported actor type: " + cs.getClass().getSimpleName());
    }

    private IntentionalElement createElement(IntentionalElementCS cs) {
        String name = cs.getfName().getText();
        IntentionalElement element;

        if (cs instanceof GoalCS) {
            Goal goal = new Goal(name);
            GoalCS goalCS = (GoalCS) cs;
            if (goalCS.getGoalType() != null) {
                goal.setGoalType(Goal.GoalType.valueOf(goalCS.getGoalType().name()));
            }
            goal.setOclExpression(goalCS.getOclExpression());
            element = goal;
        } else if (cs instanceof TaskCS) {
            Task task = new Task(name);
            task.setPreExpression(((TaskCS) cs).getPreExpression());
            task.setPostExpression(((TaskCS) cs).getPostExpression());
            element = task;
        } else if (cs instanceof QualityCS) {
            element = new Quality(name);
        } else if (cs instanceof ResourceCS) {
            element = new Resource(name);
        } else {
            throw new IllegalArgumentException("Unsupported intentional element type: " + cs.getClass().getSimpleName());
        }

        element.setDescription(cs.getDescription());
        return element;
    }

    private void createRelations(IntentionalElement source, IntentionalElementCS sourceCS) {
        int index = 1;
        for (IntentionalElementCS.RelationRef relationRef : sourceCS.getRelations()) {
            IntentionalElement target = resolveElement(relationRef.getTargetRef().getText());
            if (target == null) {
                throw new IllegalArgumentException("Unknown relation target: " + relationRef.getTargetRef().getText());
            }

            Relation relation = createRelation(source.getName() + "_rel_" + index++, relationRef.getRelOp().getText());
            relation.setSource(source);
            relation.setTarget(target);
            source.addOutgoingRelation(relation);
            target.addIncomingRelation(relation);
        }
    }

    private Relation createRelation(String name, String operator) {
        switch (operator) {
            case "&>":
            case "|>":
                RefinementRelation refinement = new RefinementRelation(name);
                refinement.setRefinementType("&>".equals(operator)
                        ? RefinementRelation.RefinementType.AND
                        : RefinementRelation.RefinementType.OR);
                return refinement;
            case "++>":
            case "+>":
            case "->":
            case "-->":
            case "<>":
                ContributionRelation contribution = new ContributionRelation(name);
                contribution.setContributionType(contributionType(operator));
                return contribution;
            case "=>":
                return new QualificationRelation(name);
            default:
                throw new IllegalArgumentException("Unsupported relation operator: " + operator);
        }
    }

    private ContributionRelation.ContributionType contributionType(String operator) {
        switch (operator) {
            case "++>":
                return ContributionRelation.ContributionType.MAKE;
            case "+>":
                return ContributionRelation.ContributionType.HELP;
            case "->":
                return ContributionRelation.ContributionType.HURT;
            case "-->":
                return ContributionRelation.ContributionType.BREAK;
            case "<>":
                return ContributionRelation.ContributionType.UNKNOWN;
            default:
                return ContributionRelation.ContributionType.UNKNOWN;
        }
    }

    private Dependency createDependency(DependencyCS cs) {
        Actor depender = resolveActor(cs.getDependerRef());
        Actor dependee = resolveActor(cs.getDependeeRef());
        IntentionalElement dependum = createElement(cs.getDependum());

        if (depender == null || dependee == null) {
            throw new IllegalArgumentException("Unknown dependency actor in " + cs.getfName().getText());
        }

        dependum.setOwner(dependee);
        dependee.addElement(dependum);
        registerElement(dependee, dependum);

        Dependency dependency = new Dependency(cs.getfName().getText());
        dependency.setDescription(cs.getDescription());
        dependency.setDepender(depender);
        dependency.setDependee(dependee);
        dependency.setDependum(dependum);
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

    private IntentionalElement resolveElement(String name) {
        return elementsByName.get(name);
    }

    private void registerElement(Actor owner, IntentionalElement element) {
        elementsByName.put(owner.getName() + "." + element.getName(), element);
        elementsByName.putIfAbsent(element.getName(), element);
    }
}
