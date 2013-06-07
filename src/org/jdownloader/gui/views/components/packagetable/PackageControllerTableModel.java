package org.jdownloader.gui.views.components.packagetable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Icon;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.ChildComparator;
import jd.controlling.packagecontroller.PackageController;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTableModel;
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

    private abstract class TableDataModifier {
        public abstract void modifyTableData(java.util.List<PackageType> packages);
    }

    private boolean tristateSorterEnabled = true;

    public boolean isTristateSorterEnabled() {
        return tristateSorterEnabled;
    }

    public void setTristateSorterEnabled(boolean tristateSorterEnabled) {
        this.tristateSorterEnabled = tristateSorterEnabled;
    }

    private static final String                                                          SORT_ORIGINAL  = "ORIGINAL";

    private DelayedRunnable                                                              asyncRefresh;
    protected final PackageController<PackageType, ChildrenType>                         pc;
    private DelayedRunnable                                                              asyncRecreate  = null;
    private java.util.List<PackageControllerTableModelFilter<PackageType, ChildrenType>> tableFilters   = new ArrayList<PackageControllerTableModelFilter<PackageType, ChildrenType>>();
    private LinkedList<TableDataModifier>                                                tableModifiers = new LinkedList<TableDataModifier>();

    public java.util.List<PackageControllerTableModelFilter<PackageType, ChildrenType>> getTableFilters() {
        return tableFilters;
    }

    private Object                      LOCK  = new Object();

    private ScheduledThreadPoolExecutor queue = new ScheduledThreadPoolExecutor(1);

    private DelayedRunnable             asyncRecreateFast;

    public PackageControllerTableModel(final PackageController<PackageType, ChildrenType> pc, String id) {
        super(id);
        queue.setKeepAliveTime(10000, TimeUnit.MILLISECONDS);
        queue.allowCoreThreadTimeOut(true);
        resetSorting();
        this.pc = pc;
        asyncRefresh = new DelayedRunnable(queue, 150l, 250l) {
            AtomicBoolean repainting = new AtomicBoolean(false);

            @Override
            public void delayedrun() {
                java.util.List<PackageType> packages = getAllPackageNodes();
                for (PackageType pkg : packages) {
                    if (pkg.getView() != null && pkg.getView().updateRequired()) {
                        pkg.getView().update();
                    }
                }
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        if (repainting.get()) { return; }
                        /* we just want to repaint */
                        try {
                            repainting.set(true);
                            getTable().repaint();
                        } finally {
                            repainting.set(false);
                        }
                    }
                };
            }
        };
        asyncRecreate = new DelayedRunnable(queue, 50l, 1000l) {
            @Override
            public void delayedrun() {
                _fireTableStructureChanged(getTableData(), true);
            }
        };

        asyncRecreateFast = new DelayedRunnable(queue, 10l, 50l) {
            @Override
            public void delayedrun() {
                _fireTableStructureChanged(getTableData(), true);
            }
        };
    }

    public void resetSorting() {
        this.sortColumn = null;
        try {
            getStorage().put(ExtTableModel.SORT_ORDER_ID_KEY, (String) null);
            getStorage().put(ExtTableModel.SORTCOLUMN_KEY, (String) null);
        } catch (final Exception e) {
            Log.exception(e);
        }
    }

    public ScheduledThreadPoolExecutor getThreadPool() {
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

    public void sortPackageChildren(final AbstractPackageNode pkg, ChildComparator<ChildrenType> comparator) {
        this.resetSorting();

        pc.sortPackageChildren((PackageType) pkg, comparator);
    }

    public PackageController<PackageType, ChildrenType> getController() {
        return pc;
    }

    public PackageControllerTable<PackageType, ChildrenType> getPackageTable() {
        return (PackageControllerTable<PackageType, ChildrenType>) this.getTable();
    }

    public void toggleFilePackageExpand(final AbstractPackageNode fp2, final TOGGLEMODE mode) {
        synchronized (tableModifiers) {
            tableModifiers.add(new TableDataModifier() {

                @Override
                public void modifyTableData(java.util.List<PackageType> packages) {
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
                        java.util.List<PackageType> selectedPackages = PackageControllerTableModel.this.getPackageTable().getSelectedPackages();
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
            });
        }
        asyncRecreate.delayedrun();
    }

    public void setFilePackageExpand(final AbstractPackageNode fp2, final boolean expanded) {
        synchronized (tableModifiers) {
            tableModifiers.add(new TableDataModifier() {
                @Override
                public void modifyTableData(java.util.List<PackageType> packages) {
                    fp2.setExpanded(expanded);
                }
            });
        }
        asyncRecreate.delayedrun();
    }

    public void addFilter(PackageControllerTableModelFilter<PackageType, ChildrenType> filter) {
        synchronized (LOCK) {
            if (tableFilters.contains(filter)) return;
            java.util.List<PackageControllerTableModelFilter<PackageType, ChildrenType>> newfilters = new ArrayList<PackageControllerTableModelFilter<PackageType, ChildrenType>>(tableFilters);
            newfilters.add(filter);
            tableFilters = newfilters;
        }
    }

    public void removeFilter(PackageControllerTableModelFilter<PackageType, ChildrenType> filter) {
        synchronized (LOCK) {
            if (!tableFilters.contains(filter)) return;
            java.util.List<PackageControllerTableModelFilter<PackageType, ChildrenType>> newfilters = new ArrayList<PackageControllerTableModelFilter<PackageType, ChildrenType>>(tableFilters);
            newfilters.remove(filter);
            tableFilters = newfilters;
        }
    }

    public boolean isFilteredView() {
        java.util.List<PackageControllerTableModelFilter<PackageType, ChildrenType>> filters = tableFilters;
        for (PackageControllerTableModelFilter<PackageType, ChildrenType> filter : filters) {
            if (filter.highlightFilter()) return true;
        }
        return false;
    }

    @Override
    protected void initColumns() {
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
                getStorage().put(ExtTableModel.SORT_ORDER_ID_KEY, (String) null);
                getStorage().put(ExtTableModel.SORTCOLUMN_KEY, (String) null);
            } catch (final Exception e) {
                Log.exception(e);
            }
        } else {
            this.sortColumn = column;
            String id = column.getSortOrderIdentifier();
            try {
                getStorage().put(ExtTableModel.SORT_ORDER_ID_KEY, id);
                getStorage().put(ExtTableModel.SORTCOLUMN_KEY, column.getID());
            } catch (final Exception e) {
                Log.exception(e);
            }
        }

        java.util.List<PackageControllerTableModelFilter<PackageType, ChildrenType>> filters = this.tableFilters;
        ArrayList<PackageType> packages = pc.getPackagesCopy();
        /* filter packages */
        for (int index = packages.size() - 1; index >= 0; index--) {
            PackageType pkg = packages.get(index);
            for (PackageControllerTableModelFilter<PackageType, ChildrenType> filter : filters) {
                if (filter.isFiltered((PackageType) pkg)) {
                    pkg.getView().clear();
                    /* remove package because it is filtered */
                    packages.remove(index);
                    break;
                }
            }
        }
        /* sort packages */
        if (column != null) Collections.sort(packages, column.getRowSorter());
        synchronized (tableModifiers) {
            while (tableModifiers.size() > 0) {
                TableDataModifier modifier = tableModifiers.removeFirst();
                modifier.modifyTableData(packages);
            }
        }
        java.util.List<AbstractNode> newData = new ArrayList<AbstractNode>(Math.max(data.size(), packages.size()));
        for (PackageType node : packages) {
            ArrayList<ChildrenType> files = null;
            boolean readL = ((PackageType) node).getModifyLock().readLock();
            try {
                files = new ArrayList<ChildrenType>(((PackageType) node).getChildren());
            } finally {
                ((PackageType) node).getModifyLock().readUnlock(readL);
            }
            /* filter children of this package */
            for (int index = files.size() - 1; index >= 0; index--) {
                ChildrenType child = files.get(index);
                for (PackageControllerTableModelFilter<PackageType, ChildrenType> filter : filters) {
                    if (filter.isFiltered((ChildrenType) child)) {
                        /* remove child because it is filtered */
                        files.remove(index);
                        break;
                    }
                }
            }
            if (node.getView() != null) {
                if (files.size() == 0) {
                    node.getView().clear();
                    /* no visible children, skip PackageNode */
                    continue;
                } else {
                    node.getView().update(files);
                }
            }
            if (files.size() == 1 && hideSingleChildPackages) {
                newData.addAll(files);
            } else {
                boolean expanded = ((PackageType) node).isExpanded();
                if (column != null && expanded && files.size() > 0) {
                    /* we only have to sort children if the package is expanded */
                    Collections.sort(files, column.getRowSorter());
                }
                /* only add package node if it contains children */
                newData.add(node);
                if (!expanded) {
                    /* not expanded */
                    continue;
                } else {
                    /* expanded, add its children */
                    newData.addAll(files);
                }
            }
        }
        return newData;
    }

    protected ExtColumn<AbstractNode> getDefaultSortColumn() {
        return null;
    }

    protected boolean isSortStateSaverEnabled() {
        return false;
    }

    public List<ChildrenType> getAllChildrenNodes() {
        java.util.List<AbstractNode> data = this.getTableData();
        return getAllChildrenNodes(data);
    }

    public List<ChildrenType> getAllChildrenNodes(java.util.List<AbstractNode> data) {
        java.util.List<PackageControllerTableModelFilter<PackageType, ChildrenType>> filters = this.tableFilters;
        HashSet<ChildrenType> ret = new HashSet<ChildrenType>(data.size());
        for (AbstractNode node : data) {
            if (node instanceof AbstractPackageNode) {
                AbstractPackageNode pkg = (AbstractPackageNode) node;
                boolean readL = pkg.getModifyLock().readLock();
                try {
                    for (Object node2 : pkg.getChildren()) {
                        if (node2 instanceof AbstractPackageChildrenNode) {
                            boolean filtered = false;
                            for (PackageControllerTableModelFilter<PackageType, ChildrenType> filter : filters) {
                                if (filter.isFiltered((ChildrenType) node2)) {
                                    filtered = true;
                                    break;
                                }
                            }
                            if (filtered == false) {
                                ret.add((ChildrenType) node2);
                            }
                        }
                    }
                } finally {
                    pkg.getModifyLock().readUnlock(readL);
                }
            } else if (node instanceof AbstractPackageChildrenNode) {
                ret.add((ChildrenType) node);
            }
        }
        return new ArrayList<ChildrenType>(ret);
    }

    public List<PackageType> getAllPackageNodes() {
        java.util.List<AbstractNode> data = this.getTableData();
        java.util.List<PackageType> ret = new ArrayList<PackageType>(data.size());
        for (AbstractNode node : data) {
            if (node instanceof AbstractPackageNode) {
                ret.add((PackageType) node);
            }
        }
        return ret;
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
            if (sortOrderIdentifier == null || sortOrderIdentifier.equals(ExtColumn.SORT_ASC)) {
                return ExtColumn.SORT_DESC;
            } else if (sortOrderIdentifier.equals(ExtColumn.SORT_DESC)) {
                return SORT_ORIGINAL;
            } else {
                return ExtColumn.SORT_ASC;
            }
        }
    }

    public Icon getSortIcon(String sortOrderIdentifier) {
        if (SORT_ORIGINAL.equals(sortOrderIdentifier)) { return null; }
        return super.getSortIcon(sortOrderIdentifier);
    }
}
