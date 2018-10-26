package org.jdownloader.gui.views.components.packagetable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.Icon;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractNodeVisitor;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.ChildrenView;
import jd.controlling.packagecontroller.PackageController;
import jd.controlling.packagecontroller.PackageControllerComparator;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.Storage;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.utils.Application;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelData.PackageControllerTableModelDataPackage;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public abstract class PackageControllerTableModel<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends ExtTableModel<AbstractNode> {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public static enum TOGGLEMODE {
        CURRENT,
        TOP,
        BOTTOM
    }

    @Override
    protected int[] guessSelectedRows(final List<AbstractNode> oldTableData, final int leadIndex, final int anchorIndex, final BitSet selectedRowsBitSet) {
        try {
            if (oldTableData instanceof PackageControllerTableModelData && getRowCount() > 0) {
                final PackageControllerTableModelData<? extends AbstractPackageNode, ? extends AbstractPackageChildrenNode> oldTableModelData = (PackageControllerTableModelData<? extends AbstractPackageNode, ? extends AbstractPackageChildrenNode>) oldTableData;
                final PackageControllerTableModelData<? extends AbstractPackageNode, ? extends AbstractPackageChildrenNode> currentTableModelData = getTableData();
                final HashSet<UniqueAlltimeID> skip = new HashSet<UniqueAlltimeID>();
                int selectedRowIndex = selectedRowsBitSet.length();
                PackageControllerTableModelDataPackage currentPackageData = null;
                PackageControllerTableModelDataPackage oldPackageData = null;
                final PackageController<PackageType, ChildrenType> controller = getController();
                int lastOldRowIndex = -1;
                while (selectedRowIndex >= 0) {
                    final int oldRowIndex;
                    if (Application.getJavaVersion() >= Application.JAVA17) {
                        oldRowIndex = selectedRowsBitSet.previousSetBit(selectedRowIndex);
                    } else {
                        int index = selectedRowIndex;
                        for (; index >= 0; index--) {
                            if (selectedRowsBitSet.get(index)) {
                                break;
                            }
                        }
                        oldRowIndex = index;
                    }
                    selectedRowIndex = oldRowIndex - 1;
                    if (oldRowIndex >= 0) {
                        lastOldRowIndex = oldRowIndex;
                        final AbstractNode oldNode = oldTableModelData.get(oldRowIndex);
                        final UniqueAlltimeID previousParentNodeID;
                        if (oldNode instanceof AbstractPackageChildrenNode) {
                            previousParentNodeID = ((AbstractPackageChildrenNode) oldNode).getPreviousParentNodeID();
                            final Object parentNode = ((AbstractPackageChildrenNode) oldNode).getParentNode();
                            if (parentNode != null) {
                                final AbstractPackageNode parent = (AbstractPackageNode) parentNode;
                                if (parent.getControlledBy() != null && parent.getControlledBy() == controller) {
                                    // links got merged
                                    final int row = getRowforObject(parent);
                                    if (row >= 0) {
                                        return new int[] { row };
                                    }
                                }
                            }
                        } else {
                            for (final PackageControllerTableModelDataPackage dataPackage : oldTableModelData.getModelDataPackages()) {
                                if (dataPackage.getPackage() == oldNode) {
                                    final List<? extends AbstractNode> oldChildren = dataPackage.getVisibleChildren();
                                    for (final AbstractNode oldChildrenNode : oldChildren) {
                                        final Object parentNode = ((AbstractPackageChildrenNode) oldChildrenNode).getParentNode();
                                        if (parentNode != null) {
                                            final AbstractPackageNode parent = (AbstractPackageNode) parentNode;
                                            if (parent.getControlledBy() != null && parent.getControlledBy() == controller) {
                                                // links got merged
                                                final int row = getRowforObject(parent);
                                                if (row >= 0) {
                                                    return new int[] { row };
                                                }
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                            continue;
                        }
                        if (skip.contains(previousParentNodeID)) {
                            continue;
                        } else {
                            if (currentPackageData == null || !currentPackageData.getPackage().getUniqueID().equals(previousParentNodeID)) {
                                currentPackageData = null;
                                for (final PackageControllerTableModelDataPackage dataPackage : currentTableModelData.getModelDataPackages()) {
                                    if (dataPackage.getPackage().getUniqueID().equals(previousParentNodeID)) {
                                        currentPackageData = dataPackage;
                                        break;
                                    }
                                }
                            }
                            if (currentPackageData == null) {
                                // AbstractPackageNode no longer existing
                                skip.add(previousParentNodeID);
                                continue;
                            }
                        }
                        if (oldPackageData == null || !oldPackageData.getPackage().getUniqueID().equals(previousParentNodeID)) {
                            oldPackageData = null;
                            for (final PackageControllerTableModelDataPackage dataPackage : oldTableModelData.getModelDataPackages()) {
                                if (dataPackage.getPackage().getUniqueID().equals(previousParentNodeID)) {
                                    oldPackageData = dataPackage;
                                    break;
                                }
                            }
                            if (oldPackageData == null) {
                                // AbstractPackageNode never existed?!
                                skip.add(previousParentNodeID);
                                continue;
                            }
                        }
                        final List<? extends AbstractNode> oldChildren = oldPackageData.getVisibleChildren();
                        final int oldChildIndex = oldChildren.indexOf(oldNode);
                        if (oldChildIndex >= 0) {
                            final AbstractNode searchChild;
                            if (oldChildIndex + 1 < oldChildren.size()) {
                                searchChild = oldChildren.get(oldChildIndex + 1);
                            } else if (oldChildIndex - 1 >= 0) {
                                searchChild = oldChildren.get(oldChildIndex - 1);
                            } else {
                                searchChild = null;
                            }
                            final List<? extends AbstractNode> currentChildren = currentPackageData.getVisibleChildren();
                            if (searchChild != null && currentChildren.contains(searchChild)) {
                                final int row = getRowforObject(searchChild);
                                if (row >= 0) {
                                    return new int[] { row };
                                }
                            }
                        }
                    }
                }
                lastOldRowIndex = Math.max(0, lastOldRowIndex - 1);
                if (lastOldRowIndex >= 0) {
                    return new int[] { Math.min(lastOldRowIndex, getRowCount() - 1) };
                }
            }
        } catch (final Throwable e) {
            LogController.CL().log(e);
        }
        return super.guessSelectedRows(oldTableData, leadIndex, anchorIndex, selectedRowsBitSet);
    }

    @Override
    public int getRowforObject(final AbstractNode node) {
        return getTableData().getRowforObject(node, getController());
    }

    @Override
    public boolean contains(final AbstractNode node) {
        return getTableData().getRowforObject(node, getController()) >= 0;
    }

    public abstract class TableDataModification {
        protected abstract void modifyPackageData(PackageType pkg, List<ChildrenType> unfilteredChildren);

        protected PackageControllerTableModelCustomizer finalizeTableModification() {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public PackageControllerTable<PackageType, ChildrenType> getTable() {
        return (PackageControllerTable<PackageType, ChildrenType>) super.getTable();
    }

    private boolean tristateSorterEnabled = true;

    public boolean isTristateSorterEnabled() {
        return tristateSorterEnabled;
    }

    public void setTristateSorterEnabled(boolean tristateSorterEnabled) {
        this.tristateSorterEnabled = tristateSorterEnabled;
    }

    private static final String                                                 SORT_ORIGINAL                   = "ORIGINAL";
    private final DelayedRunnable                                               asyncRefresh;
    protected final PackageController<PackageType, ChildrenType>                pc;
    private final DelayedRunnable                                               asyncRecreate;
    private CopyOnWriteArrayList<TableDataModification>                         tableModifiers                  = new CopyOnWriteArrayList<TableDataModification>();
    private ScheduledExecutorService                                            queue                           = DelayedRunnable.getNewScheduledExecutorService();
    private final DelayedRunnable                                               asyncRecreateFast;
    private final Storage                                                       storage;
    private final AtomicLong                                                    repaintFiredCounter             = new AtomicLong(0);
    private final AtomicLong                                                    structureChangedCounter         = new AtomicLong(0);
    private final AtomicLong                                                    repaintRequested                = new AtomicLong(0);
    private final AtomicLong                                                    structureChangeRequested        = new AtomicLong(0);
    private final AtomicLong                                                    structureChangeProcessed        = new AtomicLong(0);
    private volatile boolean                                                    hideSinglePackage               = false;
    private volatile PackageControllerTableModelData<PackageType, ChildrenType> packageControllertableModelData = new PackageControllerTableModelData<PackageType, ChildrenType>();
    private boolean                                                             scrollPositionRestoreDone;

    public boolean isHideSinglePackage() {
        return hideSinglePackage;
    }

    public PackageControllerTableModel(final PackageController<PackageType, ChildrenType> pc, String id) {
        super(id);
        storage = getStorage();
        resetSorting();
        CFG_GUI.HIDE_SINGLE_CHILD_PACKAGES.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {
            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                hideSinglePackage = Boolean.TRUE.equals(newValue);
                recreateModel(true);
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        }, false);
        CFG_GENERAL.URL_ORDER.getEventSender().addListener(new GenericConfigEventListener<Object>() {
            @Override
            public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
                recreateModel(true);
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
            }
        }, true);
        hideSinglePackage = CFG_GUI.HIDE_SINGLE_CHILD_PACKAGES.isEnabled();
        this.pc = pc;
        asyncRefresh = new DelayedRunnable(queue, 150l, 500l) {
            @Override
            public String getID() {
                return "PackageControllerTableModel_Refresh" + pc.getClass().getName();
            }

            @Override
            public void delayedrun() {
                fireRepaint();
            }
        };
        asyncRecreate = new DelayedRunnable(queue, 50l, 1000l) {
            @Override
            public String getID() {
                return "PackageControllerTableModel_Recreate" + pc.getClass().getName();
            }

            @Override
            public void delayedrun() {
                fireStructureChange();
            }
        };
        asyncRecreateFast = new DelayedRunnable(queue, 10l, 50l) {
            @Override
            public String getID() {
                return "PackageControllerTableModel_RecreateFast" + pc.getClass().getName();
            }

            @Override
            public void delayedrun() {
                fireStructureChange();
            }
        };
        scrollPositionRestoreDone = false;
    }

    private void fireRepaint() {
        if (repaintRequested.get() > structureChangeRequested.get()) {
            ArrayList<ChildrenView<ChildrenType>> viewUpdates = new ArrayList<ChildrenView<ChildrenType>>();
            for (AbstractNode node : getTableData()) {
                if (node instanceof AbstractPackageNode) {
                    ChildrenView<ChildrenType> view = ((AbstractPackageNode) node).getView();
                    if (view.updateRequired()) {
                        viewUpdates.add(view);
                    }
                }
            }
            for (ChildrenView<ChildrenType> view : viewUpdates) {
                view.aggregate();
            }
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    /**
                     * we just want to repaint
                     */
                    if (repaintRequested.get() > structureChangeRequested.get()) {
                        try {
                            getTable().repaint();
                        } finally {
                            getTable().firePropertyChange("repaintFired", repaintFiredCounter.get(), repaintFiredCounter.incrementAndGet());
                        }
                    }
                }
            };
        }
    }

    public void fireStructureChange(boolean force) {
        if (structureChangeRequested.get() > structureChangeProcessed.getAndSet(System.currentTimeMillis()) || force) {
            try {
                _fireTableStructureChanged(getTableData(), true);
            } finally {
                getTable().firePropertyChange("structureChangedFired", structureChangedCounter.get(), structureChangedCounter.incrementAndGet());
            }
        }
    }

    private void fireStructureChange() {
        fireStructureChange(false);
    }

    public void resetSorting() {
        this.sortColumn = null;
        try {
            storage.put(ExtTableModel.SORT_ORDER_ID_KEY, (String) null);
            storage.put(ExtTableModel.SORTCOLUMN_KEY, (String) null);
        } catch (final Exception e) {
            LogController.CL(true).log(e);
        }
    }

    public ScheduledExecutorService getThreadPool() {
        return queue;
    }

    public void recreateModel(boolean delay) {
        structureChangeRequested.set(System.currentTimeMillis());
        if (delay) {
            asyncRecreate.run();
        } else {
            asyncRecreateFast.run();
        }
    }

    public void recreateModel() {
        recreateModel(true);
    }

    public void refreshModel(boolean delay) {
        repaintRequested.set(System.currentTimeMillis());
        if (delay) {
            asyncRefresh.run();
        } else {
            queue.schedule(new Runnable() {
                public void run() {
                    fireRepaint();
                }
            }, 0, TimeUnit.SECONDS);
        }
    }

    public void refreshModel() {
        refreshModel(true);
    }

    public void sortPackageChildren(final AbstractPackageNode pkg, PackageControllerComparator<ChildrenType> comparator) {
        this.resetSorting();
        pc.sortPackageChildren((PackageType) pkg, comparator);
    }

    public PackageController<PackageType, ChildrenType> getController() {
        return pc;
    }

    public void addTableModifier(TableDataModification tableModifier, boolean delay) {
        tableModifiers.add(tableModifier);
        recreateModel(delay);
    }

    public void toggleFilePackageExpand(final AbstractPackageNode fp2, final TOGGLEMODE mode) {
        final boolean currentExpandedState = !fp2.isExpanded();
        switch (mode) {
        case CURRENT:
            tableModifiers.add(new TableDataModification() {
                @Override
                protected void modifyPackageData(PackageType pkg, List<ChildrenType> unfilteredChildren) {
                    if (fp2 == pkg) {
                        if (unfilteredChildren.size() > 0) {
                            pkg.setExpanded(currentExpandedState);
                        }
                    }
                }
            });
            break;
        default:
            final SelectionInfo<PackageType, ChildrenType> selectionInfo = getTable().getSelectionInfo(true, true);
            int count = 0;
            final PackageView<PackageType, ChildrenType> view = selectionInfo.getPackageView((PackageType) fp2);
            if (view != null && view.isPackageSelected()) {
                for (PackageView<PackageType, ChildrenType> packageView : selectionInfo.getPackageViews()) {
                    if (packageView.isPackageSelected()) {
                        count++;
                        if (count > 1) {
                            break;
                        }
                    }
                }
            }
            if (count > 1) {
                tableModifiers.add(new TableDataModification() {
                    @Override
                    protected void modifyPackageData(PackageType pkg, List<ChildrenType> unfilteredChildren) {
                        if (unfilteredChildren.size() > 0) {
                            final PackageView<PackageType, ChildrenType> view = selectionInfo.getPackageView(pkg);
                            if (view != null && view.isPackageSelected()) {
                                pkg.setExpanded(currentExpandedState);
                            }
                        }
                    }
                });
            } else {
                tableModifiers.add(new TableDataModification() {
                    boolean doToggle = false;
                    {
                        switch (mode) {
                        case TOP:
                            doToggle = true;
                            break;
                        case BOTTOM:
                            doToggle = false;
                            break;
                        }
                    }

                    @Override
                    protected void modifyPackageData(PackageType pkg, List<ChildrenType> unfilteredChildren) {
                        if (doToggle) {
                            if (unfilteredChildren.size() > 0) {
                                pkg.setExpanded(currentExpandedState);
                            }
                            if (pkg == fp2) {
                                doToggle = false;
                            }
                        } else {
                            if (pkg == fp2) {
                                doToggle = true;
                                if (unfilteredChildren.size() > 0) {
                                    pkg.setExpanded(currentExpandedState);
                                }
                            }
                        }
                    }
                });
            }
            break;
        }
        recreateModel(false);
    }

    public void setFilePackageExpand(final boolean expanded, final AbstractPackageNode... fp2) {
        tableModifiers.add(new TableDataModification() {
            @Override
            protected void modifyPackageData(PackageType pkg, List<ChildrenType> unfilteredChildren) {
                for (AbstractPackageNode fp : fp2) {
                    if (fp == pkg) {
                        fp.setExpanded(expanded);
                    }
                }
            }
        });
        recreateModel(false);
    }

    private final Comparator<PackageControllerTableModelFilter<PackageType, ChildrenType>> tableFilterComparator = new Comparator<PackageControllerTableModelFilter<PackageType, ChildrenType>>() {
                                                                                                                     public int compare(int x, int y) {
                                                                                                                         return (x < y) ? -1 : ((x == y) ? 0 : 1);
                                                                                                                     }

                                                                                                                     @Override
                                                                                                                     public int compare(PackageControllerTableModelFilter<PackageType, ChildrenType> o1, PackageControllerTableModelFilter<PackageType, ChildrenType> o2) {
                                                                                                                         return compare(o1.getComplexity(), o2.getComplexity());
                                                                                                                     }
                                                                                                                 };

    public boolean isFilteredView() {
        return getTableData().isFiltered();
    }

    @Override
    public PackageControllerTableModelData<PackageType, ChildrenType> getTableData() {
        return packageControllertableModelData;
    }

    private final Object                                                                     tableFiltersLock = new Object();
    private volatile ArrayList<PackageControllerTableModelFilter<PackageType, ChildrenType>> tableFilters     = new ArrayList<PackageControllerTableModelFilter<PackageType, ChildrenType>>();

    public void removeFilter(PackageControllerTableModelFilter<PackageType, ChildrenType> filter) {
        if (filter != null && tableFilters.contains(filter)) {
            synchronized (tableFiltersLock) {
                final ArrayList<PackageControllerTableModelFilter<PackageType, ChildrenType>> availableTableFilters = new ArrayList<PackageControllerTableModelFilter<PackageType, ChildrenType>>(this.tableFilters);
                availableTableFilters.remove(filter);
                this.tableFilters = availableTableFilters;
            }
        }
    }

    public void addFilter(PackageControllerTableModelFilter<PackageType, ChildrenType> filter) {
        if (filter != null && !tableFilters.contains(filter)) {
            synchronized (tableFilters) {
                if (!tableFilters.contains(filter)) {
                    final ArrayList<PackageControllerTableModelFilter<PackageType, ChildrenType>> availableTableFilters = new ArrayList<PackageControllerTableModelFilter<PackageType, ChildrenType>>(this.tableFilters);
                    availableTableFilters.add(filter);
                    try {
                        Collections.sort(availableTableFilters, tableFilterComparator);
                    } catch (Throwable e) {
                        LogController.CL(true).log(e);
                    }
                    this.tableFilters = availableTableFilters;
                }
            }
        }
    }

    @Override
    protected void initColumns() {
    }

    private class CompiledFilterList {
        private final List<PackageControllerTableModelFilter<PackageType, ChildrenType>> packageFilters;

        public List<PackageControllerTableModelFilter<PackageType, ChildrenType>> getPackageFilters() {
            return packageFilters;
        }

        public List<PackageControllerTableModelFilter<PackageType, ChildrenType>> getChildrenFilters() {
            return childrenFilters;
        }

        private final List<PackageControllerTableModelFilter<PackageType, ChildrenType>> childrenFilters;

        private CompiledFilterList() {
            this(new ArrayList<PackageControllerTableModelFilter<PackageType, ChildrenType>>(), new ArrayList<PackageControllerTableModelFilter<PackageType, ChildrenType>>());
        }

        private CompiledFilterList(List<PackageControllerTableModelFilter<PackageType, ChildrenType>> packageFilters, List<PackageControllerTableModelFilter<PackageType, ChildrenType>> childrenFilters) {
            this.packageFilters = packageFilters;
            this.childrenFilters = childrenFilters;
        }
    }

    protected CompiledFilterList compileTableFilters(Collection<PackageControllerTableModelFilter<PackageType, ChildrenType>> filters) {
        if (filters == null || filters.size() == 0) {
            return new CompiledFilterList();
        }
        final ArrayList<PackageControllerTableModelFilter<PackageType, ChildrenType>> packageFilters = new ArrayList<PackageControllerTableModelFilter<PackageType, ChildrenType>>();
        final ArrayList<PackageControllerTableModelFilter<PackageType, ChildrenType>> childrendFilters = new ArrayList<PackageControllerTableModelFilter<PackageType, ChildrenType>>();
        for (PackageControllerTableModelFilter<PackageType, ChildrenType> filter : filters) {
            if (filter.isFilteringPackageNodes()) {
                packageFilters.add(filter);
            }
            if (filter.isFilteringChildrenNodes()) {
                childrendFilters.add(filter);
            }
        }
        if (packageFilters.size() > 0 || childrendFilters.size() > 0) {
            if (packageFilters.size() > 0) {
                try {
                    Collections.sort(packageFilters, tableFilterComparator);
                } catch (Throwable e) {
                    LogController.CL(true).log(e);
                }
            }
            if (childrendFilters.size() > 0) {
                try {
                    Collections.sort(childrendFilters, tableFilterComparator);
                } catch (Throwable e) {
                    LogController.CL(true).log(e);
                }
            }
        }
        return new CompiledFilterList(packageFilters, childrendFilters);
    }

    protected List<PackageControllerTableModelFilter<PackageType, ChildrenType>> getTableFilters() {
        return tableFilters;
    }

    public List<AbstractNode> refreshUnSort(final List<AbstractNode> data) {
        if (data instanceof PackageControllerTableModelData) {
            return data;
        }
        throw new IllegalArgumentException("data must be instanceof PackageControllerTableModelData");
    }

    /*
     * we override sort to have a better sorting of packages/files, to keep their structure alive,data is only used to specify the size of
     * the new ArrayList
     */
    @Override
    public java.util.List<AbstractNode> sort(final java.util.List<AbstractNode> data, ExtColumn<AbstractNode> column) {
        final boolean hideSingleChildPackages = isHideSinglePackage();
        if (column == null || column.getSortOrderIdentifier() == SORT_ORIGINAL) {
            /* RESET sorting to nothing,tri-state */
            this.sortColumn = column = null;
            try {
                storage.put(ExtTableModel.SORT_ORDER_ID_KEY, (String) null);
                storage.put(ExtTableModel.SORTCOLUMN_KEY, (String) null);
            } catch (final Exception e) {
                LogController.CL(true).log(e);
            }
        } else {
            this.sortColumn = column;
            String id = column.getSortOrderIdentifier();
            try {
                storage.put(ExtTableModel.SORT_ORDER_ID_KEY, id);
                storage.put(ExtTableModel.SORTCOLUMN_KEY, column.getID());
            } catch (final Exception e) {
                LogController.CL(true).log(e);
            }
        }
        final CompiledFilterList tableFiltersCompiled = compileTableFilters(getTableFilters());
        final ArrayList<PackageType> packages;
        if (tableFiltersCompiled.getPackageFilters().size() > 0) {
            /* filter packages */
            packages = new ArrayList<PackageType>();
            pc.visitNodes(new AbstractNodeVisitor<ChildrenType, PackageType>() {
                @Override
                public Boolean visitPackageNode(PackageType pkg) {
                    for (final PackageControllerTableModelFilter<PackageType, ChildrenType> filter : tableFiltersCompiled.getPackageFilters()) {
                        if (filter.isFiltered(pkg)) {
                            return Boolean.FALSE;
                        }
                    }
                    packages.add(pkg);
                    return Boolean.FALSE;
                }

                @Override
                public Boolean visitChildrenNode(ChildrenType node) {
                    return Boolean.FALSE;
                }
            }, true);
        } else {
            packages = pc.getPackagesCopy();
        }
        /* sort packages */
        if (column != null) {
            try {
                final ExtDefaultRowSorter<AbstractNode> comparator = column.getRowSorter();
                if (comparator != null) {
                    Collections.sort(packages, comparator);
                }
            } catch (Throwable e) {
                LogController.CL(true).log(e);
            }
        }
        final List<TableDataModification> appliedTableDataModifier;
        if (tableModifiers.size() > 0) {
            appliedTableDataModifier = new ArrayList<TableDataModification>(tableModifiers);
            tableModifiers.removeAll(appliedTableDataModifier);
        } else {
            appliedTableDataModifier = null;
        }
        final PackageControllerTableModelData<PackageType, ChildrenType> newData = new PackageControllerTableModelData<PackageType, ChildrenType>(Math.max(data.size(), packages.size()));
        final List<ChildrenType> visibleChildren = new ArrayList<ChildrenType>(0);
        final List<AbstractNode> filteredChildren = new ArrayList<AbstractNode>(0);
        for (final PackageType node : packages) {
            visibleChildren.clear();
            filteredChildren.clear();
            if (tableFiltersCompiled.getChildrenFilters().size() > 0) {
                /* filter children of this package */
                final boolean readL = node.getModifyLock().readLock();
                try {
                    childrenLoop: for (ChildrenType child : node.getChildren()) {
                        for (PackageControllerTableModelFilter<PackageType, ChildrenType> filter : tableFiltersCompiled.getChildrenFilters()) {
                            if (filter.isFiltered(child)) {
                                filteredChildren.add(child);
                                continue childrenLoop;
                            }
                        }
                        visibleChildren.add(child);
                    }
                } finally {
                    node.getModifyLock().readUnlock(readL);
                }
            } else {
                final boolean readL = node.getModifyLock().readLock();
                try {
                    visibleChildren.addAll(node.getChildren());
                } finally {
                    node.getModifyLock().readUnlock(readL);
                }
            }
            if (appliedTableDataModifier != null) {
                for (TableDataModification modifier : appliedTableDataModifier) {
                    modifier.modifyPackageData(node, visibleChildren);
                }
            }
            final boolean expanded = node.isExpanded();
            if (node.getView() != null) {
                if (visibleChildren.size() == 0) {
                    /* no visible/hidden children, skip PackageNode */
                    continue;
                } else {
                    node.getView().setItems(visibleChildren);
                }
            }
            if (visibleChildren.size() == 1 && hideSingleChildPackages) {
                final int visibleIndex = newData.size();
                final int packageNodeIndex = newData.addHiddenPackageSingleChild(visibleChildren.get(0));
                final PackageControllerTableModelDataPackage modelDataPackage;
                if (filteredChildren.size() == 0) {
                    modelDataPackage = new PackageControllerTableModelDataPackage() {
                        @Override
                        public boolean isExpanded() {
                            return true;
                        }

                        @Override
                        public List<? extends AbstractNode> getVisibleChildren() {
                            return newData.subList(visibleIndex, visibleIndex + 1);
                        }

                        @Override
                        public List<? extends AbstractNode> getInvisibleChildren() {
                            return null;
                        }

                        @Override
                        public PackageType getPackage() {
                            return node;
                        }

                        @Override
                        public int getPackageIndex() {
                            return packageNodeIndex;
                        }
                    };
                } else {
                    final int filteredIndex = newData.getFilteredChildren().size();
                    final int filteredSize = filteredChildren.size();
                    newData.getFilteredChildren().addAll(filteredChildren);
                    modelDataPackage = new PackageControllerTableModelDataPackage() {
                        @Override
                        public boolean isExpanded() {
                            return true;
                        }

                        @Override
                        public List<? extends AbstractNode> getVisibleChildren() {
                            return newData.subList(visibleIndex, visibleIndex + 1);
                        }

                        @Override
                        public List<? extends AbstractNode> getInvisibleChildren() {
                            return newData.getFilteredChildren().subList(filteredIndex, filteredIndex + filteredSize);
                        }

                        @Override
                        public PackageType getPackage() {
                            return node;
                        }

                        @Override
                        public int getPackageIndex() {
                            return packageNodeIndex;
                        };
                    };
                }
                newData.add(modelDataPackage);
            } else {
                /* only add package node if it contains children */
                final int packageNodeIndex = newData.addPackageNode(node);
                final PackageControllerTableModelDataPackage modelDataPackage;
                if (expanded) {
                    /* expanded, add its children */
                    if (column != null && visibleChildren.size() > 1) {
                        /* we only have to sort children if the package is expanded */
                        try {
                            final ExtDefaultRowSorter<AbstractNode> comparator = column.getRowSorter();
                            if (comparator != null) {
                                Collections.sort(visibleChildren, comparator);
                            }
                        } catch (final Throwable e) {
                            LogController.CL(true).log(e);
                        }
                    }
                    final int visibleIndex = newData.size();
                    newData.addAll(visibleChildren);
                    final int visibleSize = visibleChildren.size();
                    if (filteredChildren.size() == 0) {
                        modelDataPackage = new PackageControllerTableModelDataPackage() {
                            @Override
                            public boolean isExpanded() {
                                return true;
                            }

                            @Override
                            public List<? extends AbstractNode> getVisibleChildren() {
                                return newData.subList(visibleIndex, visibleIndex + visibleSize);
                            }

                            @Override
                            public List<? extends AbstractNode> getInvisibleChildren() {
                                return null;
                            }

                            @Override
                            public PackageType getPackage() {
                                return node;
                            }

                            @Override
                            public int getPackageIndex() {
                                return packageNodeIndex;
                            };
                        };
                    } else {
                        final int filteredIndex = newData.getFilteredChildren().size();
                        final int filteredSize = filteredChildren.size();
                        newData.getFilteredChildren().addAll(filteredChildren);
                        modelDataPackage = new PackageControllerTableModelDataPackage() {
                            @Override
                            public boolean isExpanded() {
                                return true;
                            }

                            @Override
                            public List<? extends AbstractNode> getVisibleChildren() {
                                return newData.subList(visibleIndex, visibleIndex + visibleSize);
                            }

                            @Override
                            public List<? extends AbstractNode> getInvisibleChildren() {
                                return newData.getFilteredChildren().subList(filteredIndex, filteredIndex + filteredSize);
                            }

                            @Override
                            public PackageType getPackage() {
                                return node;
                            }

                            @Override
                            public int getPackageIndex() {
                                return packageNodeIndex;
                            };
                        };
                    }
                } else {
                    final int hiddenIndex = newData.getHiddenChildren().size();
                    final int hiddenSize = visibleChildren.size();
                    newData.getHiddenChildren().addAll(visibleChildren);
                    if (filteredChildren.size() == 0) {
                        modelDataPackage = new PackageControllerTableModelDataPackage() {
                            @Override
                            public boolean isExpanded() {
                                return false;
                            }

                            @Override
                            public List<? extends AbstractNode> getVisibleChildren() {
                                return newData.getHiddenChildren().subList(hiddenIndex, hiddenIndex + hiddenSize);
                            }

                            @Override
                            public List<? extends AbstractNode> getInvisibleChildren() {
                                return null;
                            }

                            @Override
                            public PackageType getPackage() {
                                return node;
                            }

                            @Override
                            public int getPackageIndex() {
                                return packageNodeIndex;
                            };
                        };
                    } else {
                        final int filteredIndex = newData.getFilteredChildren().size();
                        final int filteredSize = filteredChildren.size();
                        newData.getFilteredChildren().addAll(filteredChildren);
                        modelDataPackage = new PackageControllerTableModelDataPackage() {
                            @Override
                            public boolean isExpanded() {
                                return false;
                            }

                            @Override
                            public List<? extends AbstractNode> getVisibleChildren() {
                                return newData.getHiddenChildren().subList(hiddenIndex, hiddenIndex + hiddenSize);
                            }

                            @Override
                            public List<? extends AbstractNode> getInvisibleChildren() {
                                return newData.getFilteredChildren().subList(filteredIndex, filteredIndex + filteredSize);
                            }

                            @Override
                            public PackageType getPackage() {
                                return node;
                            }

                            @Override
                            public int getPackageIndex() {
                                return packageNodeIndex;
                            };
                        };
                    }
                }
                newData.add(modelDataPackage);
            }
        }
        final List<PackageControllerTableModelCustomizer> tableDataCustomizer = new ArrayList<PackageControllerTableModelCustomizer>();
        if (appliedTableDataModifier != null) {
            for (TableDataModification modifier : appliedTableDataModifier) {
                final PackageControllerTableModelCustomizer customizer = modifier.finalizeTableModification();
                if (customizer != null) {
                    tableDataCustomizer.add(customizer);
                }
            }
        }
        newData.setPackageFilters(tableFiltersCompiled.getPackageFilters());
        newData.setChildrenFilters(tableFiltersCompiled.getChildrenFilters());
        newData.setTableModelCustomizer(tableDataCustomizer);
        return newData;
    }

    @Override
    protected boolean postSetTableData(List<AbstractNode> newtableData) {
        boolean ret = true;
        if (!(newtableData instanceof PackageControllerTableModelData)) {
            throw new IllegalArgumentException("data must be instanceof PackageControllerTableModelData");
        } else {
            PackageControllerTableModelData<?, ?> data = (PackageControllerTableModelData<?, ?>) newtableData;
            try {
                if (data.getTableModelCustomizer() != null) {
                    for (PackageControllerTableModelCustomizer customizer : data.getTableModelCustomizer()) {
                        ret = customizer.customizedTableData();
                    }
                }
            } finally {
                data.setTableModelCustomizer(null);
            }
        }
        ret = ret && super.postSetTableData(newtableData);
        if (!scrollPositionRestoreDone && newtableData.size() > 0) {
            scrollPositionRestoreDone = true;
            int[] rows = getScrollPositionFromConfig();
            if (rows != null && rows.length == 2) {
                getTable().scrollToRow(rows[0], rows[1]);
            }
        }
        return ret;
    }

    /**
     * @return
     */
    protected abstract int[] getScrollPositionFromConfig();

    @Override
    protected void setTableData(List<AbstractNode> data) {
        if (!(data instanceof PackageControllerTableModelData)) {
            throw new IllegalArgumentException("data must be instanceof PackageControllerTableModelData");
        } else {
            packageControllertableModelData = (PackageControllerTableModelData<PackageType, ChildrenType>) data;
            boolean vs = false;
            for (final AbstractNode node : data) {
                if (node instanceof AbstractPackageChildrenNode) {
                    if (((AbstractPackageChildrenNode) node).hasVariantSupport()) {
                        vs = true;
                        break;
                    }
                }
            }
            setVariantsSupport(vs);
        }
    }

    protected void setVariantsSupport(boolean vs) {
    }

    protected ExtColumn<AbstractNode> getDefaultSortColumn() {
        return null;
    }

    protected boolean isSortStateSaverEnabled() {
        return false;
    }

    @Override
    public String getNextSortIdentifier(String sortOrderIdentifier) {
        if (sortOrderIdentifier == null) {
            if (CFG_GUI.CFG.isPrimaryTableSorterDesc()) {
                sortOrderIdentifier = ExtColumn.SORT_ASC;
            } else {
                sortOrderIdentifier = ExtColumn.SORT_DESC;
            }
        }
        if (!isTristateSorterEnabled()) {
            if (sortOrderIdentifier.equals(ExtColumn.SORT_ASC)) {
                return ExtColumn.SORT_DESC;
            } else {
                return ExtColumn.SORT_ASC;
            }
        } else {
            if (CFG_GUI.CFG.isPrimaryTableSorterDesc()) {
                if (sortOrderIdentifier.equals(SORT_ORIGINAL)) {
                    return ExtColumn.SORT_DESC;
                } else if (sortOrderIdentifier.equals(ExtColumn.SORT_DESC)) {
                    return ExtColumn.SORT_ASC;
                } else {
                    return SORT_ORIGINAL;
                }
            } else {
                if (sortOrderIdentifier.equals(SORT_ORIGINAL)) {
                    return ExtColumn.SORT_ASC;
                } else if (sortOrderIdentifier.equals(ExtColumn.SORT_ASC)) {
                    return ExtColumn.SORT_DESC;
                } else {
                    return SORT_ORIGINAL;
                }
            }
        }
    }

    public Icon getSortIcon(String sortOrderIdentifier) {
        if (SORT_ORIGINAL.equals(sortOrderIdentifier)) {
            return null;
        }
        return super.getSortIcon(sortOrderIdentifier);
    }

    public long getTableDataVersion() {
        return getTableData().getVersion();
    }
}
