package jd.controlling.packagecontroller;

import java.util.List;

import org.jdownloader.controlling.UniqueAlltimeID;

public interface AbstractPackageNode<V extends AbstractPackageChildrenNode<E>, E extends AbstractPackageNode<V, E>> extends AbstractNode, AbstractNodeNotifier {

    PackageController<E, V> getControlledBy();

    void setControlledBy(PackageController<E, V> controller);

    public UniqueAlltimeID getUniqueID();

    List<V> getChildren();

    ModifyLock getModifyLock();

    void setCurrentSorter(PackageControllerComparator<V> comparator);

    PackageControllerComparator<V> getCurrentSorter();

    ChildrenView<V> getView();

    boolean isExpanded();

    void setExpanded(boolean b);

    int indexOf(V child);

}
