package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.controlling.faviconcontroller.FavIcons;
import jd.controlling.linkcrawler.CrawledLink;

import org.jdownloader.DomainInfo;
import org.jdownloader.gui.views.components.Header;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;

public class QuickFilterHosterTable extends FilterTable {

    /**
     * 
     */
    private static final long           serialVersionUID = 658947589171018284L;
    private HashMap<DomainInfo, Filter> filterMapping    = new HashMap<DomainInfo, Filter>();
    private HashMap<DomainInfo, Filter> enabledFilters   = new HashMap<DomainInfo, Filter>();

    public QuickFilterHosterTable(Header hosterFilter, LinkGrabberTable table) {
        super(hosterFilter, table, org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_HOSTER_QUICKFILTER_ENABLED);

    }

    @Override
    protected FilterTableDataUpdater getFilterTableDataUpdater() {
        return new FilterTableDataUpdater() {
            Set<Filter>   usedFilters        = new HashSet<Filter>();
            AtomicBoolean newDisabledFilters = new AtomicBoolean(false);

            @Override
            public void updateVisible(CrawledLink link) {
                Filter filter = getFilter(link, newDisabledFilters);
                usedFilters.add(filter);
                filter.increaseCounter();
            }

            @Override
            public void updateFiltered(CrawledLink link) {
                usedFilters.add(getFilter(link, newDisabledFilters));
            }

            @Override
            public void reset() {
                for (Filter filter : filterMapping.values()) {
                    filter.resetCounter();
                }
            }

            @Override
            public FilterTable getFilterTable() {
                return QuickFilterHosterTable.this;
            }

            @Override
            public List<Filter> finalizeUpdater() {
                ArrayList<Filter> ret = new ArrayList<Filter>(usedFilters);
                Collections.sort(ret, new Comparator<Filter>() {

                    public int compare(boolean x, boolean y) {
                        return (x == y) ? 0 : (x ? -1 : 1);
                    }

                    @Override
                    public int compare(Filter o1, Filter o2) {
                        if (o1.isEnabled() == o2.isEnabled()) {
                            return o1.getName().compareToIgnoreCase(o2.getName());
                        } else {
                            return compare(o1.isEnabled(), o2.isEnabled());
                        }
                    }

                });
                return ret;
            }

            @Override
            public void afterVisible() {
            }

            @Override
            public boolean hasNewDisabledFilters() {
                return newDisabledFilters.get();
            }
        };
    }

    private void setEnabled(boolean enabled, Filter filter, DomainInfo info) {
        synchronized (enabledFilters) {
            if (!enabled) {
                enabledFilters.put(info, filter);
            } else {
                enabledFilters.remove(info);
            }
        }
        getLinkgrabberTable().getModel().recreateModel(false);
    }

    private Filter getFilter(CrawledLink link, AtomicBoolean newDisabledFilters) {
        final DomainInfo info = link.getDomainInfo();
        Filter ret = filterMapping.get(info);
        if (ret != null) return ret;
        Filter filter = new Filter(info.getTld(), null) {
            protected String getID() {
                return "Hoster_" + getName();
            }

            @Override
            public boolean isFiltered(CrawledLink link) {
                return info == link.getDomainInfo();
            }

            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                QuickFilterHosterTable.this.setEnabled(enabled, this, info);
            }

        };
        filter.setIcon(FavIcons.getFavIcon(info.getTld(), filter));
        filterMapping.put(info, filter);
        if (!filter.isEnabled()) {
            newDisabledFilters.set(true);
            synchronized (enabledFilters) {
                enabledFilters.put(info, filter);
            }
        }
        return filter;
    }

    @Override
    public boolean isFiltered(CrawledLink e) {
        Filter ret = null;
        synchronized (enabledFilters) {
            ret = enabledFilters.get(e.getDomainInfo());
        }
        return ret != null && !ret.isEnabled() && ret != getFilterException();
    }

    @Override
    public boolean isFilteringChildrenNodes() {
        synchronized (enabledFilters) {
            return isEnabled() && enabledFilters.size() > 0;
        }
    }

    @Override
    public int getComplexity() {
        return 0;
    }

}
