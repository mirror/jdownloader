package org.jdownloader.gui.views.downloads.action;

import org.appwork.uio.ConfirmDialogInterface;

public interface ConfirmDeleteLinksDialogInterface extends ConfirmDialogInterface {

    public boolean isDeleteFilesFromDiskEnabled();

    public boolean isDeleteFilesToRecycle();

    public boolean isRecycleSupported();
}
