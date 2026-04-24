package org.vnu.sme.goal.mm;

public class OrRefinement extends Refinement {
    public OrRefinement(String name) {
        super(name);
    }

    @Override
    public RefinementType getRefinementType() {
        return RefinementType.OR;
    }

    @Override
    public String getType() {
        return "Refinement";
    }
}
