package org.jdownloader.gui.views.components.packagetable;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;

import org.appwork.utils.event.queue.Queue;
import org.jdownloader.gui.views.SelectionInfo;

public class EmptySelectionInfo<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends SelectionInfo<PackageType, ChildrenType> {
    public EmptySelectionInfo(final PackageController<PackageType, ChildrenType> packageController) {
        super(packageController);
        pluginViewsInitiated.set(true);
    }

    @Override
    protected void aggregate(Queue queue) {
        super.aggregate(null);
    }
}
