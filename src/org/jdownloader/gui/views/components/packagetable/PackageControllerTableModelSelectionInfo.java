package org.jdownloader.gui.views.components.packagetable;

import java.util.List;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.utils.event.queue.Queue;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelData.PackageControllerTableModelDataPackage;

public class PackageControllerTableModelSelectionInfo<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends SelectionInfo<PackageType, ChildrenType> {

    private final PackageControllerTableModelData<PackageType, ChildrenType> tableModelData;
    private final AbstractNode                                               rawContext;

    protected PackageControllerTableModelSelectionInfo(final AbstractNode contextObject, final PackageControllerTableModel<PackageType, ChildrenType> tableModel) {
        super(tableModel.getController());
        this.rawContext = contextObject;
        this.tableModelData = tableModel.getTableData();
        aggregate();
    }

    @Override
    public AbstractNode getRawContext() {
        return rawContext;
    }

    @Override
    public List<AbstractNode> getRawSelection() {
        return tableModelData;
    }

    @Override
    protected void aggregate(Queue queue) {
        super.aggregate(null);
    }

    @Override
    protected void aggregate() {
        for (final PackageControllerTableModelDataPackage modelDataPackage : tableModelData.getModelDataPackages()) {
            final PackageType pkg = (PackageType) modelDataPackage.getPackage();
            final int index = children.size();
            final List<? extends AbstractNode> visible = modelDataPackage.getVisibleChildren();
            for (final AbstractNode node : visible) {
                children.add((ChildrenType) node);
            }
            final int size = visible.size();
            final PackageView<PackageType, ChildrenType> packageView = new PackageView<PackageType, ChildrenType>() {

                @Override
                public List<ChildrenType> getChildren() {
                    return children.subList(index, index + size);
                }

                @Override
                public PackageType getPackage() {
                    return pkg;
                }

                @Override
                public boolean isPackageSelected() {
                    return true;
                }

                @Override
                public boolean isExpanded() {
                    return modelDataPackage.isExpanded();
                }

                @Override
                public List<ChildrenType> getSelectedChildren() {
                    return children.subList(index, index + size);
                }
            };
            addPackageView(packageView, pkg);
        }
    }

}
