package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import jd.controlling.faviconcontroller.FavIcons;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.gui.views.components.Header;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;

public class QuickFilterHosterTable extends FilterTable {

    /**
     * 
     */
    private static final long             serialVersionUID = 658947589171018284L;
    private LinkedHashMap<String, Filter> filterMap        = new LinkedHashMap<String, Filter>();

    public QuickFilterHosterTable(Header hosterFilter, LinkGrabberTable table) {
        super(hosterFilter, table, org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_HOSTER_QUICKFILTER_ENABLED);

    }

    @SuppressWarnings("unchecked")
    protected java.util.List<Filter> updateQuickFilerTableData() {

        HashSet<Filter> filtersInUse = new HashSet<Filter>();
        HashSet<CrawledLink> map = new HashSet<CrawledLink>();
        /* update filter list */
        List<CrawledLink> links = getVisibleLinks();
        for (CrawledLink link : links) {
            final String hoster = link.getDomainInfo().getTld();
            map.add(link);
            if (hoster != null) {
                Filter filter = null;
                filter = filterMap.get(hoster);
                if (filter == null) {
                    /*
                     * create new filter entry and set its icon
                     */
                    filter = createFilter(hoster);
                    filter.setIcon(FavIcons.getFavIcon(hoster, filter));
                    filterMap.put(hoster, filter);
                }
                filtersInUse.add(filter);
                filter.setCounter(filter.getCounter() + 1);
            }

        }

        java.util.List<CrawledLink> filteredLinks = new ArrayList<CrawledLink>();
        /* update filter list */

        // update all filters
        for (CrawledPackage pkg : LinkCollector.getInstance().getPackagesCopy()) {
            boolean readL = pkg.getModifyLock().readLock();
            try {
                for (CrawledLink link : pkg.getChildren()) {
                    if (map.add(link)) {
                        filteredLinks.add(link);
                    }
                    String hoster = link.getDomainInfo().getTld();
                    if (hoster != null) {
                        Filter filter = null;
                        filter = filterMap.get(hoster);
                        if (filter == null) {
                            /*
                             * create new filter entry and set its icon
                             */
                            filter = createFilter(hoster);
                            filter.setIcon(FavIcons.getFavIcon(hoster, filter));
                            filterMap.put(hoster, filter);
                        }
                        filtersInUse.add(filter);
                    }
                }
            } finally {
                pkg.getModifyLock().readUnlock(readL);
            }
        }

        for (Filter filter : filtersInUse) {
            if (filter.getCounter() == 0) {
                filter.setCounter(getCountWithout(filter, filteredLinks));
            }
        }
        /* update FilterTableModel */
        // java.util.List<Filter> newfilters = new
        // java.util.List<Filter>();

        return new ArrayList<Filter>(filtersInUse);
    }

    public void reset() {
        Collection<Filter> lfilters = filterMap.values();
        for (Filter filter : lfilters) {

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
                if (name.equals(link.getDomainInfo().getTld())) return true;
                return false;
            }

            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                /*
                 * request recreate the model of filtered view
                 */
                getLinkgrabberTable().getModel().recreateModel(false);
                updateAllFiltersInstant();
            }

        };
        return filter;
    }

    @Override
    java.util.List<Filter> getAllFilters() {
        return new ArrayList<Filter>(filterMap.values());
    }

    public boolean highlightFilter() {
        return false;
    }

}
