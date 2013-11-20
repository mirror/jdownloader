package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import jd.SecondLevelLaunch;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelCustomizer;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelData;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelFilter;
import org.jdownloader.gui.views.components.packagetable.context.EnabledAction;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.contextmenu.MenuManagerLinkgrabberTableContext;
import org.jdownloader.logging.LogController;
import org.jdownloader.updatev2.gui.LAFOptions;

public abstract class FilterTable extends BasicJDTable<Filter> implements PackageControllerTableModelFilter<CrawledPackage, CrawledLink> {

    private static class FilterTableUpdater {

        private AtomicBoolean update = new AtomicBoolean(false);
        private FilterTable   table;

        public FilterTable getTable() {
            return table;
        }

        private FilterTableUpdater(FilterTable table) {
            this.table = table;
        }

        public AtomicBoolean getUpdate() {
            return update;
        }

    }

    /**
     * 
     */
    private static final long                              serialVersionUID      = -5917220196056769905L;
    private HeaderInterface                                header;
    private LinkGrabberTable                               linkgrabberTable;
    /* all instances share single DelayedRunnable to avoid multiple refreshing */
    private static CopyOnWriteArraySet<FilterTableUpdater> FILTERTABLES          = new CopyOnWriteArraySet<FilterTable.FilterTableUpdater>();
    private static ScheduledExecutorService                EXECUTER              = DelayedRunnable.getNewScheduledExecutorService();
    protected static final long                            SELECTION_REFRESH_MIN = 25l;
    protected static final long                            SELECTION_REFRESH_MAX = 100l;
    private static DelayedRunnable                         SELECTIONUPDATER      = new DelayedRunnable(EXECUTER, SELECTION_REFRESH_MIN, SELECTION_REFRESH_MAX) {

                                                                                     @Override
                                                                                     public void delayedrun() {
                                                                                         if (org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.QUICK_VIEW_SELECTION_ENABLED.getValue()) {
                                                                                             LinkGrabberTableModel.getInstance().addTableModifier(LinkGrabberTableModel.getInstance().new TableDataModification() {
                                                                                                 final List<Filter>       selectedFilters      = getSelectedFilters();
                                                                                                 final List<AbstractNode> selectedCrawledLinks = new ArrayList<AbstractNode>();

                                                                                                 @Override
                                                                                                 protected void modifyTableData(List<CrawledPackage> packages) {
                                                                                                 }

                                                                                                 @Override
                                                                                                 protected List<CrawledLink> modifyPackageData(CrawledPackage pkg, List<CrawledLink> unfilteredChildren) {
                                                                                                     boolean expand = false;
                                                                                                     for (CrawledLink link : unfilteredChildren) {
                                                                                                         for (Filter filter : selectedFilters) {
                                                                                                             if (filter.isFiltered(link)) {
                                                                                                                 expand = true;
                                                                                                                 selectedCrawledLinks.add(link);
                                                                                                                 break;
                                                                                                             }
                                                                                                         }
                                                                                                     }
                                                                                                     if (expand) pkg.setExpanded(true);
                                                                                                     return unfilteredChildren;
                                                                                                 }

                                                                                                 @Override
                                                                                                 protected PackageControllerTableModelCustomizer finalizeTableModification() {
                                                                                                     return new PackageControllerTableModelCustomizer() {

                                                                                                         @Override
                                                                                                         public boolean customizedTableData() {
                                                                                                             LinkGrabberTableModel.getInstance().setSelectedObjects(selectedCrawledLinks);
                                                                                                             return false;
                                                                                                         }
                                                                                                     };
                                                                                                 }

                                                                                             }, false);

                                                                                         }
                                                                                     }
                                                                                 };
    protected static final long                            FILTER_REFRESH_MIN    = 500l;
    protected static final long                            FILTER_REFRESH_MAX    = 2000l;
    private static DelayedRunnable                         FILTERTABLESUPDATER   = new DelayedRunnable(EXECUTER, FILTER_REFRESH_MIN, FILTER_REFRESH_MAX) {
                                                                                     @Override
                                                                                     public String getID() {
                                                                                         return "FilterTable";
                                                                                     }

                                                                                     @Override
                                                                                     public void delayedrun() {
                                                                                         try {
                                                                                             ArrayList<FilterTableDataUpdater> updater = new ArrayList<FilterTableDataUpdater>();
                                                                                             for (FilterTableUpdater filterTable : FILTERTABLES) {
                                                                                                 if (filterTable.getUpdate().getAndSet(false)) {
                                                                                                     updater.add(filterTable.getTable().getFilterTableDataUpdater());
                                                                                                 }
                                                                                             }
                                                                                             updateFilterTables(updater);
                                                                                         } catch (final Throwable e) {
                                                                                             LogController.GL.log(e);
                                                                                         }
                                                                                     }

                                                                                 };

    private static TableModelListener                      TABLELISTENER         = new TableModelListener() {

                                                                                     @Override
                                                                                     public void tableChanged(TableModelEvent e) {
                                                                                         updateAllFiltersInstant();
                                                                                     }

                                                                                 };

    private static volatile Filter                         filterException       = null;
    private static volatile Thread                         filterExceptionThread = null;
    private BooleanKeyHandler                              visibleKeyHandler;
    private GenericConfigEventListener<Boolean>            sidebarListener;
    private final FilterTableUpdater                       filterTableUpdater;

    private static void updateAllFiltersInstant() {
        for (FilterTableUpdater filterTable : FILTERTABLES) {
            filterTable.getUpdate().set(true);
        }
        FILTERTABLESUPDATER.run();
    }

    private static void updateFilterTables(List<FilterTableDataUpdater> updater) {
        if (updater.size() == 0) return;
        for (FilterTableDataUpdater update : updater) {
            update.reset();
        }
        Set<CrawledLink> processedLinks = new HashSet<CrawledLink>();
        List<CrawledLink> filteredLinks = new ArrayList<CrawledLink>();
        List<CrawledLink> allCollectorChildren = LinkCollector.getInstance().getAllChildren();
        PackageControllerTableModelData<CrawledPackage, CrawledLink> tableData = LinkGrabberTableModel.getInstance().getTableData();
        for (CrawledLink link : tableData.getAllChildrenNodes()) {
            processedLinks.add(link);
            for (FilterTableDataUpdater update : updater) {
                update.updateVisible(link);
            }
        }
        for (FilterTableDataUpdater update : updater) {
            update.afterVisible();
        }
        for (CrawledLink link : allCollectorChildren) {
            if (processedLinks.add(link)) {
                filteredLinks.add(link);
                for (FilterTableDataUpdater update : updater) {
                    update.updateFiltered(link);
                }
            }
        }
        HashMap<FilterTable, List<Filter>> updateTableData = new HashMap<FilterTable, List<Filter>>();
        boolean newDisabledFilters = false;
        for (FilterTableDataUpdater update : updater) {
            if (update.hasNewDisabledFilters()) newDisabledFilters = true;
            FilterTable filterTable = update.getFilterTable();
            List<Filter> filters = update.finalizeUpdater();
            if (filters.size() > 0 && filterTable.visibleKey()) {
                try {
                    filterExceptionThread = Thread.currentThread();
                    for (Filter filter : filters) {
                        if (filter.getCounter() == 0) {
                            filter.setCounter(getCountWithout(filter, filteredLinks, tableData.getChildrenFilters()));
                        }
                    }
                } finally {
                    filterExceptionThread = null;
                }
            }
            updateTableData.put(filterTable, filters);
        }

        Iterator<Entry<FilterTable, List<Filter>>> it = updateTableData.entrySet().iterator();
        while (it.hasNext()) {
            Entry<FilterTable, List<Filter>> next = it.next();
            next.getKey().updateTableData(next.getValue());
        }
        if (newDisabledFilters) LinkGrabberTableModel.getInstance().recreateModel(false);
    }

    protected static int getCountWithout(Filter filter, List<CrawledLink> filteredLinks, List<PackageControllerTableModelFilter<CrawledPackage, CrawledLink>> tableFilters) {
        if (tableFilters == null || tableFilters.size() == 0) return 0;
        int ret = 0;
        main: for (CrawledLink l : filteredLinks) {
            if (filter.isFiltered(l)) {
                try {
                    filterException = filter;
                    for (PackageControllerTableModelFilter<CrawledPackage, CrawledLink> f : tableFilters) {
                        if (f.isFiltered(l)) {
                            continue main;
                        }
                    }
                } finally {
                    filterException = null;
                }
                ret++;
            }
        }
        return ret;
    }

    public FilterTable(HeaderInterface hosterFilter, LinkGrabberTable table, final BooleanKeyHandler visible) {
        super(new FilterTableModel());
        header = hosterFilter;
        this.visibleKeyHandler = visible;
        header.setFilterCount(0);
        this.linkgrabberTable = table;
        filterTableUpdater = new FilterTableUpdater(this);
        this.setShowVerticalLines(false);
        this.setShowGrid(false);
        this.setShowHorizontalLines(false);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        this.setBackground((LAFOptions.getInstance().getColorForPanelBackground()));

        this.setIntercellSpacing(new Dimension(0, 0));

        sidebarListener = new GenericConfigEventListener<Boolean>() {

            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                if (Boolean.TRUE.equals(newValue) && org.jdownloader.settings.staticreferences.CFG_GUI.CFG.isLinkgrabberSidebarVisible()) {
                    boolean addListener = FILTERTABLES.size() == 0;
                    FILTERTABLES.add(filterTableUpdater);
                    linkgrabberTable.getModel().addFilter(FilterTable.this);
                    if (addListener) linkgrabberTable.getModel().addTableModelListener(TABLELISTENER);
                    FilterTable.super.setVisible(true);
                } else {
                    FILTERTABLES.remove(filterTableUpdater);
                    linkgrabberTable.getModel().removeFilter(FilterTable.this);
                    if (FILTERTABLES.size() == 0) FilterTable.this.linkgrabberTable.getModel().removeTableModelListener(TABLELISTENER);
                    FilterTable.super.setVisible(false);
                }
                linkgrabberTable.getModel().recreateModel(false);
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        };
        visible.getEventSender().addListener(sidebarListener);
        org.jdownloader.settings.staticreferences.CFG_GUI.LINKGRABBER_SIDEBAR_VISIBLE.getEventSender().addListener(sidebarListener, true);
        SecondLevelLaunch.INIT_COMPLETE.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                sidebarListener.onConfigValueModified(null, visible.getValue());
            }
        });

    }

    abstract protected FilterTableDataUpdater getFilterTableDataUpdater();

    protected void requestUpdate() {
        filterTableUpdater.getUpdate().set(true);
        FILTERTABLESUPDATER.run();
    }

    protected void onSelectionChanged() {
        if (!getModel().isTableStructureChanging() && this.hasFocus()) {
            SELECTIONUPDATER.run();
        }
    }

    private static List<Filter> getSelectedFilters() {
        final List<Filter> ret = new ArrayList<Filter>();
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                for (FilterTableUpdater filterTableUpdater : FILTERTABLES) {
                    FilterTable f = filterTableUpdater.getTable();
                    ret.addAll(f.getModel().getSelectedObjects());
                }
            }
        }.waitForEDT();
        return ret;
    }

    protected JPopupMenu onContextMenu(final JPopupMenu popup, final Filter contextObject, final java.util.List<Filter> selection, final ExtColumn<Filter> column, final MouseEvent mouseEvent) {
        return MenuManagerLinkgrabberTableContext.getInstance().build();
    }

    protected void processMouseEvent(final MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_PRESSED && !e.isControlDown()) {
            if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1) {
                final int row = this.rowAtPoint(e.getPoint());
                ExtColumn<Filter> col = this.getExtColumnAtPoint(e.getPoint());
                if (isRowSelected(row) && !(col instanceof ExtCheckColumn)) {
                    // clearSelection();
                    if (getSelectedRows().length > 1) {
                        getSelectionModel().setSelectionInterval(row, row);
                    } else {
                        getSelectionModel().removeSelectionInterval(row, row);
                    }
                    return;
                }
            }
        }
        super.processMouseEvent(e);
    }

    @Override
    protected boolean onSingleClick(MouseEvent e, Filter obj) {
        if (!e.isControlDown()) {
            for (FilterTableUpdater filterTableUpdater : FILTERTABLES) {
                FilterTable f = filterTableUpdater.getTable();
                if (f == FilterTable.this) continue;
                f.clearSelection();
            }
            return true;
        }
        return false;
    }

    protected boolean processKeyBinding(final KeyStroke stroke, final KeyEvent evt, final int condition, final boolean pressed) {
        if (!pressed) { return super.processKeyBinding(stroke, evt, condition, pressed); }
        switch (evt.getKeyCode()) {
        case KeyEvent.VK_ENTER:
        case KeyEvent.VK_BACK_SPACE:
        case KeyEvent.VK_DELETE:
            EnabledAction action = new EnabledAction();
            action.requestUpdate(null);
            action.actionPerformed(null);
            return true;
        case KeyEvent.VK_X:
        case KeyEvent.VK_V:
        case KeyEvent.VK_C:
            /* ignore copy, paste,cut */
            return true;
        case KeyEvent.VK_UP:
        case KeyEvent.VK_DOWN:
            /* ignore move */
            if (evt.isMetaDown() || evt.isControlDown()) { return true; }
        }
        return super.processKeyBinding(stroke, evt, condition, pressed);
    }

    protected boolean visibleKey() {
        return visibleKeyHandler.getValue();
    }

    protected void updateTableData(List<Filter> filter) {
        boolean enabled = filter.size() > 0;
        setVisible(enabled);
        if (visibleKey()) {
            if (enabled) {
                getModel()._fireTableStructureChanged(filter, true);
            } else {
                getModel().clear();
            }
        }
    }

    protected static Filter getFilterException() {
        if (filterException != null && Thread.currentThread() == filterExceptionThread) return filterException;
        return null;
    }

    //
    // protected int getCountWithout(Filter filter, java.util.List<CrawledLink> filteredLinks) {
    // java.util.List<PackageControllerTableModelFilter<CrawledPackage, CrawledLink>> tableFilters = getLinkgrabberTable().getModel().getTableFilters();
    // int ret = 0;
    // main: for (CrawledLink l : filteredLinks) {
    // if (filter.isFiltered(l)) {
    // filterException = filter;
    // try {
    // for (PackageControllerTableModelFilter<CrawledPackage, CrawledLink> f : tableFilters) {
    // if (f.isFiltered(l)) {
    // continue main;
    // }
    // }
    // } finally {
    // filterException = null;
    // }
    // ret++;
    // }
    // }
    // return ret;
    // }

    public LinkGrabberTable getLinkgrabberTable() {
        return linkgrabberTable;
    }

    public void setVisible(final boolean aFlag) {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                header.setEnabled(aFlag);
                FilterTable.super.setVisible(aFlag && visibleKey());
            }
        };
    }

    public boolean isEnabled() {
        return FILTERTABLES.size() > 0;
    }

    @Override
    public boolean isFilteringPackageNodes() {
        return false;
    }

    @Override
    public boolean isFiltered(CrawledPackage e) {
        return false;
    }

}
