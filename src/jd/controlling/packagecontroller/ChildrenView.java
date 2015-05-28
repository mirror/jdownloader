package jd.controlling.packagecontroller;

import java.lang.ref.WeakReference;
import java.util.List;

import org.jdownloader.DomainInfo;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelData.PackageControllerTableModelDataPackage;

public abstract class ChildrenView<T> {

    public static enum ChildrenAvailablility {
        ONLINE,
        OFFLINE,
        MIXED,
        UNKNOWN;
    }

    abstract public ChildrenView<T> setItems(List<T> items);

    abstract public ChildrenView<T> aggregate();

    abstract public void requestUpdate();

    abstract public boolean updateRequired();

    abstract public DomainInfo[] getDomainInfos();

    abstract public int size();

    abstract public boolean isEnabled();

    abstract public ChildrenAvailablility getAvailability();

    abstract public String getMessage(Object requestor);

    private volatile WeakReference<PackageControllerTableModelDataPackage> tableModelDataPackage = null;

    public PackageControllerTableModelDataPackage getTableModelDataPackage() {
        final WeakReference<PackageControllerTableModelDataPackage> ltableModelDataPackage = tableModelDataPackage;
        if (ltableModelDataPackage != null) {
            return ltableModelDataPackage.get();
        }
        return null;
    }

    public void setTableModelDataPackage(PackageControllerTableModelDataPackage tableModelDataPackage) {
        this.tableModelDataPackage = new WeakReference<PackageControllerTableModelDataPackage>(tableModelDataPackage);
    }

}
