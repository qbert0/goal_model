package org.vnu.sme.goal.mm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Quality extends IntentionalElement {
    private final List<ConcreteIntentionalElement> qualifiedElements;

    public Quality(String name) {
        super(name);
        this.qualifiedElements = new ArrayList<>();
    }

    public List<ConcreteIntentionalElement> getQualifiedElements() {
        return Collections.unmodifiableList(qualifiedElements);
    }

    public void addQualifiedElement(ConcreteIntentionalElement element) {
        if (element != null && !qualifiedElements.contains(element)) {
            qualifiedElements.add(element);
            if (!element.getQualities().contains(this)) {
                element.addQuality(this);
            }
        }
    }

    @Override
    public String getType() {
        return "Quality";
    }
}
