package org.jdownloader.gui.views.components.packagetable.dragdrop;

import java.util.ArrayList;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;

public class PackageControllerTableTransferableContent<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> {

    protected PackageControllerTable<PackageType, ChildrenType> table;

    public PackageControllerTable<PackageType, ChildrenType> getTable() {
        return table;
    }

    protected PackageControllerTableTransferableContent(PackageControllerTable<PackageType, ChildrenType> table, ArrayList<PackageType> packages, ArrayList<ChildrenType> links) {
        this.packages = packages;
        this.links = links;
        this.table = table;
    }

    protected ArrayList<PackageType> packages = null;

    public ArrayList<PackageType> getPackages() {
        return packages;
    }

    public ArrayList<ChildrenType> getLinks() {
        return links;
    }

    protected ArrayList<ChildrenType> links = null;
}
