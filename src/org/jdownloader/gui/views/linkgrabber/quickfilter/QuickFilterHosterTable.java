package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JPopupMenu;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import jd.controlling.FavIconController;
import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.filter.LinkFilterSettings;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModel;
import org.jdownloader.gui.views.linkgrabber.Header;
import org.jdownloader.gui.views.linkgrabber.sidebar.actions.DropHosterAction;
import org.jdownloader.gui.views.linkgrabber.sidebar.actions.KeepOnlyAction;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class QuickFilterHosterTable extends FilterTable implements GenericConfigEventListener<Boolean> {

    /**
     * 
     */
    private static final long             serialVersionUID = 658947589171018284L;
    private LinkedHashMap<String, Filter> filterMap        = new LinkedHashMap<String, Filter>();

    private long                          old              = -1;
    private DelayedRunnable               delayedRefresh;
    private PackageControllerTable        table2Filter     = null;
    private final Object                  LOCK             = new Object();
    private Header                        header;
    private TableModelListener            listener;

    public QuickFilterHosterTable(Header hosterFilter, PackageControllerTable table) {
        super();
        header = hosterFilter;
        header.setFilterCount(0);
        this.table2Filter = table;
        delayedRefresh = new DelayedRunnable(IOEQ.TIMINGQUEUE, REFRESH_MIN, REFRESH_MAX) {

            @Override
            public void delayedrun() {
                updateQuickFilerTableData();
            }

        };

        listener = new TableModelListener() {

            public void tableChanged(TableModelEvent e) {
                delayedRefresh.run();
            }
        };

        LinkFilterSettings.LG_QUICKFILTER_HOSTER_VISIBLE.getEventSender().addListener(this);

        onConfigValueModified(null, LinkFilterSettings.LG_QUICKFILTER_HOSTER_VISIBLE.getValue());
    }

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, Filter contextObject, ArrayList<Filter> selection, ExtColumn<Filter> column) {
        ArrayList<String> ret = new ArrayList<String>();
        for (Filter f : selection) {
            ret.add(f.getName());
        }
        popup.add(new DropHosterAction(ret).toContextMenuAction());
        popup.add(new KeepOnlyAction(ret).toContextMenuAction());

        return popup;
    }

    @SuppressWarnings("unchecked")
    private void updateQuickFilerTableData() {

        // synchronized (LOCK) {
        /* reset existing filter counters */
        Set<Entry<String, Filter>> es = filterMap.entrySet();
        Iterator<Entry<String, Filter>> it = es.iterator();
        Entry<String, Filter> next;
        while (it.hasNext()) {
            next = it.next();
            next.getValue().setCounter(0);

        }
        /* update filter list */
        HashSet<CrawledLink> map = new HashSet<CrawledLink>();
        for (CrawledLink link : ((PackageControllerTableModel<CrawledPackage, CrawledLink>) table2Filter.getExtTableModel()).getAllChildrenNodes()) {
            final String hoster = link.getRealHost();
            map.add(link);
            if (hoster != null) {
                Filter filter = null;
                filter = filterMap.get(hoster);
                if (filter == null) {
                    /*
                     * create new filter entry and set its icon
                     */
                    filter = createFilter(hoster);
                    filter.setIcon(FavIconController.getFavIcon(hoster, filter, true));
                    filterMap.put(hoster, filter);
                }
                filter.setCounter(filter.getCounter() + 1);
            }

        }
        /* update filter list */
        boolean readL = LinkCollector.getInstance().readLock();
        try {

            for (CrawledPackage pkg : LinkCollector.getInstance().getPackages()) {
                synchronized (pkg) {
                    for (CrawledLink link : pkg.getChildren()) {
                        String hoster = link.getRealHost();
                        if (hoster != null) {
                            Filter filter = null;
                            filter = filterMap.get(hoster);
                            if (filter == null) {
                                /*
                                 * create new filter entry and set its icon
                                 */
                                filter = createFilter(hoster);
                                filter.setIcon(FavIconController.getFavIcon(hoster, filter, true));
                                filterMap.put(hoster, filter);
                            }
                            if (filter.getCounter() == 0 && !filter.isEnabled()) {

                                filter.setCounter(filter.getMatchCounter());
                            }
                        }
                    }
                }
            }
        } finally {
            LinkCollector.getInstance().readUnlock(readL);
        }
        /* update FilterTableModel */
        // ArrayList<Filter> newfilters = new
        // ArrayList<Filter>();
        es = filterMap.entrySet();
        it = es.iterator();
        final ArrayList<Filter> newTableData = new ArrayList<Filter>(QuickFilterHosterTable.this.getExtTableModel().getTableData().size());
        while (it.hasNext()) {
            next = it.next();
            Filter value = next.getValue();
            if (value.getCounter() != 0) {
                /* only add entries with counter >0 to visible table */
                newTableData.add(value);
            }
            // newfilters.add(value);
        }
        filters = newTableData;
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                header.setVisible(filters.size() > 0);
                setVisible(filters.size() > 0 && LinkFilterSettings.LG_QUICKFILTER_HOSTER_VISIBLE.getValue());
            }
        };
        if (LinkFilterSettings.LG_QUICKFILTER_HOSTER_VISIBLE.getValue()) QuickFilterHosterTable.this.getExtTableModel()._fireTableStructureChanged(newTableData, true);

    }

    public void reset() {
        Collection<Filter> lfilters = filterMap.values();
        for (Filter filter : lfilters) {
            filter.setMatchCounter(0);
            filter.setCounter(0);
        }
    }

    public Filter createFilter(final String hoster) {
        Filter filter;
        filter = new Filter(hoster, null) {
            protected String getID() {
                return "Hoster_" + hoster;
            }

            @Override
            public boolean isFiltered(CrawledLink link) {
                if (name.equals(link.getRealHost())) return true;
                return false;
            }

            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                /*
                 * request recreate the model of filtered view
                 */
                table2Filter.getPackageControllerTableModel().recreateModel(false);
            }

        };
        return filter;
    }

    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        if (Boolean.TRUE.equals(newValue) && GraphicalUserInterfaceSettings.CFG.isLinkgrabberSidebarEnabled()) {
            enabled = true;
            table2Filter.getPackageControllerTableModel().addFilter(this);

            this.table2Filter.getModel().addTableModelListener(listener);
        } else {
            this.table2Filter.getModel().removeTableModelListener(listener);

            enabled = false;
            /* filter disabled */
            old = -1;
            table2Filter.getPackageControllerTableModel().removeFilter(this);
        }
        updateQuickFilerTableData();
        table2Filter.getPackageControllerTableModel().recreateModel(false);
    }

}
