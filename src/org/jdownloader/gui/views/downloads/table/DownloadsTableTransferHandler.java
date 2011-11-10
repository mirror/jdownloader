package org.jdownloader.gui.views.downloads.table;

import java.awt.datatransfer.Transferable;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public class DownloadsTableTransferHandler extends TransferHandler {

    /**
     * 
     */
    private static final long serialVersionUID = 7250267957897246993L;
    private DownloadsTable    table;

    public DownloadsTableTransferHandler(DownloadsTable downloadsTable) {
        this.table = downloadsTable;
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
            boolean linksAvailable = support.isDataFlavorSupported(DownloadLinksDataFlavor.Flavor);
            boolean packagesAvailable = support.isDataFlavorSupported(FilePackagesDataFlavor.Flavor);
            if (linksAvailable || packagesAvailable) {
                if (!DownloadsTableTransferable.isVersionOkay(support.getTransferable())) {
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
                    FilePackage fp = null;
                    if (beforeElement != null) {
                        if (beforeElement instanceof DownloadLink) {
                            /*
                             * beforeElement is DownloadLink->we are inside an
                             * expanded package
                             */
                            fp = ((DownloadLink) beforeElement).getFilePackage();
                        } else {
                            if (afterElement != null && afterElement instanceof DownloadLink) {
                                /*
                                 * beforeElement is DownloadLink->we are inside
                                 * an expanded package
                                 */
                                fp = ((DownloadLink) afterElement).getFilePackage();
                            }
                        }
                    }
                    if (fp == null && packagesAvailable == false) { return false; }
                    if (packagesAvailable) {
                        if (afterElement != null && !(afterElement instanceof FilePackage)) {
                            /*
                             * we dont allow packages get get insert inside a
                             * package
                             */
                            return false;
                        } else {
                            fp = null;
                        }
                        ArrayList<FilePackage> packages = DownloadsTableTransferable.getPackages(support.getTransferable());
                        /*
                         * we do not allow drop on items that are part of our
                         * transferable
                         */
                        for (FilePackage p : packages) {
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
                        if (onElement instanceof FilePackage) {
                            /* only drop on filepackages is allowed */
                            ArrayList<FilePackage> packages = DownloadsTableTransferable.getPackages(support.getTransferable());
                            /*
                             * we do not allow drop on items that are part of
                             * our transferable
                             */
                            for (FilePackage p : packages) {
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
                        ArrayList<DownloadLink> links = DownloadsTableTransferable.getChildren(support.getTransferable());
                        if (onElement instanceof DownloadLink) {
                            /*
                             * downloadlinks are not allowed to drop on
                             * downloadlinks
                             */
                            return false;
                        } else if (onElement instanceof FilePackage) {
                            /*
                             * downloadlinks are not allowed to drop on their
                             * own filepackage
                             */
                            for (DownloadLink link : links) {
                                if (link.getFilePackage() == onElement) return false;
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
        ArrayList<FilePackage> packages = table.getSelectedPackages();
        ArrayList<DownloadLink> links = table.getSelectedChildren();
        if ((links != null && links.size() > 0) || (packages != null && packages.size() > 0)) { return new DownloadsTableTransferable(packages, links, DownloadController.getInstance().getPackageControllerChanges()); }
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
