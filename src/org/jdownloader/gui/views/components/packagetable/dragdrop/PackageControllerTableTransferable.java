package org.jdownloader.gui.views.components.packagetable.dragdrop;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;

public class PackageControllerTableTransferable<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> implements Transferable {

    public static final DataFlavor                                                 FLAVOR = new DataFlavor(PackageControllerTableTransferable.class, PackageControllerTableTransferable.class.getName()) {

                                                                                              @Override
                                                                                              public boolean isFlavorSerializedObjectType() {
                                                                                                  return false;
                                                                                              }
                                                                                          };

    protected PackageControllerTableTransferableContent<PackageType, ChildrenType> content;

    public PackageControllerTableTransferableContent<PackageType, ChildrenType> getContent() {
        return content;
    }

    protected DataFlavor[] flavors;

    public PackageControllerTableTransferable(java.util.List<PackageType> packages, java.util.List<ChildrenType> links, PackageControllerTable<PackageType, ChildrenType> table) {
        this.content = new PackageControllerTableTransferableContent<PackageType, ChildrenType>(table, packages, links);
        java.util.List<DataFlavor> availableFlavors = new ArrayList<DataFlavor>();
        availableFlavors.add(FLAVOR);
        this.flavors = availableFlavors.toArray(new DataFlavor[] {});
    }

    public DataFlavor[] getTransferDataFlavors() {
        return flavors;
    }

    protected boolean isEmpty() {
        return content == null || (content.links == null || content.links.size() == 0) && (content.packages == null || content.packages.size() == 0);
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
        if (flavor.equals(FLAVOR)) return content;
        throw new UnsupportedFlavorException(flavor);
    }

    public boolean isDataFlavorSupported(DataFlavor wished) {
        if (wished != null && !isEmpty()) {
            for (DataFlavor exist : flavors) {
                if (exist.equals(wished)) return true;
            }
        }
        return false;
    }

}
