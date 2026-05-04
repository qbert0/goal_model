package org.vnu.sme.goal.actions;

import javax.swing.JOptionPane;

import org.tzi.use.runtime.gui.IPluginAction;
import org.tzi.use.runtime.gui.IPluginActionDelegate;
import org.vnu.sme.goal.mm.bpmn.BpmnModel;
import org.vnu.sme.goal.view.GoalDiagramView;
import org.vnu.sme.goal.view.GoalViewRegistry;

public class ActionVerifyGoalWithBpmn implements IPluginActionDelegate {
    @Override
    public void performAction(IPluginAction pluginAction) {
        GoalDiagramView view = GoalViewRegistry.getCurrentView();
        if (view == null) {
            JOptionPane.showMessageDialog(
                    pluginAction.getParent(),
                    "No GOAL diagram is open yet. Load a .goal file first.",
                    "GOAL + BPMN verification",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        BpmnModel bpmnModel = GoalViewRegistry.getCurrentBpmnModel();
        if (bpmnModel == null) {
            JOptionPane.showMessageDialog(
                    pluginAction.getParent(),
                    "No BPMN model is loaded yet. Load a .bpmn file first.",
                    "GOAL + BPMN verification",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        view.showBpmnVerificationReport(bpmnModel, GoalViewRegistry.getCurrentBpmnSource());
    }
}
