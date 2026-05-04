package org.vnu.sme.goal.view;

import org.vnu.sme.goal.mm.bpmn.BpmnModel;

public final class GoalViewRegistry {
    private static GoalDiagramView currentView;
    private static BpmnModel currentBpmnModel;
    private static String currentBpmnSource;

    private GoalViewRegistry() {
    }

    public static synchronized void setCurrentView(GoalDiagramView view) {
        currentView = view;
    }

    public static synchronized GoalDiagramView getCurrentView() {
        return currentView;
    }

    public static synchronized void setCurrentBpmnModel(BpmnModel model, String source) {
        currentBpmnModel = model;
        currentBpmnSource = source;
    }

    public static synchronized BpmnModel getCurrentBpmnModel() {
        return currentBpmnModel;
    }

    public static synchronized String getCurrentBpmnSource() {
        return currentBpmnSource;
    }
}
