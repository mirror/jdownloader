package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import jd.SecondLevelLaunch;
import jd.controlling.TaskQueue;
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
import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelFilter;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmSelectionContextAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.CreateDLCAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.MergeToPackageAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.RemoveNonSelectedContextAction;
import org.jdownloader.gui.views.linkgrabber.contextmenu.RemoveSelectionLinkgrabberAction;
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
    private static final long                              serialVersionUID   = -5917220196056769905L;
    protected java.util.List<Filter>                       filters            = new ArrayList<Filter>();
    protected volatile boolean                             enabled            = false;
    private HeaderInterface                                header;
    private LinkGrabberTable                               linkgrabberTable;
    protected static final long                            REFRESH_MIN        = 200l;
    protected static final long                            REFRESH_MAX        = 2000l;
    /* all instances share single DelayedRunnable to avoid multiple refreshing */
    private static CopyOnWriteArraySet<FilterTableUpdater> filterTableUpdates = new CopyOnWriteArraySet<FilterTable.FilterTableUpdater>();
    private static DelayedRunnable                         delayedRefresh     = new DelayedRunnable(REFRESH_MIN, REFRESH_MAX) {
                                                                                  @Override
                                                                                  public String getID() {
                                                                                      return "FilterTable";
                                                                                  }

                                                                                  @Override
                                                                                  public void delayedrun() {
                                                                                      TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>(Queue.QueuePriority.HIGH) {

                                                                                          @Override
                                                                                          protected Void run() throws RuntimeException {
                                                                                              for (FilterTableUpdater filterTable : filterTableUpdates) {
                                                                                                  if (filterTable.getUpdate().getAndSet(false)) {
                                                                                                      filterTable.getTable().updateNow();
                                                                                                  }
                                                                                              }
                                                                                              return null;
                                                                                          }
                                                                                      });
                                                                                  }

                                                                              };

    private TableModelListener                             listener;
    private BooleanKeyHandler                              visibleKeyHandler;
    private Filter                                         filterException;
    private GenericConfigEventListener<Boolean>            sidebarListener;
    private final FilterTableUpdater                       filterTableUpdater;

    public FilterTable(HeaderInterface hosterFilter, LinkGrabberTable table, final BooleanKeyHandler visible) {
        super(new FilterTableModel());
        header = hosterFilter;
        this.visibleKeyHandler = visible;
        header.setFilterCount(0);
        this.linkgrabberTable = table;
        filterTableUpdater = new FilterTableUpdater(this);
        listener = new TableModelListener() {

            public void tableChanged(TableModelEvent e) {
                requestUpdate();
            }
        };
        this.setShowVerticalLines(false);
        this.setShowGrid(false);
        this.setShowHorizontalLines(false);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        this.setBackground((LAFOptions.getInstance().getColorForPanelBackground()));

        // this.addRowHighlighter(new SelectionHighlighter(null, b2));
        // this.getModel().addExtComponentRowHighlighter(new
        // ExtComponentRowHighlighter<T>(f2, b2, null) {
        //
        // @Override
        // public boolean accept(ExtColumn<T> column, T value, boolean selected,
        // boolean focus, int row) {
        // return selected;
        // }
        //
        // });

        // this.addRowHighlighter(new AlternateHighlighter(null,
        // ColorUtils.getAlphaInstance(new JLabel().getForeground(), 6)));
        this.setIntercellSpacing(new Dimension(0, 0));

        sidebarListener = new GenericConfigEventListener<Boolean>() {

            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                if (Boolean.TRUE.equals(newValue) && org.jdownloader.settings.staticreferences.CFG_GUI.CFG.isLinkgrabberSidebarVisible()) {
                    enabled = true;
                    filterTableUpdates.add(filterTableUpdater);
                    linkgrabberTable.getModel().addFilter(FilterTable.this);
                    linkgrabberTable.getModel().addTableModelListener(listener);
                    FilterTable.super.setVisible(true);
                } else {
                    enabled = false;
                    filterTableUpdates.remove(filterTableUpdater);
                    linkgrabberTable.getModel().removeFilter(FilterTable.this);
                    FilterTable.this.linkgrabberTable.getModel().removeTableModelListener(listener);
                    FilterTable.super.setVisible(false);
                }
                updateAllFiltersInstant();
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

    protected void requestUpdate() {
        filterTableUpdater.getUpdate().set(true);
        delayedRefresh.run();
    }

    protected void onSelectionChanged() {
        updateSelection();
    }

    protected void initMouseOverRowHighlighter() {
        super.initMouseOverRowHighlighter();
    }

    private void updateSelection() {

        // clear selection in other filter tables if we switched to a new one
        if (this.hasFocus()) {
            /* quick and dirty solution */
            // org.jdownloader.settings.statics.LINKFILTER.CFG
            if (org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.QUICK_VIEW_SELECTION_ENABLED.getValue()) {
                java.util.List<Filter> selection = getSelectedFilters();
                java.util.List<AbstractNode> newSelection = getMatches(selection);
                List<CrawledPackage> expands = getPackagesToExpand(selection);
                for (CrawledPackage pkg : expands) {
                    LinkGrabberTableModel.getInstance().setFilePackageExpand(pkg, true);
                }
                System.out.println("refresh");
                getLinkgrabberTable().getModel().setSelectedObjects(newSelection);
            }
        }

    }

    private java.util.List<Filter> getSelectedFilters() {
        java.util.List<PackageControllerTableModelFilter<CrawledPackage, CrawledLink>> tableFilters = getLinkgrabberTable().getModel().getTableFilters();
        java.util.List<Filter> ret = new ArrayList<Filter>();
        for (PackageControllerTableModelFilter<CrawledPackage, CrawledLink> f : tableFilters) {
            if (f instanceof FilterTable) {
                ret.addAll(((FilterTable) f).getModel().getSelectedObjects());
            }
        }
        return ret;
    }

    public java.util.List<AbstractNode> getMatches(java.util.List<Filter> selection) {
        List<CrawledLink> all = getVisibleLinks();

        CrawledLink link;
        main: for (Iterator<CrawledLink> it = all.iterator(); it.hasNext();) {
            link = it.next();
            for (Filter f : selection) {
                if (f.isFiltered(link)) {

                    continue main;
                }
            }

            it.remove();
        }

        java.util.List<AbstractNode> newSelection = new ArrayList<AbstractNode>(all);

        return newSelection;
    }

    public java.util.List<CrawledPackage> getPackagesToExpand(java.util.List<Filter> selection) {
        List<CrawledLink> all = getVisibleLinks();
        HashSet<CrawledPackage> packages = new HashSet<CrawledPackage>();
        CrawledLink link;
        main: for (Iterator<CrawledLink> it = all.iterator(); it.hasNext();) {
            link = it.next();
            for (Filter f : selection) {
                if (f.isFiltered(link)) {
                    if (!link.getParentNode().isExpanded()) {
                        //
                        packages.add(link.getParentNode());
                    }

                    continue main;
                }
            }

        }

        return new ArrayList<CrawledPackage>(packages);
    }

    protected JPopupMenu onContextMenu(final JPopupMenu popup, final Filter contextObject, final java.util.List<Filter> selection, final ExtColumn<Filter> column, final MouseEvent mouseEvent) {

        java.util.List<String> ret = new ArrayList<String>();
        for (Filter f : selection) {
            ret.add(f.getName());
        }

        popup.add(new EnabledAllAction(getModel().getSelectedObjects()));
        java.util.List<Filter> nonSel = new ArrayList<Filter>(getModel().getTableData());
        for (Filter f : getModel().getSelectedObjects()) {
            nonSel.remove(f);
        }

        // if
        // (org.jdownloader.settings.statics.LINKGRABBER.QUICK_VIEW_SELECTION_ENABLED.getValue())
        // {
        SelectionInfo<CrawledPackage, CrawledLink> matches = new SelectionInfo<CrawledPackage, CrawledLink>(null, getMatches(getSelectedFilters()), mouseEvent, null, null, null);
        popup.add(new ConfirmSelectionContextAction(matches));
        popup.add(new MergeToPackageAction(matches));
        popup.add(new CreateDLCAction(matches));
        popup.add(new RemoveNonSelectedContextAction(matches));
        popup.add(new RemoveSelectionLinkgrabberAction(matches));
        // popup.add(new
        // RemoveIncompleteArchives(matches));

        // }

        return popup;
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
        java.util.List<PackageControllerTableModelFilter<CrawledPackage, CrawledLink>> tableFilters = getLinkgrabberTable().getModel().getTableFilters();

        if (!e.isControlDown()) {
            for (PackageControllerTableModelFilter<CrawledPackage, CrawledLink> f : tableFilters) {
                if (f instanceof FilterTable && f != this) {
                    ((FilterTable) f).clearSelection();
                }
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
            new EnabledAllAction(getModel().getSelectedObjects()).actionPerformed(null);
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

    protected void updateNow() {
        reset();
        java.util.List<Filter> newData = updateQuickFilerTableData();
        setVisible(newData.size() > 0);
        filters = newData;
        if (visibleKeyHandler.getValue()) {
            getModel()._fireTableStructureChanged(newData, true);
        }
    }

    protected int getCountWithout(Filter filter, java.util.List<CrawledLink> filteredLinks) {
        java.util.List<PackageControllerTableModelFilter<CrawledPackage, CrawledLink>> tableFilters = getLinkgrabberTable().getModel().getTableFilters();
        int ret = 0;
        main: for (CrawledLink l : filteredLinks) {
            if (filter.isFiltered(l)) {
                filterException = filter;
                try {
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

    public LinkGrabberTable getLinkgrabberTable() {
        return linkgrabberTable;
    }

    protected abstract java.util.List<Filter> updateQuickFilerTableData();

    public void setVisible(final boolean aFlag) {

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                header.setEnabled(aFlag);
                FilterTable.super.setVisible(aFlag && visibleKeyHandler.getValue());
            }
        };
    }

    protected List<CrawledLink> getVisibleLinks() {
        return ((PackageControllerTableModel<CrawledPackage, CrawledLink>) linkgrabberTable.getModel()).getAllChildrenNodes();
    }

    protected void updateAllFiltersInstant() {
        final java.util.List<PackageControllerTableModelFilter<CrawledPackage, CrawledLink>> tableFilters = getLinkgrabberTable().getModel().getTableFilters();
        for (PackageControllerTableModelFilter<CrawledPackage, CrawledLink> f : tableFilters) {
            if (f instanceof FilterTable) {
                ((FilterTable) f).requestUpdate();
            }
        }
    }

    // @Override
    // public boolean isFiltered(CrawledLink v) {
    // /*
    // * speed optimization, we dont want to get extension several times
    // */
    // if (enabled == false) return false;
    // String ext = Files.getExtension(v.getName());
    // java.util.List<Filter> lfilters = filters;
    // for (Filter filter : lfilters) {
    // if (filter.isEnabled()) continue;
    // if (((ExtensionFilter) filter).isFiltered(ext)) {
    // filter.setMatchCounter(filter.getMatchCounter() + 1);
    //
    // return true;
    // }
    // }
    // return false;
    // }
    public boolean isFiltered(CrawledLink e) {
        if (enabled == false) return false;
        java.util.List<Filter> lfilters = getAllFilters();
        for (Filter filter : lfilters) {
            if (filter == filterException) continue;
            if (filter.isEnabled()) {
                continue;
            }
            if (filter.isFiltered(e)) {

            return true; }
        }
        return false;
    }

    abstract java.util.List<Filter> getAllFilters();

    public boolean isFiltered(CrawledPackage v) {
        return false;
    }

    public void reset() {
        java.util.List<Filter> lfilters = filters;
        for (Filter filter : lfilters) {
            filter.setCounter(0);
        }
    }

}
