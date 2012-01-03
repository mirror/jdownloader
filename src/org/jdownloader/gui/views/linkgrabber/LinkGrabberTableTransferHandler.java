package org.jdownloader.gui.views.linkgrabber;

import java.awt.datatransfer.Transferable;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import jd.controlling.ClipboardMonitoring;
import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging.Log;

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

    private boolean canImportLinkCollector(TransferSupport support) {
        if (!table.isOriginalOrder()) { return false; }
        if (support.isDrop()) {
            /* dragdrop */
            JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
            boolean linksAvailable = support.isDataFlavorSupported(CrawledLinksDataFlavor.Flavor);
            boolean packagesAvailable = support.isDataFlavorSupported(CrawledPackagesDataFlavor.Flavor);
            if (linksAvailable || packagesAvailable) {
                // if
                // (!LinkGrabberTransferable.isVersionOkay(support.getTransferable()))
                // {
                /*
                 * packagecontroller has newer version, we no longer allow this
                 * copy/paste to happen
                 */
                /* DISABLED: as it will only confuse the customer! */
                // return false;
                // }
            }
            boolean isInsert = dl.isInsertRow();
            int dropRow = dl.getRow();
            if (isInsert) {
                /* handle insert,move here */
                Object beforeElement = table.getExtTableModel().getObjectbyRow(dropRow - 1);
                Object afterElement = table.getExtTableModel().getObjectbyRow(dropRow);
                if (beforeElement == null && afterElement == null) {
                    /* no before element, table is empty */
                    return true;
                }
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
                /* handle dropOn,merge here */
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
    public boolean canImport(TransferSupport support) {
        /* check for LinkCollector DragDrop stuff */
        boolean ret = canImportLinkCollector(support);
        if (ret == false) {
            /* check for LinkCrawler stuff */
            ret = ClipboardMonitoring.hasSupportedTransferData(support.getTransferable());
        }
        return ret;
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
        final ArrayList<CrawledLink> links;
        final ArrayList<CrawledPackage> packages;
        QueuePriority prio = Queue.QueuePriority.HIGH;
        if (support.isDataFlavorSupported(CrawledLinksDataFlavor.Flavor)) {
            links = LinkGrabberTransferable.getChildren(support.getTransferable());
            if (links.size() > 10) prio = Queue.QueuePriority.NORM;
        } else {
            links = null;
        }
        if (support.isDataFlavorSupported(CrawledPackagesDataFlavor.Flavor)) {
            packages = LinkGrabberTransferable.getPackages(support.getTransferable());
            if (packages.size() > 3) prio = Queue.QueuePriority.NORM;
        } else {
            packages = null;
        }

        if (packages == null && links == null) {
            /* lets try LinkCrawling stuff */
            ClipboardMonitoring.processSupportedTransferData(support.getTransferable());
            return true;
        }
        if (support.isDrop()) {
            /* dragdrop */
            JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
            final boolean isInsert = dl.isInsertRow();
            int dropRow = dl.getRow();
            final Object afterElement = table.getExtTableModel().getObjectbyRow(dropRow);
            if (isInsert == false) {
                /* dropOn,merge */

                if (afterElement instanceof CrawledPackage) {
                    IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>(prio) {

                        @Override
                        protected Void run() throws RuntimeException {
                            LinkCollector.getInstance().merge((CrawledPackage) afterElement, links, packages);
                            return null;
                        }
                    });
                } else {
                    Log.exception(new WTFException("this should not happen!"));
                }
            } else {
                final Object beforeElement = table.getExtTableModel().getObjectbyRow(dropRow - 1);
                /* insert,move */
                if (packages != null) {
                    /* move packages */
                    final CrawledPackage dest;
                    if (beforeElement == null) {
                        dest = null;
                    } else if (beforeElement instanceof CrawledPackage) {
                        dest = (CrawledPackage) beforeElement;
                    } else if (beforeElement instanceof CrawledLink) {
                        dest = ((CrawledLink) beforeElement).getParentNode();
                    } else {
                        dest = null;
                    }
                    IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>(prio) {

                        @Override
                        protected Void run() throws RuntimeException {
                            LinkCollector.getInstance().move(packages, dest);
                            return null;
                        }

                    });
                } else if (links != null) {
                    /* move links */
                    final CrawledPackage destP;
                    final CrawledLink afterL;
                    if (beforeElement != null) {
                        if (beforeElement instanceof CrawledLink) {
                            afterL = (CrawledLink) beforeElement;
                            destP = afterL.getParentNode();
                        } else if (beforeElement instanceof CrawledPackage) {
                            afterL = null;
                            destP = ((CrawledPackage) beforeElement);
                        } else {
                            afterL = null;
                            destP = null;
                        }
                    } else {
                        afterL = null;
                        destP = null;
                    }
                    IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>(prio) {

                        @Override
                        protected Void run() throws RuntimeException {
                            LinkCollector.getInstance().move(links, destP, afterL);
                            return null;
                        }
                    });

                }
            }
        } else {
            /* paste */
        }
        return true;
    }

    @Override
    protected void exportDone(final JComponent source, final Transferable data, final int action) {
        super.exportDone(source, data, action);
    }

}
