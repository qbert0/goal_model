package org.vnu.sme.goal.mm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.vnu.sme.goal.mm.ocl.Expression;

public abstract class Clause {
    private final List<Expression> expressions;

    protected Clause() {
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

    public String getPrimaryText() {
        return expressions.isEmpty() ? null : expressions.get(0).getText();
    }
}
