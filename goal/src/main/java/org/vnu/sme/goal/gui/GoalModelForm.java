package org.vnu.sme.goal.gui;

import org.tzi.use.config.Options;
import org.tzi.use.gui.main.MainWindow;
import org.tzi.use.gui.util.CloseOnEscapeKeyListener;
import org.tzi.use.gui.util.ExtFileFilter;
import org.tzi.use.gui.util.GridBagHelper;
import org.tzi.use.main.Session;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

public class GoalModelForm extends ModelFormAbs {

    private JButton btnPath;
    private JButton btnParse;
    private JButton btnClose;
    private JFileChooser goalFileChooser;

    public GoalModelForm(Session session, MainWindow parent) {
        super(session, parent, "Goal Model Loader");

        this.session.addChangeListener(e -> close());
        this.addKeyListener(new CloseOnEscapeKeyListener(this));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        goalFileChooser = new JFileChooser(Options.getLastDirectory().toFile());
        goalFileChooser.setFileFilter(new ExtFileFilter("goal", "Goal Model File"));
        goalFileChooser.setDialogTitle("Choose Goal file");
        goalFileChooser.setMultiSelectionEnabled(false);

        btnPath = new JButton("...");
        btnParse = new JButton("Parse");
        btnClose = new JButton("Close");

        JComponent contentPane = (JComponent) getContentPane();
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        GridBagHelper gh = new GridBagHelper(contentPane);

        gh.add(new JLabel("Goal file:"), 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.HORIZONTAL);
        gh.add(goalFileName,             1, 0, 5, 1, 1.0, 0.0, GridBagConstraints.HORIZONTAL);
        gh.add(btnPath,                  6, 0, 1, 1, 0.0, 0.0, GridBagConstraints.HORIZONTAL);

        gh.add(btnParse,                 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.HORIZONTAL);
        gh.add(btnClose,                 1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.HORIZONTAL);

        goalFileChooser.addActionListener((ActionEvent e) -> {
            String cmd = e.getActionCommand();

            if (JFileChooser.APPROVE_SELECTION.equals(cmd)) {
                File selected = goalFileChooser.getSelectedFile();
                if (selected != null) {
                    setSelectedFile(selected);
                    goalFileName.setText(selected.getAbsolutePath());
                }
            }
        });

        btnPath.addActionListener((ActionEvent e) ->
                goalFileChooser.showOpenDialog(GoalModelForm.this)
        );

        btnParse.addActionListener((ActionEvent e) -> {
            if (!validateForm()) {
                return;
            }
            parse();
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
            logWriter.println("[GoalModel] Selected file: " + getSelectedFile().getAbsolutePath());
            logWriter.flush();

            // TODO:
            // GoalLoader loader = new GoalLoader(session, getSelectedFile().getAbsolutePath(), logWriter, mainWindow);
            // boolean success = loader.run();

            showParseSuccess();
            close();

        } catch (Exception ex) {
            showParseError(ex.getMessage());
        }
    }
}