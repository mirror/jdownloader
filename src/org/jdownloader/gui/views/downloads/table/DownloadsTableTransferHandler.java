package org.jdownloader.gui.views.downloads.table;

import java.awt.datatransfer.Transferable;

import jd.controlling.ClipboardMonitoring;
import jd.controlling.linkcollector.LinkOrigin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.components.packagetable.dragdrop.PackageControllerTableTransferHandler;
import org.jdownloader.gui.views.components.packagetable.dragdrop.PackageControllerTableTransferable;

public class DownloadsTableTransferHandler extends PackageControllerTableTransferHandler<FilePackage, DownloadLink> {

    public DownloadsTableTransferHandler(PackageControllerTable<FilePackage, DownloadLink> table) {
        super(table);
    }

    @Override
    protected Transferable customizeTransferable(PackageControllerTableTransferable<FilePackage, DownloadLink> ret2) {
        return new DownloadsTransferable(ret2);
    }

    @Override
    protected boolean importTransferable(TransferSupport support) {
        ClipboardMonitoring.processSupportedTransferData(support.getTransferable(), support.isDrop() ? LinkOrigin.PASTE_LINKS_ACTION : LinkOrigin.CLIPBOARD);
        return true;
    }

    @Override
    protected boolean canImportTransferable(TransferSupport support) {
        return ClipboardMonitoring.hasSupportedTransferData(support.getTransferable());
    }

}