package org.vnu.sme.goal.ast;

import org.antlr.v4.runtime.Token;

public class QualificationCS extends RelationCS {

    private Token qualityRef;
    private Token targetRef;

    public QualificationCS(Token fName) {
        super(fName);
    }

    public Token getQualityRef() {
        return qualityRef;
    }

    public void setQualityRef(Token qualityRef) {
        this.qualityRef = qualityRef;
    }

    public Token getTargetRef() {
        return targetRef;
    }

    public void setTargetRef(Token targetRef) {
        this.targetRef = targetRef;
    }
}