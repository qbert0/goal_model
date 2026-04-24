package org.vnu.sme.goal.mm.ocl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OperationCallExp extends FeatureCallExp {
    private final List<Expression> arguments;

    public OperationCallExp(String text, Expression source, String featureName, boolean atPre, List<Expression> arguments) {
        super(text, source, featureName, atPre);
        this.arguments = new ArrayList<>(arguments);
    }

    public List<Expression> getArguments() {
        return Collections.unmodifiableList(arguments);
    }
}
