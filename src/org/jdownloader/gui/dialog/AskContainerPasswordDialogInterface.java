package org.jdownloader.gui.dialog;

import java.io.File;

import org.appwork.uio.InputDialogInterface;
import org.appwork.uio.Out;

public interface AskContainerPasswordDialogInterface extends InputDialogInterface {
    @Out
    public File getFile();
}