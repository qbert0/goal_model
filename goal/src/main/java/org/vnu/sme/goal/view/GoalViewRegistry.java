package org.vnu.sme.goal.view;

public final class GoalViewRegistry {
    private static GoalDiagramView currentView;

    private GoalViewRegistry() {
    }

    public static synchronized void setCurrentView(GoalDiagramView view) {
        currentView = view;
    }

    public static synchronized GoalDiagramView getCurrentView() {
        return currentView;
    }
}
