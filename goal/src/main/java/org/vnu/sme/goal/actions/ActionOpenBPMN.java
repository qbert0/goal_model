package org.vnu.sme.goal.actions;

import org.tzi.use.gui.main.MainWindow;
import org.tzi.use.main.Session;
import org.tzi.use.runtime.gui.IPluginAction;
import org.tzi.use.runtime.gui.IPluginActionDelegate;
import org.vnu.sme.goal.gui.BpmnModelForm;

public class ActionOpenBPMN implements IPluginActionDelegate {
    @Override
    public void performAction(IPluginAction pluginAction) {
        Session session = pluginAction.getSession();
        MainWindow mainWindow = pluginAction.getParent();
        BpmnModelForm bpmnModelForm = new BpmnModelForm(session, mainWindow);
        bpmnModelForm.setResizable(true);
    }
}
