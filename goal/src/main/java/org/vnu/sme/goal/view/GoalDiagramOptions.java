package org.vnu.sme.goal.view;

import org.tzi.use.gui.views.diagrams.DiagramOptions;

public class GoalDiagramOptions extends DiagramOptions {
    private boolean showMultiplicities;

    @Override
    public boolean isShowMutliplicities() {
        return showMultiplicities;
    }

    @Override
    public void setShowMutliplicities(boolean showMutliplicities) {
        this.showMultiplicities = showMutliplicities;
    }

    @Override
    protected void registerAdditionalColors() {
        // The GOAL diagram uses the default diagram color set.
    }
}
