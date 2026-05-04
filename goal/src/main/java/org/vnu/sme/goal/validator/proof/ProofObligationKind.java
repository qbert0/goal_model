package org.vnu.sme.goal.validator.proof;

public enum ProofObligationKind {
    TASK_COVERAGE("task-coverage"),
    POST_IMPLIES_PRE("post-implies-pre"),
    POST_IMPLIES_GOAL("post-implies-goal"),
    POST_PRESERVES_GOAL("post-preserves-goal"),
    POST_AVOIDS_GOAL("post-avoids-goal");

    private final String externalName;

    ProofObligationKind(String externalName) {
        this.externalName = externalName;
    }

    public String externalName() {
        return externalName;
    }
}
