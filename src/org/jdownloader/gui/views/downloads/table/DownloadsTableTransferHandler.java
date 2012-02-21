package org.jdownloader.gui.views.downloads.table;

import java.awt.datatransfer.Transferable;

import jd.controlling.ClipboardMonitoring;
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
    public boolean canImport(TransferSupport support) {
        boolean ret = super.canImport(support);
        if (ret == false && support.isDataFlavorSupported(PackageControllerTableTransferable.FLAVOR)) {
            /*
             * check if we have DownloadList stuff inside our Transferable, if
             * yes, we stop here to not fuxx up the dragdrop logic
             */
            return false;
        }
        if (ret == false) {
            /* check for LinkCrawler stuff */
            ret = ClipboardMonitoring.hasSupportedTransferData(support.getTransferable());
        }
        return ret;
    }

    @Override
    protected Transferable customizeTransferable(PackageControllerTableTransferable<FilePackage, DownloadLink> ret2) {
        return new DownloadsTransferable(ret2);
    }

    @Override
    public boolean importData(TransferSupport support) {
        boolean ret = super.importData(support);
        if (ret == false && support.isDataFlavorSupported(PackageControllerTableTransferable.FLAVOR)) {
            ClipboardMonitoring.processSupportedTransferData(support.getTransferable());
            ret = true;
        }
        return ret;
    }

}