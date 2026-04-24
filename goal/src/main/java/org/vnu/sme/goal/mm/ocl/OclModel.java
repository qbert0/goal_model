package org.vnu.sme.goal.mm.ocl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OclModel {
    private final List<Expression> expressions;

    public OclModel() {
        this.expressions = new ArrayList<>();
    }

    public List<Expression> getExpressions() {
        return Collections.unmodifiableList(expressions);
    }

    public void addExpression(Expression expression) {
        if (expression != null) {
            expressions.add(expression);
        }
    }
}
