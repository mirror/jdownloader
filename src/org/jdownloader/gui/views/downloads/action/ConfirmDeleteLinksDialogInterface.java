package org.jdownloader.gui.views.downloads.action;

import org.appwork.utils.swing.dialog.ConfirmDialogInterface;

public interface ConfirmDeleteLinksDialogInterface extends ConfirmDialogInterface {

    public boolean isDeleteFilesFromDiskEnabled();

    public boolean isDeleteFilesToRecycle();

    public boolean isRecycleSupported();
}
