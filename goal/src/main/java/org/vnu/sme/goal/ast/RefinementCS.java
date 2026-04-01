package org.vnu.sme.goal.ast;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;

public class RefinementCS extends RelationCS {

    private Token type;
    private Token parentRef;
    private List<Token> childrenRefs;

    public RefinementCS(Token fName) {
        super(fName);
        this.childrenRefs = new ArrayList<>();
    }

    public Token getType() {
        return type;
    }

    public void setType(Token type) {
        this.type = type;
    }

    public Token getParentRef() {
        return parentRef;
    }

    public void setParentRef(Token parentRef) {
        this.parentRef = parentRef;
    }

    public List<Token> getChildrenRefs() {
        return childrenRefs;
    }

    public void setChildrenRefs(List<Token> childrenRefs) {
        this.childrenRefs = childrenRefs;
    }

    public void addChildRef(Token childRef) {
        this.childrenRefs.add(childRef);
    }
}