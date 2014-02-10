package org.jdownloader.gui.dialog;

import org.appwork.uio.InputDialogInterface;
import org.appwork.uio.Out;

public interface AskDownloadPasswordDialogInterface extends InputDialogInterface {
    @Out
    public long getLinkID();

    @Out
    public String getLinkName();

    @Out
    public String getLinkHost();

    @Out
    public String getPackageName();
}