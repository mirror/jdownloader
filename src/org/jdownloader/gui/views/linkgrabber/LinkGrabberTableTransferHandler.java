package org.jdownloader.gui.views.linkgrabber;

import java.awt.datatransfer.Transferable;

import jd.controlling.ClipboardMonitoring;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.components.packagetable.dragdrop.PackageControllerTableTransferHandler;
import org.jdownloader.gui.views.components.packagetable.dragdrop.PackageControllerTableTransferable;

public class LinkGrabberTableTransferHandler extends PackageControllerTableTransferHandler<CrawledPackage, CrawledLink> {

    public LinkGrabberTableTransferHandler(PackageControllerTable<CrawledPackage, CrawledLink> table) {
        super(table);
    }

    @Override
    public boolean canImport(TransferSupport support) {
        boolean ret = super.canImport(support);
        if (ret == false && support.isDataFlavorSupported(PackageControllerTableTransferable.FLAVOR)) {
            /*
             * check if we have LinkCollector stuff inside our Transferable, if
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
    protected Transferable customizeTransferable(PackageControllerTableTransferable<CrawledPackage, CrawledLink> ret2) {
        return new LinkGrabberTransferable(ret2);
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
