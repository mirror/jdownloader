package org.jdownloader.gui.views.components.packagetable.dragdrop;

import java.awt.datatransfer.Transferable;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import jd.controlling.IOEQ;
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
        return TransferHandler.MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        /*
         * get all selected packages and children and create a transferable if
         * possible
         */
        ArrayList<PackageType> packages = table.getSelectedPackages();
        ArrayList<ChildrenType> links = table.getSelectedChildren();
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

    protected boolean allowImport(ArrayList<PackageType> packages, ArrayList<ChildrenType> links) {
        return true;
    }

    @Override
    protected void exportDone(final JComponent source, final Transferable data, final int action) {
        if (data instanceof PackageControllerTableTransferable) {
            ((PackageControllerTableTransferable<?, ?>) data).exportDone();
        }
        super.exportDone(source, data, action);
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

    @SuppressWarnings("unchecked")
    @Override
    public boolean canImport(TransferSupport support) {
        if (!table.isOriginalOrder()) { return false; }
        PackageControllerTableTransferableContent<PackageType, ChildrenType> content = getContent(support);
        if (content == null) return false;
        if (!allowImport(content.packages, content.links)) return false;
        if (support.isDrop()) {
            /* dragdrop */
            JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
            boolean linksAvailable = content.links != null && content.links.size() > 0;
            boolean packagesAvailable = content.packages != null && content.packages.size() > 0;
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
                             * beforeElement is child->we are inside an expanded
                             * package
                             */
                            fp = (PackageType) ((AbstractPackageChildrenNode) beforeElement).getParentNode();
                        } else {
                            if (afterElement != null && afterElement instanceof AbstractPackageChildrenNode) {
                                /*
                                 * beforeElement is child->we are inside an
                                 * expanded package
                                 */
                                fp = (PackageType) ((AbstractPackageChildrenNode) afterElement).getParentNode();
                            }
                        }
                    }
                    if (fp == null && packagesAvailable == false) { return false; }
                    if (packagesAvailable) {
                        if (afterElement != null && !(afterElement instanceof AbstractPackageChildrenNode)) {
                            /*
                             * we dont allow packages get get insert inside a
                             * package
                             */
                            return false;
                        } else {
                            fp = null;
                        }
                        ArrayList<PackageType> packages = content.getPackages();
                        ;
                        /*
                         * we do not allow drop on items that are part of our
                         * transferable
                         */
                        for (PackageType p : packages) {
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
                        if (onElement instanceof AbstractPackageNode) {
                            /* only drop on packages is allowed */
                            ArrayList<PackageType> packages = content.getPackages();
                            /*
                             * we do not allow drop on items that are part of
                             * our transferable
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
                            ArrayList<ChildrenType> links = content.getLinks();
                            /*
                             * children are not allowed to drop on their own
                             * packages
                             */
                            for (ChildrenType link : links) {
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

    @SuppressWarnings("unchecked")
    @Override
    public boolean importData(TransferSupport support) {
        QueuePriority prio = org.appwork.utils.event.queue.Queue.QueuePriority.HIGH;
        PackageControllerTableTransferableContent<PackageType, ChildrenType> content = getContent(support);
        ArrayList<ChildrenType> tmpLinks = null;
        ArrayList<PackageType> tmpPackages = null;
        if (content != null) {
            tmpLinks = content.links;
            tmpPackages = content.packages;
            if (tmpLinks != null && tmpLinks.size() == 0) tmpLinks = null;
            if (tmpPackages != null && tmpPackages.size() == 0) tmpPackages = null;
        }
        if (tmpLinks == null && tmpPackages == null) { return false; }
        final ArrayList<ChildrenType> links = tmpLinks;
        final ArrayList<PackageType> packages = tmpPackages;
        if (support.isDrop()) {
            /* dragdrop */
            JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
            final boolean isInsert = dl.isInsertRow();
            int dropRow = dl.getRow();
            final Object afterElement = table.getExtTableModel().getObjectbyRow(dropRow);
            if (isInsert == false) {
                /* dropOn,merge */
                if (afterElement instanceof AbstractPackageNode) {
                    IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>(prio) {

                        @Override
                        protected Void run() throws RuntimeException {
                            table.getController().merge((PackageType) afterElement, links, packages, org.jdownloader.settings.staticreferences.CFG_LINKCOLLECTOR.DO_MERGE_TOP_BOTTOM.getValue());
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
        } else {
            /* paste */
        }
        return false;

    }

}
