package org.vnu.sme.goal.parser.bpmn;

import java.awt.BorderLayout;
import java.io.PrintWriter;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import org.tzi.use.gui.main.ViewFrame;
import org.tzi.use.gui.main.MainWindow;
import org.tzi.use.main.Session;
import org.vnu.sme.goal.ast.bpmn.BpmnModelCS;
import org.vnu.sme.goal.mm.bpmn.BpmnModel;
import org.vnu.sme.goal.view.BpmnDiagramView;
import org.vnu.sme.goal.view.GoalViewRegistry;

public final class BpmnLoader {
    private final Session session;
    private final String bpmnFileName;
    private final PrintWriter logWriter;
    private final MainWindow mainWindow;
    private BpmnModel bpmnModel;

    public BpmnLoader(Session session, String bpmnFileName, PrintWriter logWriter, MainWindow mainWindow) {
        this.session = session;
        this.bpmnFileName = bpmnFileName;
        this.logWriter = logWriter;
        this.mainWindow = mainWindow;
    }

    public boolean run() {
        logWriter.println("Compiling BPMN model ...");
        logWriter.flush();
        BpmnModelCS bpmnAst = BpmnCompiler.compileSpecification(bpmnFileName, logWriter);
        if (bpmnAst == null) {
            return false;
        }
        bpmnModel = new BpmnModelFactory().create(bpmnAst);
        GoalViewRegistry.setCurrentBpmnModel(bpmnModel, bpmnFileName);
        openBpmnDiagramView(bpmnModel);
        logWriter.println("[BPMN] Loaded model '" + bpmnModel.getName() + "' from " + bpmnFileName);
        logWriter.flush();
        return true;
    }

    private void openBpmnDiagramView(BpmnModel model) {
        if (model == null) {
            return;
        }

        BpmnDiagramView bpmnDiagramView = new BpmnDiagramView(model);
        ViewFrame frame = new ViewFrame("BPMN diagram", bpmnDiagramView, "ActivityDiagram.gif");
        JComponent contentPane = (JComponent) frame.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(new JScrollPane(bpmnDiagramView), BorderLayout.CENTER);
        mainWindow.addNewViewFrame(frame);
    }

    public BpmnModel getBpmnModel() {
        return bpmnModel;
    }
}
