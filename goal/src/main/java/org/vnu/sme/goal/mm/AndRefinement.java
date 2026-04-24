package org.vnu.sme.goal.mm;

public class AndRefinement extends Refinement {
    public AndRefinement(String name) {
        super(name);
    }

    @Override
    public RefinementType getRefinementType() {
        return RefinementType.AND;
    }

    @Override
    public String getType() {
        return "Refinement";
    }
}
