package org.vnu.sme.goal.ast;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;

public abstract class IntentionalElementCS extends DescriptionContainerCS {

    protected final Token fName;
    protected List<RelationRef> relations;

    public IntentionalElementCS(Token fName) {
        this.fName = fName;
        this.relations = new ArrayList<>();
    }

    public Token getfName() {
        return fName;
    }

    public List<RelationRef> getRelations() {
        return relations;
    }

    public void setRelations(List<RelationRef> relations) {
        this.relations = relations;
    }

    public void addRelation(RelationRef relation) {
        this.relations.add(relation);
    }

    // Inner class để lưu quan hệ
    public static class RelationRef {
        private Token relOp;
        private Token targetRef;

        public RelationRef(Token relOp, Token targetRef) {
            this.relOp = relOp;
            this.targetRef = targetRef;
        }

        public Token getRelOp() {
            return relOp;
        }

        public void setRelOp(Token relOp) {
            this.relOp = relOp;
        }

        public Token getTargetRef() {
            return targetRef;
        }

        public void setTargetRef(Token targetRef) {
            this.targetRef = targetRef;
        }
    }
}