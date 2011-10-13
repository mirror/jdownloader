package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JPopupMenu;

import jd.controlling.FavIconController;
import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.controlling.filter.LinkFilterSettings;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.linkgrabber.Header;
import org.jdownloader.gui.views.linkgrabber.sidebar.actions.DropHosterAction;
import org.jdownloader.gui.views.linkgrabber.sidebar.actions.KeepOnlyAction;

public class QuickFilterHosterTable extends FilterTable<CrawledPackage, CrawledLink> implements LinkCollectorListener, GenericConfigEventListener<Boolean> {

    /**
     * 
     */
    private static final long                                          serialVersionUID = 658947589171018284L;
    private LinkedHashMap<String, Filter<CrawledPackage, CrawledLink>> filterMap        = new LinkedHashMap<String, Filter<CrawledPackage, CrawledLink>>();

    private long                                                       old              = -1;
    private DelayedRunnable                                            delayedRefresh;
    private PackageControllerTable<CrawledPackage, CrawledLink>        table2Filter     = null;
    private final Object                                               LOCK             = new Object();
    private Header                                                     header;

    public QuickFilterHosterTable(Header hosterFilter, PackageControllerTable<CrawledPackage, CrawledLink> table2Filter) {
        super();
        header = hosterFilter;
        this.table2Filter = table2Filter;
        delayedRefresh = new DelayedRunnable(IOEQ.TIMINGQUEUE, 100l, 1000l) {

            @Override
            public void delayedrun() {
                updateQuickFilerTableData();
            }

        };
        LinkFilterSettings.LG_QUICKFILTER_HOSTER_VISIBLE.getEventSender().addListener(this);

        LinkCollector.getInstance().addListener(this);
        table2Filter.getPackageControllerTableModel().addFilter(this);
        onConfigValueModified(null, LinkFilterSettings.LG_QUICKFILTER_HOSTER_VISIBLE.getValue());

    }

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, Filter<CrawledPackage, CrawledLink> contextObject, ArrayList<Filter<CrawledPackage, CrawledLink>> selection, ExtColumn<Filter<CrawledPackage, CrawledLink>> column) {
        ArrayList<String> ret = new ArrayList<String>();
        for (Filter<CrawledPackage, CrawledLink> f : selection) {
            ret.add(f.getName());
        }
        popup.add(new DropHosterAction(ret).toContextMenuAction());
        popup.add(new KeepOnlyAction(ret).toContextMenuAction());

        return popup;
    }

    public void onLinkCollectorEvent(LinkCollectorEvent event) {
        switch (event.getType()) {
        case REMOVE_CONTENT:
        case REFRESH_STRUCTURE:
            if (old != LinkCollector.getInstance().getChildrenChanges()) {
                old = LinkCollector.getInstance().getChildrenChanges();
                delayedRefresh.run();
            }
            break;
        }
    }

    private void updateQuickFilerTableData() {
        ArrayList<Filter<CrawledPackage, CrawledLink>> newTableData = null;
        synchronized (LOCK) {
            /* reset existing filter counters */
            Set<Entry<String, Filter<CrawledPackage, CrawledLink>>> es = filterMap.entrySet();
            Iterator<Entry<String, Filter<CrawledPackage, CrawledLink>>> it = es.iterator();
            while (it.hasNext()) {
                it.next().getValue().setCounter(0);
            }
            /* update filter list */
            boolean readL = LinkCollector.getInstance().readLock();
            try {
                for (CrawledPackage pkg : LinkCollector.getInstance().getPackages()) {
                    synchronized (pkg) {
                        for (CrawledLink link : pkg.getChildren()) {
                            String hoster = link.getRealHost();
                            if (hoster != null) {
                                Filter<CrawledPackage, CrawledLink> filter = null;
                                filter = filterMap.get(hoster);
                                if (filter == null) {
                                    /*
                                     * create new filter entry and set its icon
                                     */
                                    filter = new Filter<CrawledPackage, CrawledLink>(hoster, null, false) {

                                        @Override
                                        public boolean isFiltered(CrawledLink link) {
                                            if (name.equals(link.getRealHost())) return true;
                                            return false;
                                        }

                                        @Override
                                        public boolean isFiltered(CrawledPackage link) {
                                            /* we do not filter packages */
                                            return false;
                                        }

                                        @Override
                                        public void setEnabled(boolean enabled) {
                                            super.setEnabled(enabled);
                                            /*
                                             * request recreate the model of
                                             * filtered view
                                             */
                                            table2Filter.getPackageControllerTableModel().recreateModel(false);
                                        }

                                    };
                                    filter.setIcon(FavIconController.getFavIcon(hoster, filter, true));
                                    filterMap.put(hoster, filter);
                                }
                                filter.setCounter(filter.getCounter() + 1);
                            }
                        }
                    }
                }
            } finally {
                LinkCollector.getInstance().readUnlock(readL);
            }
            /* update FilterTableModel */
            ArrayList<Filter<CrawledPackage, CrawledLink>> newfilters = new ArrayList<Filter<CrawledPackage, CrawledLink>>();
            es = filterMap.entrySet();
            it = es.iterator();
            newTableData = new ArrayList<Filter<CrawledPackage, CrawledLink>>(QuickFilterHosterTable.this.getExtTableModel().getTableData().size());
            while (it.hasNext()) {
                Entry<String, Filter<CrawledPackage, CrawledLink>> next = it.next();
                Filter<CrawledPackage, CrawledLink> value = next.getValue();
                if (value.getCounter() > 0) {
                    /* only add entries with counter >0 to visible table */
                    newTableData.add(value);
                }
                newfilters.add(value);
            }
            filters = newfilters;

        }
        header.setFilterCount(filters.size());
        if (LinkFilterSettings.LG_QUICKFILTER_HOSTER_VISIBLE.getValue()) QuickFilterHosterTable.this.getExtTableModel()._fireTableStructureChanged(newTableData, true);
    }

    @Override
    public boolean isFiltered(CrawledPackage e) {
        /* we do not filter packages */
        return false;
    }

    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        if (Boolean.TRUE.equals(newValue)) {
            enabled = true;

            updateQuickFilerTableData();
            setVisible(true);
        } else {
            setVisible(false);
            enabled = false;
            /* filter disabled */
            old = -1;

        }
        table2Filter.getPackageControllerTableModel().recreateModel(false);
    }

}
