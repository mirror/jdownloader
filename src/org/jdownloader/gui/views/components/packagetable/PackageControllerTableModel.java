package org.jdownloader.gui.views.components.packagetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Icon;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.ChildrenView;
import jd.controlling.packagecontroller.PackageController;
import jd.controlling.packagecontroller.PackageControllerComparator;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.Storage;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
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
    
    public abstract class TableDataModification {
        protected abstract void modifyTableData(List<PackageType> packages);
        
        protected abstract List<ChildrenType> modifyPackageData(PackageType pkg, List<ChildrenType> unfilteredChildren);
        
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
    
    private static final String                                                               SORT_ORIGINAL         = "ORIGINAL";
    
    private final DelayedRunnable                                                             asyncRefresh;
    protected final PackageController<PackageType, ChildrenType>                              pc;
    private final DelayedRunnable                                                             asyncRecreate;
    private CopyOnWriteArraySet<PackageControllerTableModelFilter<PackageType, ChildrenType>> availableTableFilters = new CopyOnWriteArraySet<PackageControllerTableModelFilter<PackageType, ChildrenType>>();
    private CopyOnWriteArrayList<TableDataModification>                                       tableModifiers        = new CopyOnWriteArrayList<TableDataModification>();
    protected PackageControllerTableModelData<PackageType, ChildrenType>                      tableData             = new PackageControllerTableModelData<PackageType, ChildrenType>();
    
    public Collection<PackageControllerTableModelFilter<PackageType, ChildrenType>> getAvailableTableFilters() {
        return availableTableFilters;
    }
    
    private ScheduledExecutorService queue                 = DelayedRunnable.getNewScheduledExecutorService();
    
    private final DelayedRunnable    asyncRecreateFast;
    
    private final Storage            storage;
    
    private final AtomicBoolean      repaintFired          = new AtomicBoolean(false);
    private final AtomicBoolean      structureChangedFired = new AtomicBoolean(false);
    
    public PackageControllerTableModel(final PackageController<PackageType, ChildrenType> pc, String id) {
        super(id);
        storage = getStorage();
        resetSorting();
        
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
    }
    
    private void fireRepaint() {
        if (repaintFired.compareAndSet(false, true)) {
            pc.getQueue().add(new QueueAction<Void, RuntimeException>(Queue.QueuePriority.HIGH) {
                
                @Override
                protected Void run() throws RuntimeException {
                    try {
                        ArrayList<ChildrenView<ChildrenType>> viewUpdates = new ArrayList<ChildrenView<ChildrenType>>();
                        for (AbstractNode node : getTableData()) {
                            if (node instanceof AbstractPackageNode) {
                                ChildrenView<ChildrenType> view = ((AbstractPackageNode) node).getView();
                                if (view.updateRequired()) viewUpdates.add(view);
                            }
                        }
                        for (ChildrenView<ChildrenType> view : viewUpdates) {
                            view.aggregate();
                        }
                    } finally {
                        repaintFired.set(false);
                    }
                    new EDTRunner() {
                        @Override
                        protected void runInEDT() {
                            /* we just want to repaint */
                            getTable().repaint();
                        }
                    };
                    return null;
                }
            });
        }
    }
    
    private void fireStructureChange() {
        if (structureChangedFired.compareAndSet(false, true)) {
            pc.getQueue().add(new QueueAction<Void, RuntimeException>(Queue.QueuePriority.HIGH) {
                
                @Override
                protected Void run() throws RuntimeException {
                    try {
                        _fireTableStructureChanged(getTableData(), true);
                    } finally {
                        structureChangedFired.set(false);
                    }
                    return null;
                }
            });
        }
    }
    
    public void resetSorting() {
        this.sortColumn = null;
        try {
            storage.put(ExtTableModel.SORT_ORDER_ID_KEY, (String) null);
            storage.put(ExtTableModel.SORTCOLUMN_KEY, (String) null);
        } catch (final Exception e) {
            Log.exception(e);
        }
    }
    
    public ScheduledExecutorService getThreadPool() {
        return queue;
    }
    
    public void recreateModel(boolean delay) {
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
        if (delay) {
            asyncRefresh.run();
        } else {
            asyncRefresh.delayedrun();
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
        final java.util.List<PackageType> selectedPackages = PackageControllerTableModel.this.getTable().getSelectedPackages();
        tableModifiers.add(new TableDataModification() {
            
            @Override
            protected void modifyTableData(java.util.List<PackageType> packages) {
                final boolean cur = !fp2.isExpanded();
                boolean doToggle = true;
                switch (mode) {
                    case CURRENT:
                        fp2.setExpanded(cur);
                        break;
                    case TOP:
                        doToggle = true;
                        break;
                    case BOTTOM:
                        doToggle = false;
                        break;
                }
                if (mode != TOGGLEMODE.CURRENT) {
                    if (selectedPackages.size() > 1) {
                        for (PackageType fp : selectedPackages) {
                            fp.setExpanded(cur);
                        }
                        return;
                    }
                    for (PackageType fp : packages) {
                        if (doToggle) {
                            fp.setExpanded(cur);
                            if (fp == fp2) doToggle = false;
                        } else {
                            if (fp == fp2) {
                                doToggle = true;
                                fp.setExpanded(cur);
                            }
                        }
                    }
                }
            }
            
            @Override
            protected List<ChildrenType> modifyPackageData(PackageType pkg, List<ChildrenType> unfilteredChildren) {
                return unfilteredChildren;
            }
        });
        asyncRecreateFast.delayedrun();
    }
    
    public void setFilePackageExpand(final boolean expanded, final AbstractPackageNode... fp2) {
        tableModifiers.add(new TableDataModification() {
            @Override
            protected void modifyTableData(java.util.List<PackageType> packages) {
                for (AbstractPackageNode fp : fp2) {
                    fp.setExpanded(expanded);
                }
            }
            
            @Override
            protected List<ChildrenType> modifyPackageData(PackageType pkg, List<ChildrenType> unfilteredChildren) {
                return unfilteredChildren;
            }
        });
        asyncRecreateFast.delayedrun();
    }
    
    public void addFilter(PackageControllerTableModelFilter<PackageType, ChildrenType> filter) {
        if (filter == null) return;
        availableTableFilters.add(filter);
    }
    
    public boolean isFilteredView() {
        return getTableData().isFiltered();
    }
    
    @Override
    public PackageControllerTableModelData<PackageType, ChildrenType> getTableData() {
        return tableData;
    }
    
    public void removeFilter(PackageControllerTableModelFilter<PackageType, ChildrenType> filter) {
        if (filter == null) return;
        availableTableFilters.remove(filter);
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
        
        private CompiledFilterList(List<PackageControllerTableModelFilter<PackageType, ChildrenType>> packageFilters, List<PackageControllerTableModelFilter<PackageType, ChildrenType>> childrenFilters) {
            this.packageFilters = packageFilters;
            this.childrenFilters = childrenFilters;
        }
        
    }
    
    protected CompiledFilterList compileFilterList(Collection<PackageControllerTableModelFilter<PackageType, ChildrenType>> filters) {
        ArrayList<PackageControllerTableModelFilter<PackageType, ChildrenType>> packageFilters = new ArrayList<PackageControllerTableModelFilter<PackageType, ChildrenType>>();
        ArrayList<PackageControllerTableModelFilter<PackageType, ChildrenType>> childrendFilters = new ArrayList<PackageControllerTableModelFilter<PackageType, ChildrenType>>();
        if (filters == null || filters.size() == 0) return new CompiledFilterList(packageFilters, childrendFilters);
        for (PackageControllerTableModelFilter<PackageType, ChildrenType> filter : filters) {
            if (filter.isFilteringPackageNodes()) packageFilters.add(filter);
            if (filter.isFilteringChildrenNodes()) childrendFilters.add(filter);
        }
        if (packageFilters.size() > 0 || childrendFilters.size() > 0) {
            Comparator<PackageControllerTableModelFilter<PackageType, ChildrenType>> comparator = new Comparator<PackageControllerTableModelFilter<PackageType, ChildrenType>>() {
                
                public int compare(int x, int y) {
                    return (x < y) ? -1 : ((x == y) ? 0 : 1);
                }
                
                @Override
                public int compare(PackageControllerTableModelFilter<PackageType, ChildrenType> o1, PackageControllerTableModelFilter<PackageType, ChildrenType> o2) {
                    return compare(o1.getComplexity(), o2.getComplexity());
                }
            };
            if (packageFilters.size() > 0) Collections.sort(packageFilters, comparator);
            if (childrendFilters.size() > 0) Collections.sort(childrendFilters, comparator);
        }
        return new CompiledFilterList(packageFilters, childrendFilters);
    }
    
    public List<PackageControllerTableModelFilter<PackageType, ChildrenType>> getEnabledTableFilters() {
        ArrayList<PackageControllerTableModelFilter<PackageType, ChildrenType>> ret = new ArrayList<PackageControllerTableModelFilter<PackageType, ChildrenType>>();
        for (PackageControllerTableModelFilter<PackageType, ChildrenType> filter : getAvailableTableFilters()) {
            if (filter.isFilteringPackageNodes() || filter.isFilteringChildrenNodes()) ret.add(filter);
        }
        Comparator<PackageControllerTableModelFilter<PackageType, ChildrenType>> comparator = new Comparator<PackageControllerTableModelFilter<PackageType, ChildrenType>>() {
            
            public int compare(int x, int y) {
                return (x < y) ? -1 : ((x == y) ? 0 : 1);
            }
            
            @Override
            public int compare(PackageControllerTableModelFilter<PackageType, ChildrenType> o1, PackageControllerTableModelFilter<PackageType, ChildrenType> o2) {
                return compare(o1.getComplexity(), o2.getComplexity());
            }
        };
        Collections.sort(ret, comparator);
        return ret;
    }
    
    public List<AbstractNode> refreshUnSort(final List<AbstractNode> data) {
        if (data instanceof PackageControllerTableModelData) return data;
        throw new IllegalArgumentException("data must be instanceof PackageControllerTableModelData");
    }
    
    /*
     * we override sort to have a better sorting of packages/files, to keep their structure alive,data is only used to specify the size of the new ArrayList
     */
    @Override
    public java.util.List<AbstractNode> sort(final java.util.List<AbstractNode> data, ExtColumn<AbstractNode> column) {
        boolean hideSingleChildPackages = CFG_GUI.HIDE_SINGLE_CHILD_PACKAGES.isEnabled();
        if (column == null || column.getSortOrderIdentifier() == SORT_ORIGINAL) {
            /* RESET sorting to nothing,tri-state */
            this.sortColumn = column = null;
            try {
                storage.put(ExtTableModel.SORT_ORDER_ID_KEY, (String) null);
                storage.put(ExtTableModel.SORTCOLUMN_KEY, (String) null);
            } catch (final Exception e) {
                Log.exception(e);
            }
        } else {
            this.sortColumn = column;
            String id = column.getSortOrderIdentifier();
            try {
                storage.put(ExtTableModel.SORT_ORDER_ID_KEY, id);
                storage.put(ExtTableModel.SORTCOLUMN_KEY, column.getID());
            } catch (final Exception e) {
                Log.exception(e);
            }
        }
        CompiledFilterList filters = compileFilterList(getAvailableTableFilters());
        ArrayList<PackageType> packages = pc.getPackagesCopy();
        /* filter packages */
        boolean hasPackageFilters = filters.getPackageFilters().size() > 0;
        if (hasPackageFilters) {
            for (int index = packages.size() - 1; index >= 0; index--) {
                PackageType pkg = packages.get(index);
                for (PackageControllerTableModelFilter<PackageType, ChildrenType> filter : filters.getPackageFilters()) {
                    if (filter.isFiltered(pkg)) {
                        /* remove package because it is filtered */
                        packages.remove(index);
                        break;
                    }
                }
            }
        }
        /* sort packages */
        if (column != null) Collections.sort(packages, column.getRowSorter());
        List<TableDataModification> appliedTableDataModifier = new ArrayList<TableDataModification>(tableModifiers);
        tableModifiers.removeAll(appliedTableDataModifier);
        for (TableDataModification modifier : appliedTableDataModifier) {
            modifier.modifyTableData(packages);
        }
        PackageControllerTableModelData<PackageType, ChildrenType> newData = new PackageControllerTableModelData<PackageType, ChildrenType>(Math.max(data.size(), packages.size()));
        boolean hasChildrenFilters = filters.getChildrenFilters().size() > 0;
        ArrayList<ChildrenType> unfilteredChildrenNodes = new ArrayList<ChildrenType>();
        for (PackageType node : packages) {
            List<ChildrenType> files = null;
            boolean readL = node.getModifyLock().readLock();
            try {
                files = new ArrayList<ChildrenType>(node.getChildren());
            } finally {
                node.getModifyLock().readUnlock(readL);
            }
            if (hasChildrenFilters) {
                ArrayList<ChildrenType> reverseUnfilteredChildrenNotes = new ArrayList<ChildrenType>();
                /* filter children of this package */
                childLoop: for (int index = files.size() - 1; index >= 0; index--) {
                    ChildrenType child = files.get(index);
                    for (PackageControllerTableModelFilter<PackageType, ChildrenType> filter : filters.getChildrenFilters()) {
                        if (filter.isFiltered(child)) {
                            /* remove child because it is filtered */
                            files.remove(index);
                            continue childLoop;
                        }
                    }
                    reverseUnfilteredChildrenNotes.add(child);
                }
                if (reverseUnfilteredChildrenNotes.size() > 0) {
                    Collections.reverse(reverseUnfilteredChildrenNotes);
                    unfilteredChildrenNodes.addAll(reverseUnfilteredChildrenNotes);
                }
            } else {
                unfilteredChildrenNodes.addAll(files);
            }
            for (TableDataModification modifier : appliedTableDataModifier) {
                files = modifier.modifyPackageData(node, files);
            }
            if (node.getView() != null) {
                if (files.size() == 0) {
                    /* no visible children, skip PackageNode */
                    continue;
                } else {
                    node.getView().setItems(files);
                }
            }
            if (files.size() == 1 && hideSingleChildPackages) {
                newData.addAll(files);
            } else {
                boolean expanded = ((PackageType) node).isExpanded();
                /* only add package node if it contains children */
                newData.add(node);
                if (expanded) {
                    /* expanded, add its children */
                    if (column != null && files.size() > 1) {
                        /* we only have to sort children if the package is expanded */
                        Collections.sort(files, column.getRowSorter());
                    }
                    newData.addAll(files);
                }
            }
        }
        List<PackageControllerTableModelCustomizer> tableDataCustomizer = new ArrayList<PackageControllerTableModelCustomizer>();
        for (TableDataModification modifier : appliedTableDataModifier) {
            PackageControllerTableModelCustomizer customizer = modifier.finalizeTableModification();
            if (customizer != null) tableDataCustomizer.add(customizer);
        }
        if (hasPackageFilters) newData.setPackageFilters(filters.getPackageFilters());
        if (hasChildrenFilters) newData.setChildrenFilters(filters.getChildrenFilters());
        newData.setTableModelCustomizer(tableDataCustomizer);
        newData.setAllChildrenNodes(unfilteredChildrenNodes);
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
        return ret && super.postSetTableData(newtableData);
    }
    
    @Override
    protected void setTableData(List<AbstractNode> data) {
        if (!(data instanceof PackageControllerTableModelData)) {
            throw new IllegalArgumentException("data must be instanceof PackageControllerTableModelData");
        } else {
            tableData = (PackageControllerTableModelData) data;
            boolean vs = false;
            for (AbstractNode node : tableData.getAllChildrenNodes()) {
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
    
    public List<ChildrenType> getAllChildrenNodes() {
        return getTableData().getAllChildrenNodes();
    }
    
    @Override
    public String getNextSortIdentifier(String sortOrderIdentifier) {
        if (!isTristateSorterEnabled()) {
            if (sortOrderIdentifier == null || sortOrderIdentifier.equals(ExtColumn.SORT_ASC)) {
                return ExtColumn.SORT_DESC;
            } else {
                return ExtColumn.SORT_ASC;
            }
        } else {
            if (sortOrderIdentifier == null || sortOrderIdentifier.equals(SORT_ORIGINAL)) {
                return ExtColumn.SORT_ASC;
                
            } else if (sortOrderIdentifier.equals(ExtColumn.SORT_ASC)) {
                return ExtColumn.SORT_DESC;
            } else {
                return SORT_ORIGINAL;
            }
        }
    }
    
    public Icon getSortIcon(String sortOrderIdentifier) {
        if (SORT_ORIGINAL.equals(sortOrderIdentifier)) { return null; }
        return super.getSortIcon(sortOrderIdentifier);
    }
    
    public long getTableDataVersion() {
        return tableData.getVersion();
    }
}
