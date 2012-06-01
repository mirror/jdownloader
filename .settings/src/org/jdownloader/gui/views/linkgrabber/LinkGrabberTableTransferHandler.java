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
    protected Transferable customizeTransferable(PackageControllerTableTransferable<CrawledPackage, CrawledLink> ret2) {
        return new LinkGrabberTransferable(ret2);
    }

    @Override
    protected boolean importTransferable(TransferSupport support) {
        ClipboardMonitoring.processSupportedTransferData(support.getTransferable());
        return true;
    }

    @Override
    protected boolean canImportTransferable(TransferSupport support) {
        return ClipboardMonitoring.hasSupportedTransferData(support.getTransferable());
    }
}
