package org.vnu.sme.goal.parser;

import java.awt.BorderLayout;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import javax.swing.JComponent;

import org.tzi.use.gui.main.MainWindow;
import org.tzi.use.gui.main.ViewFrame;
import org.tzi.use.main.Session;
import org.tzi.use.uml.sys.MSystemException;
import org.vnu.sme.goal.mm.GoalModel;
import org.vnu.sme.goal.view.GoalDiagramView;
import org.vnu.sme.goal.view.GoalViewRegistry;

public class GoalLoader {

    private final Session session;
    private final String goalFileName;
    private final PrintWriter logWriter;
    private final MainWindow mainWindow;

    private GoalDiagramView goalDiagramView;
    private GoalModel goalModel;

    public GoalLoader(Session session, String goalFileName, PrintWriter logWriter, MainWindow parent) {
        this.session = session;
        this.goalFileName = goalFileName;
        this.logWriter = logWriter;
        this.mainWindow = parent;
    }

    public boolean run() {
        try {
            goalModel = parseGoalFile();
            openGoalDiagramView(goalModel);
            return goalModel != null;
        } catch (MSystemException | FileNotFoundException e) {
            e.printStackTrace(logWriter);
            logWriter.println("[GoalLoader] Error: " + e.getMessage());
            return false;
        } catch (Exception e) {
            e.printStackTrace(logWriter);
            logWriter.println("[GoalLoader] Unexpected error: " + e.getMessage());
            return false;
        }
    }

    private GoalModel parseGoalFile() throws MSystemException, FileNotFoundException {
        logWriter.println("Compiling GoalModel ...");
        return GOALCompiler.compileSpecification(
                goalFileName,
                logWriter,
                session.system().model(),
                session.system()
        );
    }

    private void openGoalDiagramView(GoalModel model) {
        if (model == null) {
            return;
        }

        goalDiagramView = new GoalDiagramView(mainWindow, model, session.system());
        GoalViewRegistry.setCurrentView(goalDiagramView);

        ViewFrame frame = new ViewFrame("Goal diagram", goalDiagramView, "ClassDiagram.gif");
        JComponent contentPane = (JComponent) frame.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(goalDiagramView, BorderLayout.CENTER);

        mainWindow.addNewViewFrame(frame);
        mainWindow.getModelBrowser().setModel(session.system().model());
    }

    public GoalModel getGoalModel() {
        return goalModel;
    }
}
