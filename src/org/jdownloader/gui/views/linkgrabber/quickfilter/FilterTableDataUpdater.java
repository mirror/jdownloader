package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.util.List;

import jd.controlling.linkcrawler.CrawledLink;

public interface FilterTableDataUpdater {

    public void reset();

    public void updateVisible(CrawledLink link);

    public void afterVisible();

    public void updateFiltered(CrawledLink link);

    public List<Filter> finalizeUpdater();

    public boolean hasNewDisabledFilters();

    public FilterTable getFilterTable();
}
