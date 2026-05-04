package org.vnu.sme.goal.gui;

import java.awt.GridBagConstraints;
import java.awt.KeyEventDispatcher;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;

import org.tzi.use.config.Options;
import org.tzi.use.gui.main.MainWindow;
import org.tzi.use.gui.util.CloseOnEscapeKeyListener;
import org.tzi.use.gui.util.ExtFileFilter;
import org.tzi.use.gui.util.GridBagHelper;
import org.tzi.use.main.Session;
import org.vnu.sme.goal.parser.bpmn.BpmnLoader;

import java.awt.KeyboardFocusManager;

public class BpmnModelForm extends ModelFormAbs {
    private final JButton btnPath;
    private final JButton btnParse;
    private final JButton btnClose;
    private final JFileChooser bpmnFileChooser;

    public BpmnModelForm(Session session, MainWindow parent) {
        super(session, parent, "BPMN Model Loader");

        this.session.addChangeListener(e -> close());
        this.addKeyListener(new CloseOnEscapeKeyListener(this));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        bpmnFileChooser = new JFileChooser(Options.getLastDirectory().toFile());
        bpmnFileChooser.setFileFilter(new ExtFileFilter("bpmn", "Simplified BPMN File"));
        bpmnFileChooser.setDialogTitle("Choose BPMN file");
        bpmnFileChooser.setMultiSelectionEnabled(false);

        btnPath = new JButton("...");
        btnParse = new JButton("Load");
        btnClose = new JButton("Close");

        JComponent contentPane = (JComponent) getContentPane();
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        GridBagHelper gh = new GridBagHelper(contentPane);
        gh.add(new JLabel("BPMN file:"), 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.HORIZONTAL);
        gh.add(goalFileName, 1, 0, 5, 1, 1.0, 0.0, GridBagConstraints.HORIZONTAL);
        gh.add(btnPath, 6, 0, 1, 1, 0.0, 0.0, GridBagConstraints.HORIZONTAL);
        gh.add(btnParse, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.HORIZONTAL);
        gh.add(btnClose, 1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.HORIZONTAL);

        bpmnFileChooser.addActionListener((ActionEvent e) -> {
            if (JFileChooser.APPROVE_SELECTION.equals(e.getActionCommand())) {
                File selected = bpmnFileChooser.getSelectedFile();
                if (selected != null) {
                    setSelectedFile(selected);
                    goalFileName.setText(selected.getAbsolutePath());
                    File parentDir = selected.getParentFile();
                    if (parentDir != null) {
                        Options.setLastDirectory(parentDir.toPath());
                    }
                }
            }
        });

        btnPath.addActionListener((ActionEvent e) -> bpmnFileChooser.showOpenDialog(BpmnModelForm.this));
        btnParse.addActionListener((ActionEvent e) -> {
            if (validateForm()) {
                parse();
            }
        });
        btnClose.addActionListener((ActionEvent e) -> close());

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if ((e.getSource() instanceof JComponent)
                        && ((JComponent) e.getSource()).getRootPane() == getRootPane()
                        && e.getID() == KeyEvent.KEY_PRESSED) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        btnParse.doClick();
                        return true;
                    }
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        close();
                        return true;
                    }
                }
                return false;
            }
        });

        getRootPane().setDefaultButton(btnParse);
        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    @Override
    public void close() {
        setVisible(false);
        dispose();
    }

    @Override
    public void parse() {
        try {
            logWriter.println("[BPMN] Selected file: " + getSelectedFile().getAbsolutePath());
            logWriter.flush();
            BpmnLoader loader = new BpmnLoader(session, getSelectedFile().getAbsolutePath(), logWriter, mainWindow);
            boolean success = loader.run();
            if (success) {
                showParseSuccess();
                close();
            } else {
                showParseError("Failed to load BPMN file. See console log for details.");
            }
        } catch (Exception ex) {
            showParseError(ex.getMessage());
        }
    }

    @Override
    public void showFileNullError() {
        javax.swing.JOptionPane.showMessageDialog(this, "Please select a .bpmn file!", "No file",
                javax.swing.JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void showFileEmptyError() {
        javax.swing.JOptionPane.showMessageDialog(this, "BPMN file is empty!", "Empty file",
                javax.swing.JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void showParseSuccess() {
        javax.swing.JOptionPane.showMessageDialog(
                this.mainWindow,
                "BPMN file loaded successfully:\n" + getSelectedFile().getAbsolutePath(),
                "Success",
                javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }
}
