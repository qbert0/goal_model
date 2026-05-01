package org.vnu.sme.goal.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OclValidationReport {
    private final List<String> messages = new ArrayList<>();

    public void add(String message) {
        messages.add(message);
    }

    public boolean hasProblems() {
        return !messages.isEmpty();
    }

    public List<String> getMessages() {
        return Collections.unmodifiableList(messages);
    }
}
