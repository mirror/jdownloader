package org.jdownloader.gui.views.components.packagetable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractNodeVisitor;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;

import org.jdownloader.gui.views.SelectionInfo;

public class PackageControllerSelectionInfo<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends SelectionInfo<PackageType, ChildrenType> {

    private volatile long backendVersion = -1;

    public long getBackendVersion() {
        return backendVersion;
    }

    public PackageControllerSelectionInfo(PackageController<PackageType, ChildrenType> packageController) {
        super(packageController);
        aggregate(packageController.getQueue());
    }

    @Override
    protected void aggregate() {
        final PackageController<PackageType, ChildrenType> controller = getController();
        backendVersion = controller.getBackendChanged();
        controller.visitNodes(new AbstractNodeVisitor<ChildrenType, PackageType>() {

            @Override
            public Boolean visitPackageNode(final PackageType currentPackage) {
                final boolean isExpanded = currentPackage.isExpanded();
                final boolean readL = currentPackage.getModifyLock().readLock();
                final int index;
                final int size;
                try {
                    index = children.size();
                    size = currentPackage.getChildren().size();
                    children.addAll(currentPackage.getChildren());
                } finally {
                    currentPackage.getModifyLock().readUnlock(readL);
                }
                final PackageView<PackageType, ChildrenType> packageView = new PackageView<PackageType, ChildrenType>() {

                    @Override
                    public List<ChildrenType> getChildren() {
                        return children.subList(index, index + size);
                    }

                    @Override
                    public PackageType getPackage() {
                        return currentPackage;
                    }

                    @Override
                    public boolean isPackageSelected() {
                        return true;
                    }

                    @Override
                    public boolean isExpanded() {
                        return isExpanded;
                    }

                    @Override
                    public List<ChildrenType> getSelectedChildren() {
                        return children.subList(index, index + size);
                    }
                };
                addPackageView(packageView, currentPackage);
                return Boolean.FALSE;
            }

            @Override
            public Boolean visitChildrenNode(ChildrenType node) {
                return Boolean.FALSE;
            }
        }, true);
    }

    private final AtomicBoolean rawSelectionInitiated = new AtomicBoolean(false);

    @Override
    public synchronized List<AbstractNode> getRawSelection() {
        if (rawSelectionInitiated.get() == false) {
            for (final PackageView<PackageType, ChildrenType> packageView : getPackageViews()) {
                rawSelection.add(packageView.getPackage());
                rawSelection.addAll(packageView.getChildren());
            }
            rawSelectionInitiated.set(true);
        }
        return super.getRawSelection();
    }

}
