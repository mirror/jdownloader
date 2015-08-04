package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.controlling.faviconcontroller.FavIcons;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Browser;

import org.jdownloader.DomainInfo;
import org.jdownloader.gui.views.components.Header;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;

public class QuickFilterHosterTable extends FilterTable {

    /**
     * 
     */
    private static final long       serialVersionUID = 658947589171018284L;
    private HashMap<String, Filter> filterMapping    = new HashMap<String, Filter>();
    private HashMap<String, Filter> enabledFilters   = new HashMap<String, Filter>();

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
                return new ArrayList<Filter>(usedFilters);
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

    private void setEnabled(boolean enabled, Filter filter, String ID) {
        synchronized (enabledFilters) {
            if (!enabled) {
                enabledFilters.put(ID, filter);
            } else {
                enabledFilters.remove(ID);
            }
        }
        getLinkgrabberTable().getModel().recreateModel(false);
    }

    private String getID(CrawledLink link) {
        final DomainInfo info = link.getDomainInfo();
        final String linkID;
        final String linkHOST;
        if (link.isDirectHTTP()) {
            linkHOST = Browser.getHost(link.getURL());
            linkID = "http_" + linkHOST;
        } else if (link.isFTP()) {
            linkHOST = Browser.getHost(link.getURL());
            linkID = "ftp_" + linkHOST;
        } else {
            linkHOST = info.getTld();
            linkID = linkHOST;
        }
        return linkID;
    }

    private Filter getFilter(CrawledLink link, AtomicBoolean newDisabledFilters) {
        final DomainInfo info = link.getDomainInfo();
        final String ID;
        final String HOST;
        if (link.isDirectHTTP()) {
            HOST = Browser.getHost(link.getURL());
            ID = "http_" + HOST;
        } else if (link.isFTP()) {
            HOST = Browser.getHost(link.getURL());
            ID = "ftp_" + HOST;
        } else {
            HOST = info.getTld();
            ID = HOST;
        }
        Filter ret = filterMapping.get(ID);
        if (ret == null) {
            ret = new Filter(ID, null) {
                protected String getID() {
                    return "Hoster_" + ID;
                }

                @Override
                public boolean isFiltered(CrawledLink link) {
                    return ID.equals(QuickFilterHosterTable.this.getID(link));
                }

                @Override
                public void setEnabled(boolean enabled) {
                    super.setEnabled(enabled);
                    QuickFilterHosterTable.this.setEnabled(enabled, this, ID);
                }

            };
            ret.setIcon(FavIcons.getFavIcon(HOST, ret));
            filterMapping.put(ID, ret);
            if (!ret.isEnabled()) {
                newDisabledFilters.set(true);
                synchronized (enabledFilters) {
                    enabledFilters.put(ID, ret);
                }
            }
        }
        return ret;
    }

    @Override
    public boolean isFiltered(CrawledLink e) {
        Filter ret = null;
        synchronized (enabledFilters) {
            ret = enabledFilters.get(getID(e));
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
