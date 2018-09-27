package jd.controlling.linkcrawler;

import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;

public abstract class LinkCrawlerLock {
    public abstract int maxConcurrency();

    public abstract boolean matches(final LazyCrawlerPlugin plugin, final CrawledLink crawledLink);

    protected String getPluginID(final LazyCrawlerPlugin plugin) {
        return plugin.getDisplayName() + "." + plugin.getLazyPluginClass().getClassName();
    }
}
