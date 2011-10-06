package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import jd.controlling.FavIconController;
import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;

public class QuickFilterHosterTable extends FilterTable<CrawledPackage, CrawledLink> implements LinkCollectorListener {

    /**
     * 
     */
    private static final long                                          serialVersionUID = 658947589171018284L;
    private LinkedHashMap<String, Filter<CrawledPackage, CrawledLink>> filterMap        = new LinkedHashMap<String, Filter<CrawledPackage, CrawledLink>>();

    private long                                                       old              = -1;
    private DelayedRunnable                                            delayedRefresh;
    private PackageControllerTable<CrawledPackage, CrawledLink>        table2Filter     = null;

    public QuickFilterHosterTable(PackageControllerTable<CrawledPackage, CrawledLink> table2Filter) {
        super();
        this.table2Filter = table2Filter;
        LinkCollector.getInstance().addListener(this);
        table2Filter.getPackageControllerTableModel().addFilter(this);
        delayedRefresh = new DelayedRunnable(IOEQ.TIMINGQUEUE, 100l, 1000l) {

            @Override
            public void delayedrun() {
                updateQuickFilerTableData();
            }

        };
    }

    public void onLinkCollectorEvent(LinkCollectorEvent event) {
        switch (event.getType()) {
        case REMOVE_CONTENT:
        case REFRESH_STRUCTURE:
            if (filtersEnabledGlobally && old != LinkCollector.getInstance().getChildrenChanges()) {
                old = LinkCollector.getInstance().getChildrenChanges();
                IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                    @Override
                    protected Void run() throws RuntimeException {
                        delayedRefresh.run();
                        return null;
                    }
                });
            }
            break;
        }
    }

    private void updateQuickFilerTableData() {
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
                                        table2Filter.getPackageControllerTableModel().recreateModel();
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
        ArrayList<Filter<CrawledPackage, CrawledLink>> newTableData = new ArrayList<Filter<CrawledPackage, CrawledLink>>(QuickFilterHosterTable.this.getExtTableModel().getTableData().size());
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
        QuickFilterHosterTable.this.getExtTableModel()._fireTableStructureChanged(newTableData, true);
    }

    @Override
    public boolean isFiltered(CrawledPackage e) {
        /* we do not filter packages */
        return false;
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
        if (aFlag) {
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    delayedRefresh.run();
                    return null;
                }
            });
        }
    }

}
