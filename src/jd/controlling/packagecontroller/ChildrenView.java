package jd.controlling.packagecontroller;

import java.lang.ref.WeakReference;

import org.jdownloader.DomainInfo;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelData.PackageControllerTableModelDataPackage;

public abstract class ChildrenView<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> {
    public static enum ChildrenAvailablility {
        ONLINE,
        OFFLINE,
        MIXED,
        UNKNOWN;
    }

    abstract public ChildrenView<PackageType, ChildrenType> aggregate();

    abstract public void requestUpdate();

    abstract public boolean updateRequired();

    abstract public DomainInfo[] getDomainInfos();

    abstract public int size();

    abstract public boolean isEnabled();

    abstract public ChildrenAvailablility getAvailability();

    abstract public String getMessage(Object requestor);

    protected volatile WeakReference<PackageControllerTableModelDataPackage<PackageType, ChildrenType>> tableModelDataPackage = null;

    public PackageControllerTableModelDataPackage<PackageType, ChildrenType> getTableModelDataPackage() {
        final WeakReference<PackageControllerTableModelDataPackage<PackageType, ChildrenType>> ltableModelDataPackage = tableModelDataPackage;
        if (ltableModelDataPackage != null) {
            return ltableModelDataPackage.get();
        } else {
            return null;
        }
    }

    public void setTableModelDataPackage(PackageControllerTableModelDataPackage<PackageType, ChildrenType> tableModelDataPackage) {
        if (tableModelDataPackage == null) {
            this.tableModelDataPackage = null;
        } else {
            this.tableModelDataPackage = new WeakReference<PackageControllerTableModelDataPackage<PackageType, ChildrenType>>(tableModelDataPackage);
        }
        aggregate();
    }
}
