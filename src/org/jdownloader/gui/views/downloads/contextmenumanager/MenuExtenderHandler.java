package org.jdownloader.gui.views.downloads.contextmenumanager;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

public interface MenuExtenderHandler<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> {

    void updateMenuModel(ContextMenuManager manager, MenuContainerRoot mr);

}
