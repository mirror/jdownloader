package org.jdownloader.gui.views.components.packagetable;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

public interface PackageControllerTableModelFilter<E extends AbstractPackageNode<V, E>, V extends AbstractPackageChildrenNode<E>> {

    public boolean isFiltered(E e);

    public boolean isFiltered(V v);

    public boolean isFilteringChildrenNodes();

    public boolean isFilteringPackageNodes();

    public int getComplexity();

}
