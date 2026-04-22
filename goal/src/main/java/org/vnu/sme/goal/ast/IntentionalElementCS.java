package org.vnu.sme.goal.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.Token;

public abstract class IntentionalElementCS extends DescriptionContainerCS {

    protected final Token fName;
    private final List<OutgoingLink> outgoingLinks = new ArrayList<>();

    public IntentionalElementCS(Token fName) {
        this.fName = fName;
    }

    public Token getfName() {
        return fName;
    }

    public List<OutgoingLink> getOutgoingLinks() {
        return Collections.unmodifiableList(outgoingLinks);
    }

    public void addOutgoingLink(OutgoingLink link) {
        outgoingLinks.add(link);
    }
}
