package org.jdownloader.gui.views.linkgrabber;

import java.awt.datatransfer.Transferable;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

public class LinkGrabberTableTransferHandler extends TransferHandler {

    /**
     * 
     */
    private static final long serialVersionUID = 7250267957897246993L;
    private LinkGrabberTable  table;

    public LinkGrabberTableTransferHandler(LinkGrabberTable grabberTable) {
        this.table = grabberTable;
    }

    @Override
    public int getSourceActions(JComponent c) {
        /* we only allow to move links/packages */
        return TransferHandler.MOVE;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        if (!table.isOriginalOrder()) { return false; }
        if (support.isDrop()) {
            /* we want to drop something */
            JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
            boolean linksAvailable = support.isDataFlavorSupported(CrawledLinksDataFlavor.Flavor);
            boolean packagesAvailable = support.isDataFlavorSupported(CrawledPackagesDataFlavor.Flavor);
            if (linksAvailable || packagesAvailable) {
                if (!LinkGrabberTransferable.isVersionOkay(support.getTransferable())) {
                    /*
                     * packagecontroller has newer version, we no longer allow
                     * this copy/paste to happen
                     */
                    return false;
                }
            }
            boolean isInsert = dl.isInsertRow();
            int dropRow = dl.getRow();
            if (isInsert) {
                /* handle insert here */
                Object beforeElement = table.getExtTableModel().getObjectbyRow(dropRow - 1);
                if (beforeElement == null) {
                    /* no before element, table is empty */
                    return true;
                }
                Object afterElement = table.getExtTableModel().getObjectbyRow(dropRow);
                if (linksAvailable && packagesAvailable) {
                    /* we dont allow links/packages to get mixed on insert */
                    return false;
                }
                if (linksAvailable || packagesAvailable) {
                    CrawledPackage fp = null;
                    if (beforeElement != null) {
                        if (beforeElement instanceof CrawledLink) {
                            /*
                             * beforeElement is DownloadLink->we are inside an
                             * expanded package
                             */
                            fp = ((CrawledLink) beforeElement).getParentNode();
                        } else {
                            if (afterElement != null && afterElement instanceof CrawledLink) {
                                /*
                                 * beforeElement is DownloadLink->we are inside
                                 * an expanded package
                                 */
                                fp = ((CrawledLink) afterElement).getParentNode();
                            }
                        }
                    }
                    if (fp == null && packagesAvailable == false) { return false; }
                    if (packagesAvailable) {
                        if (afterElement != null && !(afterElement instanceof CrawledPackage)) {
                            /*
                             * we dont allow packages get get insert inside a
                             * package
                             */
                            return false;
                        } else {
                            fp = null;
                        }
                        ArrayList<CrawledPackage> packages = LinkGrabberTransferable.getPackages(support.getTransferable());
                        /*
                         * we do not allow drop on items that are part of our
                         * transferable
                         */
                        for (CrawledPackage p : packages) {
                            if (p == fp) return false;
                        }
                    }
                }
            } else {
                /* handle dropOn here */
                Object onElement = table.getExtTableModel().getObjectbyRow(dropRow);
                if (onElement != null) {
                    /* on 1 element */
                    if (packagesAvailable) {
                        if (onElement instanceof CrawledPackage) {
                            /* only drop on filepackages is allowed */
                            ArrayList<CrawledPackage> packages = LinkGrabberTransferable.getPackages(support.getTransferable());
                            /*
                             * we do not allow drop on items that are part of
                             * our transferable
                             */
                            for (CrawledPackage p : packages) {
                                if (p == onElement) return false;
                            }
                        } else {
                            /*
                             * filepackages are not allowed to drop on
                             * downloadlinks
                             */
                            return false;
                        }
                    }
                    if (linksAvailable) {
                        ArrayList<CrawledLink> links = LinkGrabberTransferable.getChildren(support.getTransferable());
                        if (onElement instanceof CrawledLink) {
                            /*
                             * downloadlinks are not allowed to drop on
                             * downloadlinks
                             */
                            return false;
                        } else if (onElement instanceof CrawledPackage) {
                            /*
                             * downloadlinks are not allowed to drop on their
                             * own filepackage
                             */
                            for (CrawledLink link : links) {
                                if (link.getParentNode() == onElement) return false;
                            }
                        }
                    }
                }
            }
        } else {
            /* paste action */
        }
        return true;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        /*
         * get all selected filepackages and links and create a transferable if
         * possible
         */
        ArrayList<CrawledPackage> packages = table.getSelectedPackages();
        ArrayList<CrawledLink> links = table.getSelectedChildren();
        if ((links != null && links.size() > 0) || (packages != null && packages.size() > 0)) { return new LinkGrabberTransferable(packages, links, LinkCollector.getInstance().getPackageControllerChanges()); }
        return null;
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) return false;
        return false;
    }

    @Override
    protected void exportDone(final JComponent source, final Transferable data, final int action) {
        super.exportDone(source, data, action);
    }

}
