package org.jdownloader.gui.views.components.packagetable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.event.queue.Queue;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelData.PackageControllerTableModelDataPackage;

public class PackageControllerTableModelSelectionOnlySelectionInfo<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends SelectionInfo<PackageType, ChildrenType> {

    private final PackageControllerTableModel<PackageType, ChildrenType>     tableModel;
    private final PackageControllerTableModelData<PackageType, ChildrenType> tableModelData;
    private ListSelectionModel                                               selectionModel = null;
    private volatile AbstractNode                                            rawContext     = null;

    protected void setRawContext(AbstractNode rawContext) {
        this.rawContext = rawContext;
    }

    public static interface SelectionOnlyPackageView<PackageType, ChildrenType> extends PackageView {

        public List<ChildrenType> getVisibleChildren();

        public List<ChildrenType> getInvisibleChildren();
    }

    protected PackageControllerTableModelSelectionOnlySelectionInfo(final AbstractNode contextObject, final PackageControllerTableModel<PackageType, ChildrenType> tableModel) {
        super();
        this.rawContext = contextObject;
        this.tableModel = tableModel;
        this.tableModelData = tableModel.getTableData();
        aggregate();
    }

    protected PackageControllerTableModelSelectionOnlySelectionInfo(final AbstractNode contextObject, PackageControllerTableModelSelectionOnlySelectionInfo<PackageType, ChildrenType> selectionInfo) {
        super();
        this.rawContext = contextObject;
        this.tableModel = selectionInfo.tableModel;
        this.tableModelData = selectionInfo.tableModelData;
        this.selectionModel = selectionInfo.selectionModel;
        this.rawSelection.addAll(selectionInfo.rawSelection);
        this.children.addAll(selectionInfo.children);
        if (selectionInfo.unselectedChildrenInitialized.get()) {
            this.unselectedChildrenInitialized.set(true);
            this.unselectedChildren.addAll(selectionInfo.getUnselectedChildren());
        }
        this.packageViewList.addAll(selectionInfo.packageViewList);
        this.packageViews.putAll(selectionInfo.packageViews);
        if (selectionInfo.pluginViewsInitiated.get()) {
            this.pluginViewsInitiated.set(true);
            this.pluginViews.putAll(selectionInfo.pluginViews);
        }
    }

    @Override
    public AbstractNode getRawContext() {
        return rawContext;
    }

    @Override
    protected void aggregate(Queue queue) {
        super.aggregate(null);
    }

    @Override
    protected void aggregate() {
        final ListSelectionModel selectionModel = tableModel.getTable().getSelectionModel();
        if (selectionModel == null || tableModel.isTableSelectionClearing() || selectionModel.isSelectionEmpty()) {
            return;
        }
        final int iMin = selectionModel.getMinSelectionIndex();
        final int iMax = selectionModel.getMaxSelectionIndex();
        if (iMin == -1 || iMax == -1) {
            return;
        }
        if (iMin >= tableModelData.size() || iMax >= tableModelData.size()) {
            throw new IllegalStateException("SelectionModel and TableData missmatch! IMin:" + iMin + "|IMax:" + iMax + "|TableSize:" + tableModelData.size());
        }
        if (selectionModel instanceof DefaultListSelectionModel) {
            try {
                this.selectionModel = (ListSelectionModel) (((DefaultListSelectionModel) selectionModel).clone());
            } catch (CloneNotSupportedException e) {
                this.selectionModel = selectionModel;
            }
        } else {
            this.selectionModel = selectionModel;
        }
        if (rawSelection instanceof ArrayList) {
            ((ArrayList) rawSelection).ensureCapacity(Math.max(1, iMax - iMin));
        }
        final ArrayList<ChildrenType> lastPackageSelectedChildren = new ArrayList<ChildrenType>();
        PackageControllerTableModelDataPackage lastPackage = null;
        boolean lastPackageSelected = false;
        final AtomicInteger lastPackageIndex = new AtomicInteger(0);

        for (int selectionIndex = iMin; selectionIndex <= iMax; selectionIndex++) {
            final AbstractNode node = tableModelData.get(selectionIndex);
            if (node instanceof AbstractPackageNode) {
                final PackageType pkg = (PackageType) node;
                if (lastPackage != null) {
                    aggregatePackagePackageView(lastPackage, lastPackageSelected, lastPackageSelectedChildren);
                    lastPackage = null;
                    lastPackageSelected = false;
                    lastPackageSelectedChildren.clear();
                }
            }
            if (selectionModel.isSelectedIndex(selectionIndex)) {
                rawSelection.add(node);
                if (node instanceof AbstractPackageNode) {
                    final PackageType pkg = (PackageType) node;
                    aggregatePackagePackageView(lastPackage, lastPackageSelected, lastPackageSelectedChildren);
                    lastPackage = getPackageData(lastPackageIndex, pkg);
                    lastPackageSelected = true;
                    lastPackageSelectedChildren.clear();
                } else if (node instanceof AbstractPackageChildrenNode) {
                    final ChildrenType child = (ChildrenType) node;
                    if (tableModelData.isHiddenPackageSingleChildIndex(selectionIndex)) {
                        if (lastPackage != null) {
                            aggregatePackagePackageView(lastPackage, lastPackageSelected, lastPackageSelectedChildren);
                            lastPackage = null;
                            lastPackageSelected = false;
                            lastPackageSelectedChildren.clear();
                        }
                        final PackageType pkg = getPackage(lastPackageIndex.get(), child);
                        lastPackage = getPackageData(lastPackageIndex, pkg);
                        lastPackageSelected = true;
                        lastPackageSelectedChildren.add(child);
                    } else {
                        if (lastPackage == null) {
                            final PackageType pkg = getPreviousPackage(selectionIndex, child);
                            lastPackage = getPackageData(lastPackageIndex, pkg);
                            lastPackageSelected = false;
                        }
                        lastPackageSelectedChildren.add(child);
                    }
                }
            }
        }
        aggregatePackagePackageView(lastPackage, lastPackageSelected, lastPackageSelectedChildren);
    }

    private final AtomicBoolean           unselectedChildrenInitialized = new AtomicBoolean(false);
    private final ArrayList<ChildrenType> unselectedChildren            = new ArrayList<ChildrenType>();

    @Override
    public synchronized List<ChildrenType> getUnselectedChildren() {
        if (unselectedChildrenInitialized.get() == false && selectionModel != null) {
            unselectedChildrenInitialized.set(true);
            final AtomicInteger lastPackageIndex = new AtomicInteger(0);
            final int maxSize = tableModelData.size();
            final ArrayList<ChildrenType> unselected = new ArrayList<ChildrenType>();
            for (int selectionIndex = 0; selectionIndex < maxSize; selectionIndex++) {
                final AbstractNode node = tableModelData.get(selectionIndex);
                final boolean isSelected = selectionModel.isSelectedIndex(selectionIndex);
                if (node instanceof AbstractPackageNode) {
                    unselectedChildren.addAll(unselected);
                    unselected.clear();
                    final PackageType pkg = (PackageType) node;
                    final PackageControllerTableModelDataPackage pkgData = getPackageData(lastPackageIndex, pkg);
                    if (!pkgData.isExpanded()) {
                        if (!isSelected) {
                            for (final AbstractNode child : pkgData.getVisibleChildren()) {
                                unselectedChildren.add((ChildrenType) child);
                            }
                        }
                    } else {
                        if (!isSelected) {
                            for (final AbstractNode child : pkgData.getVisibleChildren()) {
                                unselected.add((ChildrenType) child);
                            }
                        }
                    }
                } else if (node instanceof AbstractPackageChildrenNode) {
                    final boolean hidden = tableModelData.isHiddenPackageSingleChildIndex(selectionIndex);
                    if (hidden) {
                        unselectedChildren.addAll(unselected);
                        unselected.clear();
                        if (!isSelected) {
                            unselectedChildren.add((ChildrenType) node);
                        }
                    } else {
                        if (isSelected) {
                            unselected.remove(node);
                        }
                    }
                }
            }
            unselectedChildren.addAll(unselected);
        }
        return unselectedChildren;
    }

    private PackageType getPackage(int lastPackageIndex, ChildrenType childrenType) {
        final int size = tableModelData.getModelDataPackages().size();
        for (int index = lastPackageIndex; index < size; index++) {
            final PackageControllerTableModelDataPackage next = tableModelData.getModelDataPackages().get(index);
            if (next.getVisibleChildren().contains(childrenType)) {
                return (PackageType) next.getPackage();
            }
        }
        throw new WTFException("No PreviousPackage?!");
    }

    private PackageType getPreviousPackage(int currentIndex, ChildrenType childrenType) {
        for (int index = currentIndex; index >= 0; index--) {
            final AbstractNode node = tableModelData.get(index);
            if (node instanceof AbstractPackageNode) {
                return (PackageType) node;
            }
        }
        throw new WTFException("No PreviousPackage?!");
    }

    private PackageControllerTableModelDataPackage getPackageData(AtomicInteger lastPackageIndex, PackageType currentPackage) {
        final int size = tableModelData.getModelDataPackages().size();
        for (int index = lastPackageIndex.get(); index < size; index++) {
            final PackageControllerTableModelDataPackage next = tableModelData.getModelDataPackages().get(index);
            if (next.getPackage() == currentPackage) {
                lastPackageIndex.set(index);
                return next;
            }
        }
        throw new WTFException("MissMatch between Selection and TableData detected");
    }

    private void aggregatePackagePackageView(final PackageControllerTableModelDataPackage pkgData, final boolean packageSelected, final List<ChildrenType> selectedChildren) {
        if (pkgData != null) {
            final PackageType pkg = (PackageType) pkgData.getPackage();
            final int index = children.size();
            if (pkgData.isExpanded() == false) {
                final List<? extends AbstractNode> visible = pkgData.getVisibleChildren();
                for (final AbstractNode node : visible) {
                    children.add((ChildrenType) node);
                }
                final int size = visible.size();
                final SelectionOnlyPackageView<PackageType, ChildrenType> packageView = new SelectionOnlyPackageView<PackageType, ChildrenType>() {

                    @Override
                    public List<ChildrenType> getChildren() {
                        return children.subList(index, index + size);
                    }

                    @Override
                    public PackageType getPackage() {
                        return (PackageType) pkgData.getPackage();
                    }

                    @Override
                    public boolean isPackageSelected() {
                        return packageSelected;
                    }

                    @Override
                    public List<ChildrenType> getVisibleChildren() {
                        return (List<ChildrenType>) pkgData.getVisibleChildren();
                    }

                    @Override
                    public List<ChildrenType> getInvisibleChildren() {
                        return (List<ChildrenType>) pkgData.getVisibleChildren();
                    }

                    @Override
                    public boolean isExpanded() {
                        return pkgData.isExpanded();
                    }

                    @Override
                    public List<ChildrenType> getSelectedChildren() {
                        return children.subList(index, index + size);
                    }
                };
                addPackageView(packageView, pkg);
                return;
            } else if (selectedChildren.size() == 0) {
                final List<? extends AbstractNode> visible = pkgData.getVisibleChildren();
                for (final AbstractNode node : visible) {
                    children.add((ChildrenType) node);
                }
                final int size = visible.size();
                final SelectionOnlyPackageView<PackageType, ChildrenType> packageView = new SelectionOnlyPackageView<PackageType, ChildrenType>() {

                    @Override
                    public List<ChildrenType> getChildren() {
                        return children.subList(index, index + size);
                    }

                    @Override
                    public List<ChildrenType> getVisibleChildren() {
                        return (List<ChildrenType>) pkgData.getVisibleChildren();
                    }

                    @Override
                    public List<ChildrenType> getInvisibleChildren() {
                        return (List<ChildrenType>) pkgData.getVisibleChildren();
                    }

                    @Override
                    public PackageType getPackage() {
                        return (PackageType) pkgData.getPackage();
                    }

                    @Override
                    public boolean isPackageSelected() {
                        return packageSelected;
                    }

                    @Override
                    public boolean isExpanded() {
                        return pkgData.isExpanded();
                    }

                    @Override
                    public List<ChildrenType> getSelectedChildren() {
                        return new ArrayList<ChildrenType>(0);
                    }
                };
                addPackageView(packageView, pkg);
                return;
            } else {
                if (selectedChildren.size() == 1 && pkgData.isExpanded() && tableModelData.isHideSingleChildPackages()) {
                    final List<? extends AbstractNode> visible = pkgData.getVisibleChildren();
                    if (visible.size() == 1) {
                        for (final AbstractNode node : visible) {
                            children.add((ChildrenType) node);
                        }
                        final int size = visible.size();
                        final SelectionOnlyPackageView<PackageType, ChildrenType> packageView = new SelectionOnlyPackageView<PackageType, ChildrenType>() {

                            @Override
                            public List<ChildrenType> getChildren() {
                                return children.subList(index, index + size);
                            }

                            @Override
                            public List<ChildrenType> getVisibleChildren() {
                                return (List<ChildrenType>) pkgData.getVisibleChildren();
                            }

                            @Override
                            public List<ChildrenType> getInvisibleChildren() {
                                return (List<ChildrenType>) pkgData.getVisibleChildren();
                            }

                            @Override
                            public PackageType getPackage() {
                                return (PackageType) pkgData.getPackage();
                            }

                            @Override
                            public boolean isPackageSelected() {
                                return true;
                            }

                            @Override
                            public boolean isExpanded() {
                                return true;
                            }

                            @Override
                            public List<ChildrenType> getSelectedChildren() {
                                return new ArrayList<ChildrenType>(0);
                            }
                        };
                        addPackageView(packageView, pkg);
                        return;
                    }
                }
                children.addAll(selectedChildren);
                final int size = selectedChildren.size();
                final SelectionOnlyPackageView<PackageType, ChildrenType> packageView = new SelectionOnlyPackageView<PackageType, ChildrenType>() {

                    @Override
                    public List<ChildrenType> getChildren() {
                        return children.subList(index, index + size);
                    }

                    @Override
                    public PackageType getPackage() {
                        return (PackageType) pkgData.getPackage();
                    }

                    @Override
                    public boolean isPackageSelected() {
                        return packageSelected;
                    }

                    @Override
                    public boolean isExpanded() {
                        return pkgData.isExpanded();
                    }

                    @Override
                    public List<ChildrenType> getSelectedChildren() {
                        return children.subList(index, index + size);
                    }

                    @Override
                    public List<ChildrenType> getVisibleChildren() {
                        return (List<ChildrenType>) pkgData.getVisibleChildren();
                    }

                    @Override
                    public List<ChildrenType> getInvisibleChildren() {
                        return (List<ChildrenType>) pkgData.getVisibleChildren();
                    }
                };
                addPackageView(packageView, pkg);
                return;
            }
        }
    }
}