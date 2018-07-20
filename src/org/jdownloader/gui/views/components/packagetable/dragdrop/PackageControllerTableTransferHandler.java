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

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;

public abstract class PackageControllerTableTransferHandler<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends TransferHandler {
    /**
     *
     */
    private static final long                                       serialVersionUID = 4546100125368290556L;
    final private PackageControllerTable<PackageType, ChildrenType> table;
    final private PackageController<PackageType, ChildrenType>      controller;

    public PackageControllerTableTransferHandler(PackageControllerTable<PackageType, ChildrenType> table) {
        this.table = table;
        controller = table.getController();
    }

    protected PackageController<PackageType, ChildrenType> getController() {
        return controller;
    }

    @Override
    public int getSourceActions(JComponent c) {
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

    @Override
    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        super.exportAsDrag(comp, e, action);
    }

    @Override
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
        super.exportToClipboard(comp, clip, action);
    }

    protected SelectionInfo<?, ?> getSelectionInfo(final TransferSupport support) {
        if (support.isDataFlavorSupported(PackageControllerTableTransferable.FLAVOR)) {
            try {
                final SelectionInfo<?, ?> ret = (SelectionInfo<?, ?>) support.getTransferable().getTransferData(PackageControllerTableTransferable.FLAVOR);
                return ret;
            } catch (final Throwable e) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            }
        }
        return null;
    }

    protected Boolean canImportSelectionInfo(TransferSupport support) {
        return Boolean.TRUE;
    }

    abstract protected boolean importTransferable(TransferSupport support);

    abstract protected boolean canImportTransferable(TransferSupport support);

    @Override
    public boolean canImport(TransferSupport support) {
        final Boolean ret = canImportSelectionInfo(support);
        if (ret == null) {
            return canImportTransferable(support);
        } else {
            return ret;
        }
    }

    @Override
    public boolean importData(TransferSupport support) {
        final Boolean ret = importSelectionInfo(support);
        if (ret == null) {
            return importTransferable(support);
        } else {
            return ret;
        }
    }

    protected Boolean importSelectionInfo(final TransferSupport support, final SelectionInfo<?, ?> selectionInfo, final boolean isFilteredView, final AbstractNode beforeElement, final AbstractNode afterElement) {
        if (selectionInfo != null && selectionInfo.getController() == getController()) {
            if (!support.isDrop() || !((JTable.DropLocation) support.getDropLocation()).isInsertRow()) {
                // TODO
            } else {
                getController().getQueue().add(new QueueAction<Void, RuntimeException>(org.appwork.utils.event.queue.Queue.QueuePriority.HIGH) {
                    @Override
                    protected Void run() throws RuntimeException {
                        boolean hasSelectedPackage = false;
                        boolean hasSelectedLinks = false;
                        for (final PackageView<?, ?> packageView : selectionInfo.getPackageViews()) {
                            if (packageView.isPackageSelected()) {
                                hasSelectedPackage = true;
                                break;
                            } else if (packageView.isExpanded()) {
                                hasSelectedLinks = true;
                                break;
                            }
                        }
                        if (hasSelectedPackage) {
                            final PackageType destinationPackage;
                            if (beforeElement instanceof AbstractPackageNode) {
                                destinationPackage = (PackageType) beforeElement;
                            } else if (beforeElement instanceof AbstractPackageChildrenNode) {
                                destinationPackage = ((ChildrenType) beforeElement).getParentNode();
                            } else {
                                destinationPackage = null;
                            }
                            final ArrayList<PackageType> packages = new ArrayList<PackageType>();
                            for (final PackageView<?, ?> packageView : selectionInfo.getPackageViews()) {
                                if (packageView.isPackageSelected()) {
                                    packages.add((PackageType) packageView.getPackage());
                                }
                            }
                            getController().move(packages, destinationPackage);
                        } else if (hasSelectedLinks) {
                            /* move links */
                            final PackageType destinationPackage;
                            final ChildrenType afterChild;
                            if (beforeElement != null) {
                                if (beforeElement instanceof AbstractPackageChildrenNode) {
                                    afterChild = (ChildrenType) beforeElement;
                                    destinationPackage = afterChild.getParentNode();
                                } else if (beforeElement instanceof AbstractPackageNode) {
                                    afterChild = null;
                                    destinationPackage = ((PackageType) beforeElement);
                                } else {
                                    afterChild = null;
                                    destinationPackage = null;
                                }
                            } else {
                                afterChild = null;
                                destinationPackage = null;
                            }
                            if (destinationPackage != null) {
                                final ArrayList<ChildrenType> links = new ArrayList<ChildrenType>();
                                for (final PackageView<?, ?> packageView : selectionInfo.getPackageViews()) {
                                    if (packageView.isExpanded()) {
                                        links.addAll((List<ChildrenType>) packageView.getSelectedChildren());
                                    }
                                }
                                getController().move(links, destinationPackage, afterChild);
                            } else {
                                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(new WTFException("this should not happen!"));
                            }
                        }
                        return null;
                    }
                });
            }
        }
        return null;
    }

    protected boolean importSelectionInfo(TransferSupport support) {
        final SelectionInfo<?, ?> selectionInfo = getSelectionInfo(support);
        if (selectionInfo != null && !selectionInfo.isEmpty()) {
            final AbstractNode afterElement;
            final AbstractNode beforeElement;
            final PackageControllerTableModel<PackageType, ChildrenType> model = table.getModel();
            final boolean isFilteredView = model.isFilteredView();
            if (support.isDrop()) {
                final JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
                final int dropRow = dl.getRow();
                afterElement = model.getObjectbyRow(dropRow);
                if (dl.isInsertRow() && dropRow > 0) {
                    beforeElement = model.getObjectbyRow(dropRow - 1);
                } else {
                    beforeElement = null;
                }
            } else {
                beforeElement = null;
                final List<AbstractNode> firstSelected = model.getSelectedObjects(1);
                if (firstSelected.size() > 0) {
                    afterElement = firstSelected.get(0);
                } else {
                    afterElement = null;
                }
            }
            final Boolean ret = importSelectionInfo(support, selectionInfo, isFilteredView, beforeElement, afterElement);
            if (ret != null) {
                return ret;
            }
        }
        return false;
    }
}
