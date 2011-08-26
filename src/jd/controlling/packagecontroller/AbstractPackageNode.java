package jd.controlling.packagecontroller;

import java.util.List;

public interface AbstractPackageNode<V extends AbstractPackageChildrenNode<E>, E extends AbstractPackageNode<V, E>> extends AbstractNode {

    PackageController<E, V> getControlledBy();

    void setControlledBy(PackageController<E, V> controller);

    List<V> getChildren();

}
