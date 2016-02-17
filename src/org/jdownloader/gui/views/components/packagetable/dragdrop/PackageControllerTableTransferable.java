package org.jdownloader.gui.views.components.packagetable.dragdrop;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;

public class PackageControllerTableTransferable<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> implements Transferable {

    public static final DataFlavor                           FLAVOR = new DataFlavor(PackageControllerTableTransferable.class, PackageControllerTableTransferable.class.getName()) {

                                                                        @Override
                                                                        public boolean isFlavorSerializedObjectType() {
                                                                            return false;
                                                                        }
                                                                    };

    protected final SelectionInfo<PackageType, ChildrenType> selectionInfo;

    public SelectionInfo<PackageType, ChildrenType> getSelectionInfo() {
        return selectionInfo;
    }

    protected DataFlavor[]                                            flavors;

    protected final PackageControllerTable<PackageType, ChildrenType> table;

    public PackageControllerTable<PackageType, ChildrenType> getTable() {
        return table;
    }

    public PackageControllerTableTransferable(SelectionInfo<PackageType, ChildrenType> selectionInfo, PackageControllerTable<PackageType, ChildrenType> table) {
        this.selectionInfo = selectionInfo;
        this.table = table;
        final List<DataFlavor> availableFlavors = new ArrayList<DataFlavor>();
        availableFlavors.add(FLAVOR);
        this.flavors = availableFlavors.toArray(new DataFlavor[] {});
    }

    protected PackageControllerTableTransferable(PackageControllerTableTransferable<PackageType, ChildrenType> transferable) {
        this.selectionInfo = transferable.getSelectionInfo();
        this.table = transferable.getTable();
        this.flavors = transferable.getTransferDataFlavors();
    }

    public DataFlavor[] getTransferDataFlavors() {
        return flavors;
    }

    protected boolean isEmpty() {
        final SelectionInfo<PackageType, ChildrenType> selectionInfo = getSelectionInfo();
        return selectionInfo == null || selectionInfo.isEmpty();
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        if (flavor.equals(FLAVOR)) {
            return getSelectionInfo();
        }
        throw new UnsupportedFlavorException(flavor);
    }

    protected Set<String> getURLs() {
        return LinkTreeUtils.getURLs(getSelectionInfo(), false);
    }

    public boolean isDataFlavorSupported(DataFlavor wished) {
        if (wished != null && !isEmpty()) {
            for (final DataFlavor exist : getTransferDataFlavors()) {
                if (exist.equals(wished)) {
                    return true;
                }
            }
        }
        return false;
    }

}
