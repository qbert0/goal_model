package org.vnu.sme.goal.parser.semantic.symbols;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import org.vnu.sme.goal.ast.RelationCS;
import org.vnu.sme.goal.ast.ResourceCS;
import org.vnu.sme.goal.ast.RoleCS;
import org.vnu.sme.goal.ast.TaskCS;
import org.vnu.sme.goal.parser.semantic.enums.ActorKind;
import org.vnu.sme.goal.parser.semantic.enums.ElementKind;

/**
 * Symbol table construction in two passes.
 * <p>
 * Pass 1: collect declarations and create symbols.
 * Pass 2: resolve cross references and compute leaf flags.
 */
public final class GoalSymbolTableBuilder {
    private final List<SemanticIssue> issues = new ArrayList<>();

    public GoalSymbolTable build(GoalModelCS ast) {
        GoalSymbolTable table = new GoalSymbolTable(ast.getfName().getText());
        runDeclarationPass(ast, table);
        runResolutionPass(ast, table);
        return table;
    }

    public List<SemanticIssue> getIssues() {
        return issues;
    }

    public void runDeclarationPass(GoalModelCS ast, GoalSymbolTable table) {
        for (ActorDeclCS actorDecl : ast.getActorDeclsCS()) {
            registerActor(actorDecl, table);
        }
        for (RelationCS relationDecl : ast.getRelationDeclsCS()) {
            if (relationDecl instanceof DependencyCS dep) {
                registerDependency(dep, table);
            }
        }
    }

    public void runResolutionPass(GoalModelCS ast, GoalSymbolTable table) {
        resolveRelationTargets(table);
        resolveDependencyReferences(table);
        recomputeLeafFlags(table);
    }

    public void reportDuplicateDeclaration(String name, int line, int column) {
        issues.add(new SemanticIssue("S5", "Duplicate declaration: " + name, line, column));
    }

    public void reportUndeclaredReference(String ref, int line, int column) {
        issues.add(new SemanticIssue("S1", "Undeclared reference: " + ref, line, column));
    }

    private void registerActor(ActorDeclCS actorDecl, GoalSymbolTable table) {
        String actorName = actorDecl.getfName().getText();
        if (table.getActorsByName().containsKey(actorName)) {
            reportDuplicateDeclaration(actorName, actorDecl.getfName().getLine(), actorDecl.getfName().getCharPositionInLine());
            return;
        }

        ActorSymbol actorSymbol = new ActorSymbol(actorName, mapActorKind(actorDecl), actorDecl.getfName());
        table.getActorsByName().put(actorName, actorSymbol);

        for (IntentionalElementCS elementDecl : actorDecl.getIntentionalElements()) {
            registerElement(actorSymbol, elementDecl, table);
        }
    }

    private void registerElement(ActorSymbol actorSymbol, IntentionalElementCS elementDecl, GoalSymbolTable table) {
        String elementName = elementDecl.getfName().getText();
        if (actorSymbol.getElementTable().containsKey(elementName)) {
            reportDuplicateDeclaration(
                    actorSymbol.getName() + "." + elementName,
                    elementDecl.getfName().getLine(),
                    elementDecl.getfName().getCharPositionInLine());
            return;
        }

        ElementSymbol elementSymbol =
                new ElementSymbol(elementName, mapElementKind(elementDecl), actorSymbol, elementDecl.getfName());
        actorSymbol.getElementTable().put(elementName, elementSymbol);
        table.getElementsByQualifiedName().put(elementSymbol.getQualifiedName(), elementSymbol);

        for (OutgoingLink link : elementDecl.getOutgoingLinks()) {
            elementSymbol.getRelations().add(new RelationEntry(link.kind(), link.target()));
        }
    }

    private void registerDependency(DependencyCS depDecl, GoalSymbolTable table) {
        String depName = depDecl.getfName().getText();
        if (table.getDependenciesByName().containsKey(depName)) {
            reportDuplicateDeclaration(depName, depDecl.getfName().getLine(), depDecl.getfName().getCharPositionInLine());
            return;
        }

        DependencySymbol dependencySymbol = new DependencySymbol(
                depName,
                depDecl.getfName(),
                depDecl.getDependerQualifiedName(),
                depDecl.getDependeeQualifiedName());

        IntentionalElementCS dependumDecl = depDecl.getDependumElement();
        if (dependumDecl != null) {
            ActorSymbol virtualOwner = new ActorSymbol("__dependency__" + depName, ActorKind.ACTOR, depDecl.getfName());
            ElementSymbol dependum = new ElementSymbol(
                    dependumDecl.getfName().getText(),
                    mapElementKind(dependumDecl),
                    virtualOwner,
                    dependumDecl.getfName());
            dependencySymbol.setDependum(dependum);
        }

        table.getDependenciesByName().put(depName, dependencySymbol);
    }

    private void resolveRelationTargets(GoalSymbolTable table) {
        for (ActorSymbol actor : table.getActorsByName().values()) {
            for (ElementSymbol source : actor.getElementTable().values()) {
                for (RelationEntry relation : source.getRelations()) {
                    Token targetToken = relation.getTargetRef();
                    String targetText = targetToken.getText();

                    Optional<ElementSymbol> resolved = resolveElementReference(source.getOwnerActor(), targetText, table);
                    if (resolved.isPresent()) {
                        relation.setResolvedTarget(resolved.get());
                    } else {
                        reportUndeclaredReference(targetText, targetToken.getLine(), targetToken.getCharPositionInLine());
                    }
                }
            }
        }
    }

    private void resolveDependencyReferences(GoalSymbolTable table) {
        for (DependencySymbol dep : table.getDependenciesByName().values()) {
            Optional<ElementSymbol> depender = table.resolveElement(dep.getDependerRawRef());
            if (depender.isPresent()) {
                dep.setDepender(depender.get());
            } else {
                reportUndeclaredReference(dep.getDependerRawRef(), dep.getDeclarationToken().getLine(), dep.getDeclarationToken().getCharPositionInLine());
            }

            Optional<ElementSymbol> dependee = table.resolveElement(dep.getDependeeRawRef());
            if (dependee.isPresent()) {
                dep.setDependee(dependee.get());
            } else {
                reportUndeclaredReference(dep.getDependeeRawRef(), dep.getDeclarationToken().getLine(), dep.getDeclarationToken().getCharPositionInLine());
            }
        }
    }

    private void recomputeLeafFlags(GoalSymbolTable table) {
        for (ElementSymbol symbol : table.getElementsByQualifiedName().values()) {
            symbol.setLeaf(true);
        }
        for (ElementSymbol source : table.getElementsByQualifiedName().values()) {
            for (RelationEntry relation : source.getRelations()) {
                if (isRefinement(relation.getOperator()) && relation.getResolvedTarget() != null) {
                    relation.getResolvedTarget().setLeaf(false);
                }
            }
        }
    }

    private Optional<ElementSymbol> resolveElementReference(
            ActorSymbol ownerActor,
            String targetText,
            GoalSymbolTable table) {
        if (targetText.contains(".")) {
            return table.resolveElement(targetText);
        }
        ElementSymbol local = ownerActor.getElementTable().get(targetText);
        return Optional.ofNullable(local);
    }

    private static boolean isRefinement(OutgoingLink.Kind kind) {
        return kind == OutgoingLink.Kind.REFINE_AND || kind == OutgoingLink.Kind.REFINE_OR;
    }

    private static ActorKind mapActorKind(ActorDeclCS actorDecl) {
        if (actorDecl instanceof ActorCS) {
            return ActorKind.ACTOR;
        }
        if (actorDecl instanceof AgentCS) {
            return ActorKind.AGENT;
        }
        if (actorDecl instanceof RoleCS) {
            return ActorKind.ROLE;
        }
        throw new IllegalStateException("Unknown actor kind: " + actorDecl.getClass().getName());
    }

    private static ElementKind mapElementKind(IntentionalElementCS elementDecl) {
        if (elementDecl instanceof GoalCS) {
            return ElementKind.GOAL;
        }
        if (elementDecl instanceof TaskCS) {
            return ElementKind.TASK;
        }
        if (elementDecl instanceof QualityCS) {
            return ElementKind.QUALITY;
        }
        if (elementDecl instanceof ResourceCS) {
            return ElementKind.RESOURCE;
        }
        throw new IllegalStateException("Unknown element kind: " + elementDecl.getClass().getName());
    }
}

