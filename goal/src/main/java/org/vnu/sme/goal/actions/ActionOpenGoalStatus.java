package org.vnu.sme.goal.actions;

import javax.swing.JOptionPane;

import org.tzi.use.runtime.gui.IPluginAction;
import org.tzi.use.runtime.gui.IPluginActionDelegate;
import org.vnu.sme.goal.view.GoalDiagramView;
import org.vnu.sme.goal.view.GoalViewRegistry;

public class ActionOpenGoalStatus implements IPluginActionDelegate {
    @Override
    public void performAction(IPluginAction pluginAction) {
        GoalDiagramView view = GoalViewRegistry.getCurrentView();
        if (view == null) {
            JOptionPane.showMessageDialog(
                    pluginAction.getParent(),
                    "No GOAL diagram is open yet. Load a .goal file first.",
                    "GOAL status",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        view.showGoalStatusTable();
    }
}
