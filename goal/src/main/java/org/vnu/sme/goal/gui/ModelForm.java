package org.vnu.sme.goal.gui;

import java.io.File;

public interface ModelForm {

    File getSelectedFile();

    void setSelectedFile(File file);

    String getModelName();

    void setModelName(String modelName);

    void close();

    void parse();

    boolean validateForm();

    void showFileNullError();

    void showFileNotExistsError();

    void showFileEmptyError();

    void showModelEmptyError();

    void showParseSuccess();

    void showParseError(String message);
}