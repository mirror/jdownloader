package jd.controlling.packagecontroller;

import java.util.List;

import org.appwork.utils.ModifyLock;
import org.jdownloader.controlling.UniqueAlltimeID;

public interface AbstractPackageNode<V extends AbstractPackageChildrenNode<E>, E extends AbstractPackageNode<V, E>> extends AbstractNode, AbstractNodeNotifier {
    PackageController<E, V> getControlledBy();

    void setControlledBy(PackageController<E, V> controller);

    public UniqueAlltimeID getUniqueID();

    List<V> getChildren();

    public int size();

    ModifyLock getModifyLock();

    void setCurrentSorter(PackageControllerComparator<AbstractNode> comparator);

    PackageControllerComparator<AbstractNode> getCurrentSorter();

    String getDownloadDirectory();

    ChildrenView<E, V> getView();

    boolean isExpanded();

    long getModified();

    void setExpanded(boolean b);

    int indexOf(V child);
}
