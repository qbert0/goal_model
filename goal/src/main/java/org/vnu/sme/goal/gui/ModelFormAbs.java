package org.vnu.sme.goal.gui;

import org.tzi.use.gui.main.MainWindow;
import org.tzi.use.main.Session;

import javax.swing.*;
import java.io.File;
import java.io.PrintWriter;

public abstract class ModelFormAbs extends JDialog implements ModelForm {

    protected final Session session;
    protected final MainWindow mainWindow;
    protected final PrintWriter logWriter;

    protected File selectedFile;
    protected String modelName;

    protected JTextField goalFileName;

    public ModelFormAbs(Session session, MainWindow parent, String title) {
        super(parent, title, true);
        this.session = session;
        this.mainWindow = parent;
        this.logWriter = parent.logWriter();
        this.selectedFile = null;
        this.modelName = session.system().model().name();

        this.goalFileName = new JTextField();
        this.goalFileName.setEditable(false);
    }

    @Override
    public File getSelectedFile() {
        return selectedFile;
    }

    @Override
    public void setSelectedFile(File file) {
        this.selectedFile = file;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    @Override
    public boolean validateForm() {
        if (getSelectedFile() == null) {
            showFileNullError();
            return false;
        }

        if (!getSelectedFile().exists()) {
            showFileNotExistsError();
            return false;
        }

        if (getSelectedFile().length() == 0) {
            showFileEmptyError();
            return false;
        }

        if (getModelName() == null || getModelName().trim().isEmpty()) {
            showModelEmptyError();
            return false;
        }

        return true;
    }

    @Override
    public void showFileNullError() {
        JOptionPane.showMessageDialog(
                this,
                "Please select a .goal file!",
                "No file",
                JOptionPane.ERROR_MESSAGE
        );
    }

    @Override
    public void showFileNotExistsError() {
        JOptionPane.showMessageDialog(
                this,
                "Selected file does not exist!",
                "Invalid file",
                JOptionPane.ERROR_MESSAGE
        );
    }

    @Override
    public void showFileEmptyError() {
        JOptionPane.showMessageDialog(
                this,
                "Goal file is empty!",
                "Empty file",
                JOptionPane.ERROR_MESSAGE
        );
    }

    @Override
    public void showModelEmptyError() {
        JOptionPane.showMessageDialog(
                this,
                "Model name is empty!",
                "Invalid model name",
                JOptionPane.ERROR_MESSAGE
        );
    }

    @Override
    public void showParseSuccess() {
        JOptionPane.showMessageDialog(
                this.mainWindow,
                "Goal file loaded successfully:\n" + getSelectedFile().getAbsolutePath(),
                "Success",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    @Override
    public void showParseError(String message) {
        JOptionPane.showMessageDialog(
                this.mainWindow,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}