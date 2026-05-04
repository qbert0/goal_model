package org.vnu.sme.goal.parser.semantic.symbols;

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.Token;
import org.vnu.sme.goal.parser.semantic.enums.ElementKind;

public final class ElementSymbol {
    private final String name;
    private final ElementKind kind;
    private final ActorSymbol ownerActor;
    private final Token declarationToken;
    private final List<RelationEntry> relations = new ArrayList<>();
    private boolean leaf = true;

    /** OCL contract attached to a goal element ({@code achieve|maintain|avoid|achieve for unique}). */
    private GoalContract goalContract;

    /**
     * Number of goal contracts assigned to this symbol.
     * <p>Defensive counter — although the grammar permits at most one
     * {@code goalClause}, downstream tools may bypass the parser. E5
     * (DuplicateGoalContractType) fires whenever this exceeds 1.</p>
     */
    private int goalContractAssignmentCount;

    /** OCL contract attached to a task element ({@code pre} / {@code post}). */
    private TaskContract taskContract;

    public ElementSymbol(String name, ElementKind kind, ActorSymbol ownerActor, Token declarationToken) {
        this.name = name;
        this.kind = kind;
        this.ownerActor = ownerActor;
        this.declarationToken = declarationToken;
    }

    public String getName() {
        return name;
    }

    public ElementKind getKind() {
        return kind;
    }

    public ActorSymbol getOwnerActor() {
        return ownerActor;
    }

    public Token getDeclarationToken() {
        return declarationToken;
    }

    public List<RelationEntry> getRelations() {
        return relations;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public void setLeaf(boolean leaf) {
        this.leaf = leaf;
    }

    public String getQualifiedName() {
        return ownerActor.getName() + "." + name;
    }

    public GoalContract getGoalContract() {
        return goalContract;
    }

    /**
     * Assign a goal contract; tracks the assignment count so that E5
     * can fire defensively if the same element is assigned more than one
     * contract (which the grammar already forbids via {@code goalClause?}).
     */
    public void setGoalContract(GoalContract goalContract) {
        this.goalContract = goalContract;
        this.goalContractAssignmentCount++;
    }

    public int getGoalContractAssignmentCount() {
        return goalContractAssignmentCount;
    }

    public TaskContract getTaskContract() {
        return taskContract;
    }

    public void setTaskContract(TaskContract taskContract) {
        this.taskContract = taskContract;
    }
}
