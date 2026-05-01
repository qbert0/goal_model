package org.vnu.sme.goal.mm.ocl;

public abstract class FeatureCallExp extends Expression {
    private final Expression source;
    private final String featureName;
    private final boolean atPre;

    protected FeatureCallExp(String text, Expression source, String featureName, boolean atPre) {
        super(text);
        this.source = source;
        this.featureName = featureName;
        this.atPre = atPre;
    }

    public Expression getSource() {
        return source;
    }

    public String getFeatureName() {
        return featureName;
    }

    public boolean isAtPre() {
        return atPre;
    }
}
