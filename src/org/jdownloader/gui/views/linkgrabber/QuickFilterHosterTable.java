package org.jdownloader.gui.views.linkgrabber;

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

public class QuickFilterHosterTable extends FilterTable implements LinkCollectorListener {

    /**
     * 
     */
    private static final long             serialVersionUID = 658947589171018284L;
    private LinkedHashMap<String, Filter> filterMap        = new LinkedHashMap<String, Filter>();
    private long                          old              = -1;
    private DelayedRunnable               delayedRefresh;

    public QuickFilterHosterTable() {
        super();
        LinkCollector.getInstance().addListener(this);
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
            if (old != LinkCollector.getInstance().getChildrenChanges()) {
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
        Set<Entry<String, Filter>> es = filterMap.entrySet();
        Iterator<Entry<String, Filter>> it = es.iterator();
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
                            Filter filter = null;
                            filter = filterMap.get(hoster);
                            if (filter == null) {
                                /*
                                 * create new filter entry and set its icon
                                 */
                                filter = new Filter(hoster, null, true);
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
        es = filterMap.entrySet();
        it = es.iterator();
        ArrayList<Filter> newTableData = new ArrayList<Filter>(QuickFilterHosterTable.this.getExtTableModel().getTableData().size());
        while (it.hasNext()) {
            Entry<String, Filter> next = it.next();
            Filter value = next.getValue();
            if (value.getCounter() > 0) newTableData.add(value);
        }
        QuickFilterHosterTable.this.getExtTableModel()._fireTableStructureChanged(newTableData, false);
    }
}
