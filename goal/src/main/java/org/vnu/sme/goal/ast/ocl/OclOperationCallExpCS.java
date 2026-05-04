package org.vnu.sme.goal.ast.ocl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OclOperationCallExpCS extends OclFeatureCallExpCS {
    private final List<OclExpressionCS> arguments;

    public OclOperationCallExpCS(String text, OclExpressionCS source, String featureName, boolean atPre,
            List<OclExpressionCS> arguments) {
        super(text, source, featureName, atPre);
        this.arguments = arguments == null ? Collections.emptyList() : new ArrayList<>(arguments);
    }

    public List<OclExpressionCS> getArguments() {
        return Collections.unmodifiableList(arguments);
    }
}
