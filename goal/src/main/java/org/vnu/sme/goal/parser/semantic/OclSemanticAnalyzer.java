package org.vnu.sme.goal.parser.semantic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.antlr.v4.runtime.Token;
import org.tzi.use.uml.mm.MAssociationEnd;
import org.tzi.use.uml.mm.MAttribute;
import org.tzi.use.uml.mm.MClass;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.mm.MNavigableElement;
import org.tzi.use.uml.ocl.type.Type;
import org.vnu.sme.goal.ast.ocl.OclVariableDeclarationCS;
import org.vnu.sme.goal.mm.ocl.AggregateCallExp;
import org.vnu.sme.goal.mm.ocl.AtPreExp;
import org.vnu.sme.goal.mm.ocl.BinaryExp;
import org.vnu.sme.goal.mm.ocl.Expression;
import org.vnu.sme.goal.mm.ocl.IteratorExp;
import org.vnu.sme.goal.mm.ocl.OperationCallExp;
import org.vnu.sme.goal.mm.ocl.PropertyCallExp;
import org.vnu.sme.goal.mm.ocl.SelfExp;
import org.vnu.sme.goal.mm.ocl.UnaryExp;
import org.vnu.sme.goal.mm.ocl.VariableDeclaration;
import org.vnu.sme.goal.mm.ocl.VariableDeclaration;
import org.vnu.sme.goal.mm.ocl.VariableExp;
import org.vnu.sme.goal.parser.semantic.enums.ElementKind;
import org.vnu.sme.goal.parser.semantic.enums.GoalContractType;
import org.vnu.sme.goal.parser.semantic.symbols.ActorSymbol;
import org.vnu.sme.goal.parser.semantic.symbols.ElementSymbol;
import org.vnu.sme.goal.parser.semantic.symbols.GoalContract;
import org.vnu.sme.goal.parser.semantic.symbols.GoalSymbolTable;
import org.vnu.sme.goal.parser.semantic.symbols.SemanticIssue;
import org.vnu.sme.goal.parser.semantic.symbols.TaskContract;

/**
 * Semantic checks for the OCL extension introduced in spec v2 (sections 2–6).
 *
 * <p>Mirrors the abstraction style of {@link GoalSemanticAnalyzer}: each public
 * {@code traverseXxx} method walks one slice of the symbol table and returns
 * a {@code List<SemanticIssue>}. Issues collected by the pipeline are merged
 * into a single list after S1–S10 have run.</p>
 *
 * <p>Order of error groups, following SEMANTIC_EXTENSION_V2.md §5:</p>
 * <ol>
 *   <li>{@code traverseGoalContractCardinality} — E5 (defensive)</li>
 *   <li>{@code traverseAchieveContracts}        — E1, E2</li>
 *   <li>{@code traverseContractBodies}          — E4</li>
 *   <li>{@code traverseOclExpressions}          — E3, E6, E7 (one walk)</li>
 * </ol>
 */
public final class OclSemanticAnalyzer {
    private final MModel useModel;
    private final MClass selfRootClass;

    public OclSemanticAnalyzer(MModel useModel) {
        this.useModel = useModel;
        this.selfRootClass = resolveRootClass(useModel);
    }

    // -----------------------------------------------------------------
    // Traversal: goal-contract cardinality (E5)
    // -----------------------------------------------------------------

    /**
     * E5: a single goal element must declare at most one {@code goalClause}.
     * <p>Defensive: the grammar permits {@code goalClause?} (max one) so this
     * only fires when an external builder bypassed the parser.</p>
     */
    public List<SemanticIssue> traverseGoalContractCardinality(GoalSymbolTable table) {
        List<SemanticIssue> issues = new ArrayList<>();
        for (ActorSymbol actor : table.getActorsByName().values()) {
            for (ElementSymbol element : actor.getElementTable().values()) {
                if (element.getKind() != ElementKind.GOAL) {
                    continue;
                }
                if (element.getGoalContractAssignmentCount() > 1) {
                    Token at = element.getDeclarationToken();
                    issues.add(new SemanticIssue(
                            "E5",
                            "Goal '" + element.getQualifiedName()
                                    + "' has more than one goalClause (count="
                                    + element.getGoalContractAssignmentCount() + ")",
                            at.getLine(),
                            at.getCharPositionInLine()));
                }
            }
        }
        return issues;
    }

    // -----------------------------------------------------------------
    // Traversal: achieve-for-unique syntax (E1, E2)
    // -----------------------------------------------------------------

    /**
     * E1: {@code achieve for unique (...)} requires a non-empty typed-var list.
     * E2: {@code achieve for unique ... in &lt;sourceExpr&gt;} requires the source.
     *
     * <p>Both checks apply only when {@code goalContract.type == ACHIEVE_UNIQUE}.</p>
     */
    public List<SemanticIssue> traverseAchieveContracts(GoalSymbolTable table) {
        List<SemanticIssue> issues = new ArrayList<>();
        for (ElementSymbol element : iterateGoalElements(table)) {
            GoalContract contract = element.getGoalContract();
            if (contract == null || contract.getType() != GoalContractType.ACHIEVE_UNIQUE) {
                continue;
            }
            Token at = positionToken(element, contract.getKeywordToken());

            if (contract.getIterVars() == null || contract.getIterVars().isEmpty()) {
                issues.add(new SemanticIssue(
                        "E1",
                        "achieve for unique on '" + element.getQualifiedName()
                                + "' is missing iterator variables",
                        at.getLine(),
                        at.getCharPositionInLine()));
            }
            if (contract.getSourceExpr() == null) {
                issues.add(new SemanticIssue(
                        "E2",
                        "achieve for unique on '" + element.getQualifiedName()
                                + "' is missing 'in <expression>' source",
                        at.getLine(),
                        at.getCharPositionInLine()));
            }
        }
        return issues;
    }

    // -----------------------------------------------------------------
    // Traversal: empty contract body (E4)
    // -----------------------------------------------------------------

    /**
     * E4: a contract keyword exists but the body expression(s) are missing.
     * <p>Defensive: the grammar already requires {@code expression} after every
     * clause keyword. Fires when AST construction yields a partially populated
     * contract (e.g., bug in builder or external producer).</p>
     */
    public List<SemanticIssue> traverseContractBodies(GoalSymbolTable table) {
        List<SemanticIssue> issues = new ArrayList<>();
        for (ActorSymbol actor : table.getActorsByName().values()) {
            for (ElementSymbol element : actor.getElementTable().values()) {
                checkGoalContractBody(element, issues);
                checkTaskContractBody(element, issues);
            }
        }
        return issues;
    }

    private void checkGoalContractBody(ElementSymbol element, List<SemanticIssue> issues) {
        GoalContract contract = element.getGoalContract();
        if (contract == null || contract.getBodyExpr() != null) {
            return;
        }
        Token at = positionToken(element, contract.getKeywordToken());
        issues.add(new SemanticIssue(
                "E4",
                "Goal contract on '" + element.getQualifiedName()
                        + "' has no body expression after the keyword",
                at.getLine(),
                at.getCharPositionInLine()));
    }

    private void checkTaskContractBody(ElementSymbol element, List<SemanticIssue> issues) {
        TaskContract contract = element.getTaskContract();
        if (contract == null) {
            return;
        }
        // Only fire when both pre and post are absent — a single side is allowed
        // by grammar (see SEMANTIC_EXTENSION_V2 §3.2).
        if (!contract.isEmpty()) {
            return;
        }
        Token at = positionToken(element, firstNonNull(contract.getPreToken(), contract.getPostToken()));
        issues.add(new SemanticIssue(
                "E4",
                "Task contract on '" + element.getQualifiedName()
                        + "' has neither pre nor post expression",
                at.getLine(),
                at.getCharPositionInLine()));
    }

    // -----------------------------------------------------------------
    // Traversal: OCL expression validity (E3, E6, E7)
    // -----------------------------------------------------------------

    /**
     * Walk every OCL body in the table once and emit:
     * <ul>
     *   <li>E3 — {@code ->} chained after a scalar/boolean aggregation
     *       (size/max/min/isEmpty/notEmpty/includes/excludes/count) or after
     *       a boolean iterator (forAll/exists).</li>
     *   <li>E6 — a {@code self.x.y...} navigation segment that does not exist
     *       on the corresponding USE class.</li>
     *   <li>E7 — an iterator-variable type ({@code s: Student}) that does not
     *       resolve to a class in the USE model.</li>
     * </ul>
     */
    public List<SemanticIssue> traverseOclExpressions(GoalSymbolTable table) {
        List<SemanticIssue> issues = new ArrayList<>();
        for (ActorSymbol actor : table.getActorsByName().values()) {
            for (ElementSymbol element : actor.getElementTable().values()) {
                walkContracts(element, issues);
            }
        }
        return issues;
    }

    private void walkContracts(ElementSymbol element, List<SemanticIssue> issues) {
        GoalContract goal = element.getGoalContract();
        if (goal != null) {
            Token at = positionToken(element, goal.getKeywordToken());
            // Iterator vars on `achieve for unique` live on the contract, not in
            // an OCL node — type-check them separately for E7.
            checkIteratorVarTypes(goal.getIterVars(), at, element, issues);

            ExprWalkContext ctx = newRootContext().bindAll(goal.getIterVars());
            walk(goal.getSourceExprRuntime(), ctx, at, element, issues);
            walk(goal.getBodyExprRuntime(), ctx, at, element, issues);
        }
        TaskContract task = element.getTaskContract();
        if (task != null) {
            ExprWalkContext ctx = newRootContext();
            if (task.getPrecondition() != null) {
                Token at = positionToken(element, task.getPreToken());
                walk(task.getPreconditionRuntime(), ctx, at, element, issues);
            }
            if (task.getPostcondition() != null) {
                Token at = positionToken(element, task.getPostToken());
                walk(task.getPostconditionRuntime(), ctx, at, element, issues);
            }
        }
    }

    /**
     * Recursive AST walker that dispatches per node type and emits E3/E6.
     * Iterator-variable type checks (E7) run inline at IteratorExp/AggregateCallExp(COUNT)
     * and at form-2 achieve clauses ({@link #checkIteratorVarTypes}).
     */
    private void walk(Expression expr, ExprWalkContext ctx, Token reportToken,
                      ElementSymbol element, List<SemanticIssue> issues) {
        if (expr == null) {
            return;
        }
        if (expr instanceof IteratorExp it) {
            checkInvalidArrowChain(it.getSource(), reportToken, element, expr, issues);
            walk(it.getSource(), ctx, reportToken, element, issues);
            checkIteratorVarTypesRuntime(it.getVariables(), reportToken, element, issues);
            ExprWalkContext bodyCtx = ctx.bindIteratorVars(it.getVariables(), elementClassOf(it.getSource(), ctx));
            walk(it.getBody(), bodyCtx, reportToken, element, issues);
            return;
        }
        if (expr instanceof AggregateCallExp ag) {
            checkInvalidArrowChain(ag.getSource(), reportToken, element, expr, issues);
            walk(ag.getSource(), ctx, reportToken, element, issues);
            if (ag.getKind() == AggregateCallExp.Kind.COUNT) {
                checkIteratorVarTypesRuntime(ag.getIteratorVars(), reportToken, element, issues);
                ExprWalkContext bodyCtx = ctx.bindIteratorVars(ag.getIteratorVars(),
                        elementClassOf(ag.getSource(), ctx));
                walk(ag.getBody(), bodyCtx, reportToken, element, issues);
            } else {
                for (Expression argument : ag.getArguments()) {
                    walk(argument, ctx, reportToken, element, issues);
                }
            }
            return;
        }
        if (expr instanceof PropertyCallExp prop) {
            walk(prop.getSource(), ctx, reportToken, element, issues);
            checkSelfNavigation(prop, ctx, reportToken, element, issues);
            return;
        }
        if (expr instanceof OperationCallExp op) {
            walk(op.getSource(), ctx, reportToken, element, issues);
            for (Expression argument : op.getArguments()) {
                walk(argument, ctx, reportToken, element, issues);
            }
            return;
        }
        if (expr instanceof AtPreExp atPre) {
            walk(atPre.getSource(), ctx, reportToken, element, issues);
            return;
        }
        if (expr instanceof BinaryExp bin) {
            walk(bin.getLeft(), ctx, reportToken, element, issues);
            walk(bin.getRight(), ctx, reportToken, element, issues);
            return;
        }
        if (expr instanceof UnaryExp un) {
            walk(un.getOperand(), ctx, reportToken, element, issues);
        }
    }

    // ---- E3: collection chaining --------------------------------------------

    /**
     * Emit E3 if {@code source} cannot be chained with another {@code ->op}
     * because it returns a scalar or boolean.
     */
    private void checkInvalidArrowChain(Expression source, Token reportToken,
                                        ElementSymbol element, Expression chained,
                                        List<SemanticIssue> issues) {
        if (source == null) {
            return;
        }
        String reason = scalarOrBooleanReason(source);
        if (reason == null) {
            return;
        }
        issues.add(new SemanticIssue(
                "E3",
                "Invalid '->' chain on '" + element.getQualifiedName()
                        + "': '" + source.getText() + "' returns " + reason
                        + " — cannot chain '" + chainSummary(chained) + "' after it",
                reportToken.getLine(),
                reportToken.getCharPositionInLine()));
    }

    private static String scalarOrBooleanReason(Expression source) {
        if (source instanceof AggregateCallExp ag) {
            return ag.getKind() == AggregateCallExp.Kind.INCLUDES
                    || ag.getKind() == AggregateCallExp.Kind.EXCLUDES
                    || ag.getKind() == AggregateCallExp.Kind.IS_EMPTY
                    || ag.getKind() == AggregateCallExp.Kind.NOT_EMPTY
                    ? "boolean"
                    : "scalar";
        }
        if (source instanceof IteratorExp it) {
            switch (it.getKind()) {
                case FOR_ALL:
                case EXISTS:
                case IS_UNIQUE:
                    return "boolean";
                default:
                    return null;
            }
        }
        return null;
    }

    private static String chainSummary(Expression chained) {
        if (chained instanceof IteratorExp it) {
            return "->" + it.getIteratorName() + "(...)";
        }
        if (chained instanceof AggregateCallExp ag) {
            return "->" + ag.getFeatureName() + "(...)";
        }
        return chained.getText();
    }

    // ---- E6: invalid self-rooted navigation ---------------------------------

    /**
     * Emit E6 when a {@link PropertyCallExp} navigates a property that does
     * not exist on the source's resolved {@link MClass}.
     * <p>The check fires only when the source's class is known — so navigations
     * starting from {@code self} or from typed iterator variables are checked,
     * while paths off untyped variables are silently skipped.</p>
     */
    private void checkSelfNavigation(PropertyCallExp prop, ExprWalkContext ctx,
                                     Token reportToken, ElementSymbol element,
                                     List<SemanticIssue> issues) {
        if (useModel == null) {
            return;
        }
        MClass sourceClass = classOf(prop.getSource(), ctx);
        if (sourceClass == null) {
            return;
        }
        if (resolveProperty(sourceClass, prop.getFeatureName()) != null) {
            return;
        }
        issues.add(new SemanticIssue(
                "E6",
                "Invalid self navigation on '" + element.getQualifiedName()
                        + "': property '" + prop.getFeatureName()
                        + "' does not exist on USE class '" + sourceClass.name() + "'",
                reportToken.getLine(),
                reportToken.getCharPositionInLine()));
    }

    private static Object resolveProperty(MClass owner, String name) {
        MAttribute attribute = owner.attribute(name, true);
        if (attribute != null) {
            return attribute;
        }
        MNavigableElement navigableEnd = owner.navigableEnd(name);
        if (navigableEnd != null) {
            return navigableEnd;
        }
        for (MAssociationEnd end : owner.getAssociationEnd(name)) {
            return end;
        }
        return null;
    }

    // ---- E7: iterator-variable type unresolvable ----------------------------

    private void checkIteratorVarTypes(List<OclVariableDeclarationCS> vars, Token reportToken,
                                       ElementSymbol element, List<SemanticIssue> issues) {
        if (useModel == null || vars == null) {
            return;
        }
        for (OclVariableDeclarationCS var : vars) {
            if (var.getTypeName() == null) {
                continue;
            }
            if (resolveTypeClass(var.getTypeName()) != null) {
                continue;
            }
            issues.add(new SemanticIssue(
                    "E7",
                    "Iterator variable '" + var.getName() + ": " + var.getTypeName()
                            + "' on '" + element.getQualifiedName()
                            + "' references unknown type — not a class in the USE model",
                    reportToken.getLine(),
                    reportToken.getCharPositionInLine()));
        }
    }

    private void checkIteratorVarTypesRuntime(List<VariableDeclaration> vars, Token reportToken,
                                              ElementSymbol element, List<SemanticIssue> issues) {
        if (useModel == null || vars == null) {
            return;
        }
        for (VariableDeclaration var : vars) {
            if (var.getTypeName() == null) {
                continue;
            }
            if (resolveTypeClass(var.getTypeName()) != null) {
                continue;
            }
            issues.add(new SemanticIssue(
                    "E7",
                    "Iterator variable '" + var.getName() + ": " + var.getTypeName()
                            + "' on '" + element.getQualifiedName()
                            + "' references unknown type — not a class in the USE model",
                    reportToken.getLine(),
                    reportToken.getCharPositionInLine()));
        }
    }

    // -----------------------------------------------------------------
    // Helpers — type resolution against the USE model
    // -----------------------------------------------------------------

    private ExprWalkContext newRootContext() {
        ExprWalkContext ctx = new ExprWalkContext();
        if (selfRootClass != null) {
            ctx.bind("self", selfRootClass);
        }
        return ctx;
    }

    /**
     * Resolve the {@link MClass} of an OCL expression. Returns {@code null}
     * when the type is unknown — callers must skip dependent checks rather
     * than cascading false positives.
     */
    private MClass classOf(Expression expr, ExprWalkContext ctx) {
        if (expr == null) {
            return null;
        }
        if (expr instanceof SelfExp) {
            return ctx.lookup("self");
        }
        if (expr instanceof VariableExp var) {
            return ctx.lookup(var.getName());
        }
        if (expr instanceof AtPreExp atPre) {
            return classOf(atPre.getSource(), ctx);
        }
        if (expr instanceof PropertyCallExp prop) {
            MClass owner = classOf(prop.getSource(), ctx);
            return owner == null ? null : navigateProperty(owner, prop.getFeatureName());
        }
        if (expr instanceof IteratorExp it && it.getKind() == IteratorExp.IteratorKind.COLLECT) {
            ExprWalkContext bodyCtx = ctx.bindIteratorVars(
                    it.getVariables(), elementClassOf(it.getSource(), ctx));
            return classOf(it.getBody(), bodyCtx);
        }
        return null;
    }

    /**
     * Element type of a collection expression — used to bind iterator vars
     * (e.g., {@code self.classes->forAll(c | ...)} binds {@code c} to the
     * element class of {@code self.classes}).
     */
    private MClass elementClassOf(Expression collectionExpr, ExprWalkContext ctx) {
        if (collectionExpr instanceof PropertyCallExp prop) {
            MClass owner = classOf(prop.getSource(), ctx);
            if (owner == null) {
                return null;
            }
            MNavigableElement navigableEnd = owner.navigableEnd(prop.getFeatureName());
            if (navigableEnd != null) {
                return navigableEnd.cls();
            }
            for (MAssociationEnd end : owner.getAssociationEnd(prop.getFeatureName())) {
                return end.cls();
            }
            MAttribute attr = owner.attribute(prop.getFeatureName(), true);
            if (attr != null && attr.type() instanceof MClass cls) {
                return cls;
            }
            return null;
        }
        return classOf(collectionExpr, ctx);
    }

    private MClass navigateProperty(MClass owner, String name) {
        MAttribute attribute = owner.attribute(name, true);
        if (attribute != null) {
            Type type = attribute.type();
            return type instanceof MClass cls ? cls : null;
        }
        MNavigableElement navigableEnd = owner.navigableEnd(name);
        if (navigableEnd != null) {
            return navigableEnd.cls();
        }
        for (MAssociationEnd end : owner.getAssociationEnd(name)) {
            return end.cls();
        }
        return null;
    }

    private MClass resolveTypeClass(String qualifiedName) {
        if (useModel == null || qualifiedName == null) {
            return null;
        }
        // Symbol table allows dotted names (e.g., 'pkg.Cls'); fall back to last segment.
        MClass direct = useModel.getClass(qualifiedName);
        if (direct != null) {
            return direct;
        }
        int dot = qualifiedName.lastIndexOf('.');
        if (dot >= 0 && dot + 1 < qualifiedName.length()) {
            return useModel.getClass(qualifiedName.substring(dot + 1));
        }
        return null;
    }

    private static MClass resolveRootClass(MModel model) {
        if (model == null) {
            return null;
        }
        MClass systemState = model.getClass("SystemState");
        if (systemState != null) {
            return systemState;
        }
        return model.classes().isEmpty() ? null : model.classes().iterator().next();
    }

    // -----------------------------------------------------------------
    // Helpers — symbol-table iteration / token plumbing
    // -----------------------------------------------------------------

    private static Iterable<ElementSymbol> iterateGoalElements(GoalSymbolTable table) {
        List<ElementSymbol> goals = new ArrayList<>();
        for (ActorSymbol actor : table.getActorsByName().values()) {
            for (ElementSymbol element : actor.getElementTable().values()) {
                if (element.getKind() == ElementKind.GOAL && element.getGoalContract() != null) {
                    goals.add(element);
                }
            }
        }
        return goals;
    }

    private static Token positionToken(ElementSymbol element, Token preferred) {
        return preferred != null ? preferred : element.getDeclarationToken();
    }

    private static Token firstNonNull(Token first, Token second) {
        return first != null ? first : second;
    }

    /**
     * Per-walk variable scope. Maps OCL variable names ({@code self}, iterator
     * vars, typed-var-list bindings) onto USE classes so that
     * {@link #classOf(Expression, ExprWalkContext)} can decide whether a
     * navigation is type-resolvable.
     */
    private final class ExprWalkContext {
        private final Map<String, MClass> variableTypes;

        private ExprWalkContext() {
            this.variableTypes = new LinkedHashMap<>();
        }

        private ExprWalkContext(Map<String, MClass> variableTypes) {
            this.variableTypes = new LinkedHashMap<>(variableTypes);
        }

        ExprWalkContext bind(String name, MClass clazz) {
            variableTypes.put(name, clazz);
            return this;
        }

        ExprWalkContext bindAll(List<OclVariableDeclarationCS> typedVars) {
            if (typedVars == null) {
                return this;
            }
            for (OclVariableDeclarationCS var : typedVars) {
                MClass type = var.getTypeName() == null ? null : resolveTypeClass(var.getTypeName());
                if (type != null) {
                    variableTypes.put(var.getName(), type);
                }
            }
            return this;
        }

        ExprWalkContext bindIteratorVars(List<VariableDeclaration> iterVars, MClass elementClass) {
            ExprWalkContext copy = new ExprWalkContext(this.variableTypes);
            if (iterVars == null) {
                return copy;
            }
            for (VariableDeclaration var : iterVars) {
                MClass type;
                if (var.getTypeName() != null) {
                    type = resolveTypeClass(var.getTypeName());
                } else {
                    type = elementClass;
                }
                if (type != null) {
                    copy.variableTypes.put(var.getName(), type);
                }
            }
            return copy;
        }

        MClass lookup(String name) {
            return variableTypes.get(name);
        }
    }
}
