package org.vnu.sme.goal.mm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ConcreteIntentionalElement extends IntentionalElement {
    private final List<Quality> qualities;

    protected ConcreteIntentionalElement(String name) {
        super(name);
        this.qualities = new ArrayList<>();
    }

    public List<Quality> getQualities() {
        return Collections.unmodifiableList(qualities);
    }

    public void addQuality(Quality quality) {
        if (quality != null && !qualities.contains(quality)) {
            qualities.add(quality);
            if (!quality.getQualifiedElements().contains(this)) {
                quality.addQualifiedElement(this);
            }
        }
    }
}
