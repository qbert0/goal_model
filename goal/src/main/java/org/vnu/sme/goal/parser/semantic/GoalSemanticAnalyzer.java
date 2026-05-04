package org.vnu.sme.goal.parser.semantic;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.antlr.v4.runtime.Token;
import org.vnu.sme.goal.ast.ActorDeclCS;
import org.vnu.sme.goal.ast.GoalModelCS;
import org.vnu.sme.goal.ast.OutgoingLink;
import org.vnu.sme.goal.parser.semantic.enums.ActorKind;
import org.vnu.sme.goal.parser.semantic.enums.ElementKind;
import org.vnu.sme.goal.parser.semantic.symbols.ActorSymbol;
import org.vnu.sme.goal.parser.semantic.symbols.DependencySymbol;
import org.vnu.sme.goal.parser.semantic.symbols.ElementSymbol;
import org.vnu.sme.goal.parser.semantic.symbols.GoalSymbolTable;
import org.vnu.sme.goal.parser.semantic.symbols.GoalSymbolTableBuilder;
import org.vnu.sme.goal.parser.semantic.symbols.RelationEntry;
import org.vnu.sme.goal.parser.semantic.symbols.SemanticIssue;

/**
 * Semantic checks for GOAL core V1 (without OCL).
 */
public final class GoalSemanticAnalyzer {
    private static final Map<ElementKind, Map<OutgoingLink.Kind, EnumSet<ElementKind>>> OPERATOR_MATRIX =
            createOperatorMatrix();

    /**
     * Standalone entry point: builds its own symbol table then runs all checks.
     * <p>
     * NOT called by {@link org.vnu.sme.goal.parser.semantic.pipeline.GoalSemanticPipelineSkeleton},
     * which shares a single builder across passes and calls each {@code validate*()} directly.
     * Use this method for unit tests or standalone invocations only.
     *
     * @deprecated Use {@link org.vnu.sme.goal.parser.semantic.pipeline.GoalSemanticPipelineSkeleton}
     *             in the compiler pipeline; keep this only for standalone/test invocations.
     */
    @Deprecated
    public List<SemanticIssue> analyze(GoalModelCS ast) {
        GoalSymbolTableBuilder builder = new GoalSymbolTableBuilder();
        GoalSymbolTable table = builder.build(ast);

        List<SemanticIssue> issues = new ArrayList<>(builder.getIssues());
        issues.addAll(traverseActorReferenceTree(ast, table));       // S1, S4
        RelationTraversalContext relationContext = traverseElementRelationTree(table); // S2, S6, S7, S8 (+ collect S9, S10)
        issues.addAll(relationContext.issues);
        issues.addAll(traverseRefinementTargetMap(relationContext)); // S9, S10
        issues.addAll(traverseDependencyTree(table));                // S3
        return issues;
    }

    /**
     * Traversal: Actor declaration references.
     * Handles: S1 (undeclared actor ref), S4 (invalid actor relationship).
     */
    public List<SemanticIssue> traverseActorReferenceTree(GoalModelCS ast, GoalSymbolTable table) {
        List<SemanticIssue> issues = new ArrayList<>();
        for (ActorDeclCS actorDecl : ast.getActorDeclsCS()) {
            ActorSymbol source = table.getActorsByName().get(actorDecl.getfName().getText());
            if (source == null) {
                continue;
            }

            for (Token isARef : actorDecl.getIsARefs()) {
                validateActorRefConstraint(
                        source,
                        isARef,
                        table,
                        issues,
                        "is-a",
                        true);
            }

            for (Token participatesInRef : actorDecl.getParticipatesInRefs()) {
                validateActorRefConstraint(
                        source,
                        participatesInRef,
                        table,
                        issues,
                        "participates-in",
                        false);
            }
        }
        return issues;
    }

    /**
     * Traversal: Element relation tree (actor -> element -> relation) in one pass.
     * Handles: S2 (operator matrix), S6 (self reference), S7 (qualify source), S8 (needed-by source).
     * Collects refinement graph and incoming refinement metadata for S9 and S10 post checks.
     */
    public RelationTraversalContext traverseElementRelationTree(GoalSymbolTable table) {
        RelationTraversalContext context = new RelationTraversalContext();
        for (ActorSymbol actor : table.getActorsByName().values()) {
            for (ElementSymbol source : actor.getElementTable().values()) {
                context.refinementGraph.computeIfAbsent(source, key -> new ArrayList<>());
                for (RelationEntry relation : source.getRelations()) {
                    ElementSymbol target = relation.getResolvedTarget();
                    if (target == null) {
                        continue;
                    }

                    OutgoingLink.Kind operator = relation.getOperator();
                    Token errorToken = relation.getTargetRef();

                    // S6
                    if (source == target) {
                        context.issues.add(new SemanticIssue(
                                "S6",
                                "Self reference is not allowed: " + source.getQualifiedName(),
                                errorToken.getLine(),
                                errorToken.getCharPositionInLine()));
                    }

                    // S7
                    if (operator == OutgoingLink.Kind.QUALIFY && source.getKind() != ElementKind.QUALITY) {
                        context.issues.add(new SemanticIssue(
                                "S7",
                                "Qualify source must be QUALITY: " + source.getQualifiedName(),
                                errorToken.getLine(),
                                errorToken.getCharPositionInLine()));
                    }

                    // S8
                    if (operator == OutgoingLink.Kind.NEEDED_BY && source.getKind() != ElementKind.RESOURCE) {
                        context.issues.add(new SemanticIssue(
                                "S8",
                                "NeededBy source must be RESOURCE: " + source.getQualifiedName(),
                                errorToken.getLine(),
                                errorToken.getCharPositionInLine()));
                    }

                    // S2
                    // When source kind is already wrong for => or neededBy, S7/S8 handle those cases.
                    // But if source kind is correct and only target is wrong, S2 must still fire.
                    if (!((operator == OutgoingLink.Kind.QUALIFY && source.getKind() != ElementKind.QUALITY)
                            || (operator == OutgoingLink.Kind.NEEDED_BY && source.getKind() != ElementKind.RESOURCE))
                            && !isMatrixAllowed(source.getKind(), operator, target.getKind())) {
                        context.issues.add(new SemanticIssue(
                                "S2",
                                "Invalid operator matrix: " + source.getKind() + " "
                                        + toSymbol(operator) + " " + target.getKind()
                                        + " (" + source.getQualifiedName() + " -> " + target.getQualifiedName() + ")",
                                errorToken.getLine(),
                                errorToken.getCharPositionInLine()));
                    }

                    // Collect for S9 and S10.
                    if (isRefinement(operator)) {
                        context.refinementGraph.computeIfAbsent(target, key -> new ArrayList<>());
                        context.refinementGraph.get(source).add(new RefinementEdge(target, errorToken));
                        context.incomingRefinementsByTarget
                                .computeIfAbsent(target, key -> new ArrayList<>())
                                .add(new IncomingRefinement(operator, errorToken));
                    }
                }
            }
        }
        return context;
    }

    /**
     * Traversal: Derived refinement structures.
     * Handles: S9 (circular refinement), S10 (mixed refinement type).
     */
    public List<SemanticIssue> traverseRefinementTargetMap(RelationTraversalContext context) {
        List<SemanticIssue> issues = new ArrayList<>();
        issues.addAll(validateMixedRefinementType(context.incomingRefinementsByTarget));
        issues.addAll(validateCircularRefinement(context.refinementGraph));
        return issues;
    }

    /**
     * Traversal: Dependency declarations.
     * Handles: S3 (dependency on non-leaf element).
     */
    public List<SemanticIssue> traverseDependencyTree(GoalSymbolTable table) {
        List<SemanticIssue> issues = new ArrayList<>();
        for (DependencySymbol dep : table.getDependenciesByName().values()) {
            if (dep.getDepender() != null && !dep.getDepender().isLeaf()) {
                issues.add(new SemanticIssue(
                        "S3",
                        "Dependency depender must be leaf: " + dep.getDepender().getQualifiedName(),
                        dep.getDeclarationToken().getLine(),
                        dep.getDeclarationToken().getCharPositionInLine()));
            }
            if (dep.getDependee() != null && !dep.getDependee().isLeaf()) {
                issues.add(new SemanticIssue(
                        "S3",
                        "Dependency dependee must be leaf: " + dep.getDependee().getQualifiedName(),
                        dep.getDeclarationToken().getLine(),
                        dep.getDeclarationToken().getCharPositionInLine()));
            }
        }
        return issues;
    }

    private void validateActorRefConstraint(
            ActorSymbol source,
            Token refToken,
            GoalSymbolTable table,
            List<SemanticIssue> issues,
            String relationLabel,
            boolean isA) {
        ActorSymbol target = table.getActorsByName().get(refToken.getText());
        if (target == null) {
            issues.add(new SemanticIssue(
                    "S1",
                    "Undeclared actor reference: " + refToken.getText(),
                    refToken.getLine(),
                    refToken.getCharPositionInLine()));
            return;
        }

        boolean allowed = isA
                ? isAAllowed(source.getKind(), target.getKind())
                : participatesInAllowed(source.getKind(), target.getKind());
        if (!allowed) {
            issues.add(new SemanticIssue(
                    "S4",
                    "Invalid actor relationship (" + relationLabel + "): "
                            + source.getKind() + " -> " + target.getKind()
                            + " (" + source.getName() + " -> " + target.getName() + ")",
                    refToken.getLine(),
                    refToken.getCharPositionInLine()));
        }
    }

    private static boolean isAAllowed(ActorKind source, ActorKind target) {
        if (target == null) {
            return true;
        }
        return (source == ActorKind.ACTOR && target == ActorKind.ACTOR)
                || (source == ActorKind.ROLE && target == ActorKind.ROLE);
    }

    private static boolean participatesInAllowed(ActorKind source, ActorKind target) {
        if (target == null) {
            return true;
        }
        return source == ActorKind.AGENT && (target == ActorKind.ROLE || target == ActorKind.AGENT);
    }

    private static boolean isMatrixAllowed(ElementKind source, OutgoingLink.Kind operator, ElementKind target) {
        Map<OutgoingLink.Kind, EnumSet<ElementKind>> opMap = OPERATOR_MATRIX.get(source);
        if (opMap == null) {
            return false;
        }
        EnumSet<ElementKind> allowedTargets = opMap.get(operator);
        return allowedTargets != null && allowedTargets.contains(target);
    }

    private static String toSymbol(OutgoingLink.Kind operator) {
        return switch (operator) {
            case REFINE_AND -> "&>";
            case REFINE_OR -> "|>";
            case CONTRIB_MAKE -> "++>";
            case CONTRIB_HELP -> "+>";
            case CONTRIB_HURT -> "->";
            case CONTRIB_BREAK -> "-->";
            case QUALIFY -> "=>";
            case NEEDED_BY -> "neededBy";
        };
    }

    private static boolean isRefinement(OutgoingLink.Kind operator) {
        return operator == OutgoingLink.Kind.REFINE_AND || operator == OutgoingLink.Kind.REFINE_OR;
    }

    private static List<SemanticIssue> validateMixedRefinementType(
            Map<ElementSymbol, List<IncomingRefinement>> incomingRefinementsByTarget) {
        List<SemanticIssue> issues = new ArrayList<>();
        for (Map.Entry<ElementSymbol, List<IncomingRefinement>> entry : incomingRefinementsByTarget.entrySet()) {
            ElementSymbol target = entry.getKey();
            List<IncomingRefinement> incomingRefinements = entry.getValue();
            if (incomingRefinements.isEmpty()) {
                continue;
            }
            OutgoingLink.Kind firstKind = incomingRefinements.get(0).kind();
            for (int i = 1; i < incomingRefinements.size(); i++) {
                IncomingRefinement incoming = incomingRefinements.get(i);
                if (incoming.kind() == firstKind) {
                    continue;
                }
                Token errorToken = incoming.targetToken();
                issues.add(new SemanticIssue(
                        "S10",
                        "Mixed refinement type on target " + target.getQualifiedName()
                                + ": existing " + toSymbol(firstKind)
                                + ", found " + toSymbol(incoming.kind()),
                        errorToken.getLine(),
                        errorToken.getCharPositionInLine()));
            }
        }
        return issues;
    }

    private static List<SemanticIssue> validateCircularRefinement(
            Map<ElementSymbol, List<RefinementEdge>> refinementGraph) {
        List<SemanticIssue> issues = new ArrayList<>();
        Map<ElementSymbol, VisitState> states = new HashMap<>();
        Set<String> emittedCycleEdges = new HashSet<>();
        for (ElementSymbol node : refinementGraph.keySet()) {
            if (states.getOrDefault(node, VisitState.UNVISITED) == VisitState.UNVISITED) {
                detectCircularRefinementDfs(node, refinementGraph, states, emittedCycleEdges, issues);
            }
        }
        return issues;
    }

    private static void detectCircularRefinementDfs(
            ElementSymbol node,
            Map<ElementSymbol, List<RefinementEdge>> graph,
            Map<ElementSymbol, VisitState> states,
            Set<String> emittedCycleEdges,
            List<SemanticIssue> issues) {
        states.put(node, VisitState.VISITING);

        for (RefinementEdge edge : graph.getOrDefault(node, List.of())) {
            ElementSymbol next = edge.target();
            VisitState nextState = states.getOrDefault(next, VisitState.UNVISITED);
            if (nextState == VisitState.UNVISITED) {
                detectCircularRefinementDfs(next, graph, states, emittedCycleEdges, issues);
                continue;
            }
            if (nextState == VisitState.VISITING) {
                String cycleKey = node.getQualifiedName() + "->" + next.getQualifiedName();
                if (emittedCycleEdges.add(cycleKey)) {
                    Token errorToken = edge.targetToken();
                    issues.add(new SemanticIssue(
                            "S9",
                            "Circular refinement detected: "
                                    + next.getQualifiedName() + " ... -> " + node.getQualifiedName()
                                    + " -> " + next.getQualifiedName(),
                            errorToken.getLine(),
                            errorToken.getCharPositionInLine()));
                }
            }
        }

        states.put(node, VisitState.VISITED);
    }

    private enum VisitState {
        UNVISITED,
        VISITING,
        VISITED
    }

    public static final class RelationTraversalContext {
        private final List<SemanticIssue> issues = new ArrayList<>();
        private final Map<ElementSymbol, List<RefinementEdge>> refinementGraph = new HashMap<>();
        private final Map<ElementSymbol, List<IncomingRefinement>> incomingRefinementsByTarget = new HashMap<>();

        public List<SemanticIssue> getIssues() {
            return issues;
        }

        public Map<ElementSymbol, List<RefinementEdge>> getRefinementGraph() {
            return refinementGraph;
        }

        public Map<ElementSymbol, List<IncomingRefinement>> getIncomingRefinementsByTarget() {
            return incomingRefinementsByTarget;
        }
    }

    private record RefinementEdge(ElementSymbol target, Token targetToken) {
    }

    private record IncomingRefinement(OutgoingLink.Kind kind, Token targetToken) {
    }

    private static Map<ElementKind, Map<OutgoingLink.Kind, EnumSet<ElementKind>>> createOperatorMatrix() {
        Map<ElementKind, Map<OutgoingLink.Kind, EnumSet<ElementKind>>> matrix = new EnumMap<>(ElementKind.class);

        matrix.put(ElementKind.GOAL, opMap(
                allow(OutgoingLink.Kind.REFINE_AND, ElementKind.GOAL, ElementKind.TASK),
                allow(OutgoingLink.Kind.REFINE_OR, ElementKind.GOAL, ElementKind.TASK),
                allow(OutgoingLink.Kind.CONTRIB_MAKE, ElementKind.QUALITY),
                allow(OutgoingLink.Kind.CONTRIB_HELP, ElementKind.QUALITY),
                allow(OutgoingLink.Kind.CONTRIB_HURT, ElementKind.QUALITY),
                allow(OutgoingLink.Kind.CONTRIB_BREAK, ElementKind.QUALITY)));

        matrix.put(ElementKind.TASK, opMap(
                allow(OutgoingLink.Kind.REFINE_AND, ElementKind.GOAL, ElementKind.TASK),
                allow(OutgoingLink.Kind.REFINE_OR, ElementKind.GOAL, ElementKind.TASK),
                allow(OutgoingLink.Kind.CONTRIB_MAKE, ElementKind.QUALITY),
                allow(OutgoingLink.Kind.CONTRIB_HELP, ElementKind.QUALITY),
                allow(OutgoingLink.Kind.CONTRIB_HURT, ElementKind.QUALITY),
                allow(OutgoingLink.Kind.CONTRIB_BREAK, ElementKind.QUALITY)));

        matrix.put(ElementKind.QUALITY, opMap(
                allow(OutgoingLink.Kind.QUALIFY, ElementKind.GOAL, ElementKind.TASK, ElementKind.RESOURCE),
                allow(OutgoingLink.Kind.CONTRIB_MAKE, ElementKind.QUALITY),
                allow(OutgoingLink.Kind.CONTRIB_HELP, ElementKind.QUALITY),
                allow(OutgoingLink.Kind.CONTRIB_HURT, ElementKind.QUALITY),
                allow(OutgoingLink.Kind.CONTRIB_BREAK, ElementKind.QUALITY)));

        matrix.put(ElementKind.RESOURCE, opMap(
                allow(OutgoingLink.Kind.NEEDED_BY, ElementKind.TASK),
                allow(OutgoingLink.Kind.CONTRIB_MAKE, ElementKind.QUALITY),
                allow(OutgoingLink.Kind.CONTRIB_HELP, ElementKind.QUALITY),
                allow(OutgoingLink.Kind.CONTRIB_HURT, ElementKind.QUALITY),
                allow(OutgoingLink.Kind.CONTRIB_BREAK, ElementKind.QUALITY)));

        return matrix;
    }

    @SafeVarargs
    private static Map<OutgoingLink.Kind, EnumSet<ElementKind>> opMap(Map.Entry<OutgoingLink.Kind, EnumSet<ElementKind>>... entries) {
        Map<OutgoingLink.Kind, EnumSet<ElementKind>> map = new EnumMap<>(OutgoingLink.Kind.class);
        for (Map.Entry<OutgoingLink.Kind, EnumSet<ElementKind>> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    private static Map.Entry<OutgoingLink.Kind, EnumSet<ElementKind>> allow(
            OutgoingLink.Kind operator,
            ElementKind... targets) {
        EnumSet<ElementKind> targetSet = EnumSet.noneOf(ElementKind.class);
        for (ElementKind target : targets) {
            targetSet.add(target);
        }
        return Map.entry(operator, targetSet);
    }
}

