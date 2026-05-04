package org.vnu.sme.goal.ast.ocl;

public abstract class OclFeatureCallExpCS extends OclExpressionCS {
    private final OclExpressionCS source;
    private final String featureName;
    private final boolean atPre;

    protected OclFeatureCallExpCS(String text, OclExpressionCS source, String featureName, boolean atPre) {
        super(text);
        this.source = source;
        this.featureName = featureName;
        this.atPre = atPre;
    }

    public OclExpressionCS getSource() {
        return source;
    }

    public String getFeatureName() {
        return featureName;
    }

    public boolean isAtPre() {
        return atPre;
    }
}
