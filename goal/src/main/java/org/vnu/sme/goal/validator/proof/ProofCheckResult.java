package org.vnu.sme.goal.validator.proof;

public record ProofCheckResult(boolean proved, String detail) {
    public static ProofCheckResult proved(String detail) {
        return new ProofCheckResult(true, detail);
    }

    public static ProofCheckResult failed(String detail) {
        return new ProofCheckResult(false, detail);
    }

    public String externalStatus() {
        return proved ? "TRUE" : "FALSE";
    }
}
