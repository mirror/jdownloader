package org.jdownloader.gui.views.components.packagetable.dragdrop;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import jd.controlling.IOEQ;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging.Log;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;

public abstract class PackageControllerTableTransferHandler<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends TransferHandler {

    /**
     * 
     */
    private static final long                                         serialVersionUID = 4546100125368290556L;
    final protected PackageControllerTable<PackageType, ChildrenType> table;

    public PackageControllerTableTransferHandler(PackageControllerTable<PackageType, ChildrenType> table) {
        this.table = table;
    }

    @Override
    public int getSourceActions(JComponent c) {
        /* we only allow to move/cut&paste packages/children by default */
        return TransferHandler.COPY_OR_MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        /*
         * get all selected packages and children and create a transferable if possible
         */
        java.util.List<PackageType> packages = table.getSelectedPackages();
        java.util.List<ChildrenType> links = table.getSelectedChildren();
        Transferable ret = null;
        if ((links != null && links.size() > 0) || (packages != null && packages.size() > 0)) {
            PackageControllerTableTransferable<PackageType, ChildrenType> ret2 = new PackageControllerTableTransferable<PackageType, ChildrenType>(packages, links, table);
            ret = customizeTransferable(ret2);
        }
        return ret;
    }

    /* here you can customize the Transferable or create new one if needed */
    protected Transferable customizeTransferable(PackageControllerTableTransferable<PackageType, ChildrenType> ret2) {
        return ret2;
    }

    protected boolean allowImport(java.util.List<PackageType> packages, java.util.List<ChildrenType> links) {
        return true;
    }

    @Override
    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        /* default Controller only handles move! */
        super.exportAsDrag(comp, e, TransferHandler.MOVE);
    }

    @Override
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
        /* default Controller only handles move! */
        super.exportToClipboard(comp, clip, TransferHandler.MOVE);
    }

    @SuppressWarnings("unchecked")
    protected PackageControllerTableTransferableContent<PackageType, ChildrenType> getContent(TransferSupport support) {
        if (!support.isDataFlavorSupported(PackageControllerTableTransferable.FLAVOR)) return null;
        try {
            PackageControllerTableTransferableContent<?, ?> content = (PackageControllerTableTransferableContent<?, ?>) support.getTransferable().getTransferData(PackageControllerTableTransferable.FLAVOR);
            if (content.table != table) return null;
            return (PackageControllerTableTransferableContent<PackageType, ChildrenType>) content;
        } catch (final Throwable e) {
            Log.exception(e);
            return null;
        }
    }

    protected boolean canImportPackageControllerTransferable(TransferSupport support) {
        if (!table.isOriginalOrder() || table.getPackageControllerTableModel().isFilteredView()) { return false; }
        PackageControllerTableTransferableContent<PackageType, ChildrenType> content = getContent(support);
        if (content == null) return false;
        if (!allowImport(content.packages, content.links)) return false;
        Object onElement = null;
        boolean linksAvailable = content.links != null && content.links.size() > 0;
        boolean packagesAvailable = content.packages != null && content.packages.size() > 0;
        if (support.isDrop()) {
            /* dragdrop */
            JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
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
                    /* we don't allow children/packages to get mixed on insert */
                    return false;
                }
                if (linksAvailable || packagesAvailable) {
                    PackageType fp = null;
                    if (beforeElement != null) {
                        if (beforeElement instanceof AbstractPackageChildrenNode) {
                            /*
                             * beforeElement is child->we are inside an expanded package
                             */
                            fp = (PackageType) ((AbstractPackageChildrenNode) beforeElement).getParentNode();
                        } else {
                            if (afterElement != null && afterElement instanceof AbstractPackageChildrenNode) {
                                /*
                                 * beforeElement is child->we are inside an expanded package
                                 */
                                fp = (PackageType) ((AbstractPackageChildrenNode) afterElement).getParentNode();
                            }
                        }
                    }
                    if (fp == null && packagesAvailable == false) { return false; }
                    if (packagesAvailable) {
                        if (afterElement != null && !(afterElement instanceof AbstractPackageNode)) {
                            /*
                             * we dont allow packages get get insert inside a package
                             */
                            return false;
                        } else {
                            fp = null;
                        }
                        java.util.List<PackageType> packages = content.getPackages();
                        ;
                        /*
                         * we do not allow drop on items that are part of our transferable
                         */
                        for (PackageType p : packages) {
                            if (p == fp) { return false; }
                        }
                    }
                }
            } else {
                /* handle dropOn,merge here */
                onElement = table.getExtTableModel().getObjectbyRow(dropRow);
            }
        } else {
            /* Paste */
            java.util.List<AbstractNode> firstSelected = table.getExtTableModel().getSelectedObjects(1);
            if (firstSelected.size() > 0) {
                onElement = firstSelected.get(0);
            }
        }
        if (onElement != null) {
            /* on 1 element */
            if (packagesAvailable) {
                if (onElement instanceof AbstractPackageNode) {
                    /* only drop on packages is allowed */
                    java.util.List<PackageType> packages = content.getPackages();
                    /*
                     * we do not allow drop on items that are part of our transferable
                     */
                    for (PackageType p : packages) {
                        if (p == onElement) return false;
                    }
                } else {
                    /*
                     * packages are not allowed to drop on children
                     */
                    return false;
                }
            }
            if (linksAvailable) {
                if (onElement instanceof AbstractPackageChildrenNode) {
                    /*
                     * children are not allowed to drop on children
                     */
                    return false;
                } else if (onElement instanceof AbstractPackageNode) {
                    java.util.List<ChildrenType> links = content.getLinks();
                    /*
                     * children are not allowed to drop on their own packages
                     */
                    for (ChildrenType link : links) {
                        if (link.getParentNode() == onElement) return false;
                    }
                }
            }
        }
        return true;
    }

    abstract protected boolean importTransferable(TransferSupport support);

    abstract protected boolean canImportTransferable(TransferSupport support);

    @Override
    public boolean canImport(TransferSupport support) {
        boolean ret = canImportPackageControllerTransferable(support);
        if (ret == false && !support.isDataFlavorSupported(PackageControllerTableTransferable.FLAVOR)) {
            ret = canImportTransferable(support);
        }
        return ret;
    }

    protected boolean importPackageControllerTransferable(TransferSupport support) {
        if (canImportPackageControllerTransferable(support) == false) return false;
        QueuePriority prio = org.appwork.utils.event.queue.Queue.QueuePriority.HIGH;
        PackageControllerTableTransferableContent<PackageType, ChildrenType> content = getContent(support);
        java.util.List<ChildrenType> tmpLinks = null;
        java.util.List<PackageType> tmpPackages = null;
        if (content != null) {
            tmpLinks = content.links;
            tmpPackages = content.packages;
            if (tmpLinks != null && tmpLinks.size() == 0) tmpLinks = null;
            if (tmpPackages != null && tmpPackages.size() == 0) tmpPackages = null;
        }
        if (tmpLinks == null && tmpPackages == null) { return false; }
        final java.util.List<ChildrenType> links = tmpLinks;
        final java.util.List<PackageType> packages = tmpPackages;
        boolean isInsert = false;
        Object afterElement = null;
        Object beforeElement = null;
        if (support.isDrop()) {
            /* dragdrop */
            JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
            isInsert = dl.isInsertRow();
            int dropRow = dl.getRow();
            afterElement = table.getExtTableModel().getObjectbyRow(dropRow);
            if (isInsert) {
                beforeElement = table.getExtTableModel().getObjectbyRow(dropRow - 1);
            }
        } else {
            /* paste */
            java.util.List<AbstractNode> firstSelected = table.getExtTableModel().getSelectedObjects(1);
            if (firstSelected.size() > 0) {
                afterElement = firstSelected.get(0);
                if (afterElement instanceof AbstractPackageChildrenNode) {
                    afterElement = ((AbstractPackageChildrenNode) afterElement).getParentNode();
                }
            }
        }
        if (isInsert == false || !support.isDrop()) {
            /* dropOn,merge or paste */
            if (afterElement != null && afterElement instanceof AbstractPackageNode) {
                final Object element = afterElement;
                IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>(prio) {

                    @Override
                    protected Void run() throws RuntimeException {
                        if (((PackageType) element).getCurrentSorter() == null) {
                            table.getController().merge((PackageType) element, links, packages, org.jdownloader.settings.staticreferences.CFG_LINKCOLLECTOR.DO_MERGE_TOP_BOTTOM.getValue() ? MergePosition.BOTTOM : MergePosition.TOP);
                        } else {
                            // we have a sorter.neither top nor bottom but sorted insert
                            table.getController().merge((PackageType) element, links, packages, MergePosition.SORTED);

                        }
                        return null;
                    }
                });
            } else {
                Log.exception(new WTFException("this should not happen!"));
            }
        } else {
            /* insert,move */
            if (packages != null) {
                /* move packages */
                final PackageType dest;
                if (beforeElement == null) {
                    dest = null;
                } else if (beforeElement instanceof AbstractPackageNode) {
                    dest = (PackageType) beforeElement;
                } else if (beforeElement instanceof AbstractPackageChildrenNode) {
                    dest = (PackageType) ((ChildrenType) beforeElement).getParentNode();
                } else {
                    dest = null;
                }
                IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>(prio) {

                    @Override
                    protected Void run() throws RuntimeException {
                        table.getController().move(packages, dest);
                        return null;
                    }
                });
            } else if (links != null) {
                /* move links */
                final PackageType destP;
                final ChildrenType afterL;
                if (beforeElement != null) {
                    if (beforeElement instanceof AbstractPackageChildrenNode) {
                        afterL = (ChildrenType) beforeElement;
                        destP = afterL.getParentNode();
                    } else if (beforeElement instanceof AbstractPackageNode) {
                        afterL = null;
                        destP = ((PackageType) beforeElement);
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
                        table.getController().move(links, destP, afterL);
                        return null;
                    }
                });
            }
        }
        content.exportDone();
        return true;

    }

    @Override
    public boolean importData(TransferSupport support) {
        boolean ret = importPackageControllerTransferable(support);
        if (ret == false && !support.isDataFlavorSupported(PackageControllerTableTransferable.FLAVOR)) {
            ret = importTransferable(support);
        }
        return ret;
    }

}
