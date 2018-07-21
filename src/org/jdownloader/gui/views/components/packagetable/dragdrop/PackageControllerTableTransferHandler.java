package org.jdownloader.gui.views.components.packagetable.dragdrop;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

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

    private final AtomicReference<String> transferableStringContent = new AtomicReference<String>(null);

    public void setTransferableStringContent(String content) {
        transferableStringContent.set(content);
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        /*
         * get all selected packages and children and create a transferable if possible
         */
        final SelectionInfo<PackageType, ChildrenType> selectionInfo = table.getSelectionInfo(true, true);
        final String transferableStringContent = this.transferableStringContent.getAndSet(null);
        if (selectionInfo.getPackageViews().size() > 0) {
            final PackageControllerTableTransferable<PackageType, ChildrenType> ret2 = new PackageControllerTableTransferable<PackageType, ChildrenType>(selectionInfo, table, transferableStringContent);
            return customizeTransferable(ret2);
        } else {
            return null;
        }
    }

    /* here you can customize the Transferable or create new one if needed */
    protected Transferable customizeTransferable(PackageControllerTableTransferable<PackageType, ChildrenType> ret2) {
        return ret2;
    }

    protected boolean allowImport(SelectionInfo<PackageType, ChildrenType> selectionInfo) {
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
    protected SelectionInfo<PackageType, ChildrenType> getSelectionInfo(TransferSupport support) {
        if (!support.isDataFlavorSupported(PackageControllerTableTransferable.FLAVOR)) {
            return null;
        }
        try {
            final SelectionInfo<?, ?> content = (SelectionInfo<?, ?>) support.getTransferable().getTransferData(PackageControllerTableTransferable.FLAVOR);
            if (content.getController() != table.getController()) {
                return null;
            }
            return (SelectionInfo<PackageType, ChildrenType>) content;
        } catch (final Throwable e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            return null;
        }
    }

    protected boolean canImportPackageControllerTransferable(TransferSupport support) {
        if (!table.isOriginalOrder() || table.getModel().isFilteredView()) {
            return false;
        }
        final SelectionInfo<PackageType, ChildrenType> selectionInfo = getSelectionInfo(support);
        if (selectionInfo == null || selectionInfo.isEmpty()) {
            return false;
        }
        if (!allowImport(selectionInfo)) {
            return false;
        }
        Object onElement = null;
        List<ChildrenType> links = getSelectedChildren(selectionInfo);
        List<PackageType> packages = getSelectedPackages(selectionInfo);
        boolean linksAvailable = links != null && links.size() > 0;
        boolean packagesAvailable = packages != null && packages.size() > 0;
        if (support.isDrop()) {
            /* dragdrop */
            JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
            boolean isInsert = dl.isInsertRow();
            int dropRow = dl.getRow();
            if (isInsert) {
                /* handle insert,move here */
                Object beforeElement = table.getModel().getObjectbyRow(dropRow - 1);
                Object afterElement = table.getModel().getObjectbyRow(dropRow);
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
                    if (fp == null && packagesAvailable == false) {
                        return false;
                    }
                    if (packagesAvailable) {
                        if (afterElement != null && !(afterElement instanceof AbstractPackageNode)) {
                            /*
                             * we dont allow packages get get insert inside a package
                             */
                            return false;
                        } else {
                            fp = null;
                        }
                        /*
                         * we do not allow drop on items that are part of our transferable
                         */
                        for (PackageType p : packages) {
                            if (p == fp) {
                                return false;
                            }
                        }
                    }
                }
            } else {
                /* handle dropOn,merge here */
                onElement = table.getModel().getObjectbyRow(dropRow);
            }
        } else {
            /* Paste */
            java.util.List<AbstractNode> firstSelected = table.getModel().getSelectedObjects(1);
            if (firstSelected.size() > 0) {
                onElement = firstSelected.get(0);
            }
        }
        if (onElement != null) {
            /* on 1 element */
            if (packagesAvailable) {
                if (onElement instanceof AbstractPackageNode) {
                    /* only drop on packages is allowed */
                    /*
                     * we do not allow drop on items that are part of our transferable
                     */
                    for (PackageType p : packages) {
                        if (p == onElement) {
                            return false;
                        }
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
                    /*
                     * children are not allowed to drop on their own packages
                     */
                    for (ChildrenType link : links) {
                        if (link.getParentNode() == onElement) {
                            return false;
                        }
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

    private List<ChildrenType> getSelectedChildren(SelectionInfo<PackageType, ChildrenType> selectionInfo) {
        final ArrayList<ChildrenType> ret = new ArrayList<ChildrenType>();
        for (final PackageView<PackageType, ChildrenType> packageView : selectionInfo.getPackageViews()) {
            if (packageView.isExpanded()) {
                ret.addAll(packageView.getSelectedChildren());
            }
        }
        return ret;
    }

    private List<PackageType> getSelectedPackages(SelectionInfo<PackageType, ChildrenType> selectionInfo) {
        final List<PackageView<PackageType, ChildrenType>> packageViews = selectionInfo.getPackageViews();
        final ArrayList<PackageType> ret = new ArrayList<PackageType>(packageViews.size());
        for (final PackageView<PackageType, ChildrenType> packageView : packageViews) {
            if (packageView.isPackageSelected()) {
                ret.add(packageView.getPackage());
            }
        }
        return ret;
    }

    protected boolean importPackageControllerTransferable(TransferSupport support) {
        if (canImportPackageControllerTransferable(support) == false) {
            return false;
        }
        QueuePriority prio = org.appwork.utils.event.queue.Queue.QueuePriority.HIGH;
        final SelectionInfo<PackageType, ChildrenType> selectionInfo = getSelectionInfo(support);
        java.util.List<ChildrenType> tmpLinks = null;
        java.util.List<PackageType> tmpPackages = null;
        if (selectionInfo != null) {
            tmpLinks = getSelectedChildren(selectionInfo);
            tmpPackages = getSelectedPackages(selectionInfo);
            if (tmpLinks != null && tmpLinks.size() == 0) {
                tmpLinks = null;
            }
            if (tmpPackages != null && tmpPackages.size() == 0) {
                tmpPackages = null;
            }
        }
        if (tmpLinks == null && tmpPackages == null) {
            return false;
        }
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
            afterElement = table.getModel().getObjectbyRow(dropRow);
            if (isInsert) {
                beforeElement = table.getModel().getObjectbyRow(dropRow - 1);
            }
        } else {
            /* paste */
            java.util.List<AbstractNode> firstSelected = table.getModel().getSelectedObjects(1);
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
                table.getController().getQueue().add(new QueueAction<Void, RuntimeException>(prio) {
                    @Override
                    protected Void run() throws RuntimeException {
                        if (((PackageType) element).getCurrentSorter() == null) {
                            table.getController().merge((PackageType) element, links, packages, org.jdownloader.settings.staticreferences.CFG_LINKCOLLECTOR.DO_MERGE_TOP_BOTTOM.isEnabled() ? MergePosition.BOTTOM : MergePosition.TOP);
                        } else {
                            // we have a sorter.neither top nor bottom but sorted insert
                            table.getController().merge((PackageType) element, links, packages, MergePosition.SORTED);
                        }
                        return null;
                    }
                });
            } else {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(new WTFException("this should not happen!"));
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
                    dest = ((ChildrenType) beforeElement).getParentNode();
                } else {
                    dest = null;
                }
                table.getController().getQueue().add(new QueueAction<Void, RuntimeException>(prio) {
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
                table.getController().getQueue().add(new QueueAction<Void, RuntimeException>(prio) {
                    @Override
                    protected Void run() throws RuntimeException {
                        table.getController().move(links, destP, afterL);
                        return null;
                    }
                });
            }
        }
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
