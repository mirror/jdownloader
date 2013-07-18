package org.jdownloader.gui.views.components.packagetable.dragdrop;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;

public class PackageControllerTableTransferableContent<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> {

    protected PackageControllerTable<PackageType, ChildrenType> table;

    public PackageControllerTable<PackageType, ChildrenType> getTable() {
        return table;
    }

    protected PackageControllerTableTransferableContent(PackageControllerTable<PackageType, ChildrenType> table, java.util.List<PackageType> packages, java.util.List<ChildrenType> links) {
        this.packages = packages;
        this.links = links;
        this.table = table;
    }

    protected java.util.List<PackageType> packages = null;

    public java.util.List<PackageType> getPackages() {
        return packages;
    }

    public java.util.List<ChildrenType> getLinks() {
        return links;
    }

    protected java.util.List<ChildrenType> links = null;

    protected void exportDone() {
        links = null;
        packages = null;
    }
}
