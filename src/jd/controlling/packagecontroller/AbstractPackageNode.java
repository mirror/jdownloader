package jd.controlling.packagecontroller;

import java.util.List;

public interface AbstractPackageNode<V extends AbstractPackageChildrenNode<E>, E extends AbstractPackageNode<V, E>> extends AbstractNode, AbstractNodeNotifier {

    PackageController<E, V> getControlledBy();

    void setControlledBy(PackageController<E, V> controller);

    List<V> getChildren();

    ModifyLock getModifyLock();

    void sort();

    void setCurrentSorter(ChildComparator<V> comparator);

    ChildComparator<V> getCurrentSorter();

    ChildrenView<V> getView();

    boolean isExpanded();

    void setExpanded(boolean b);

    int indexOf(V child);

}
