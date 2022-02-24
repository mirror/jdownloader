package jd.controlling.linkcrawler;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;

public class LinkCrawlerLock {
    protected final LazyCrawlerPlugin plugin;

    public LinkCrawlerLock(final LazyCrawlerPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean matches(LazyCrawlerPlugin plugin, CrawledLink crawledLink) {
        return isSameLazyCrawlerPlugin(this.plugin, plugin);
    }

    @Override
    public String toString() {
        return plugin + "|" + getMaxConcurrency();
    }

    protected String getMatchingIdentifier() {
        return getMaxConcurrency() + plugin.getDisplayName().concat(plugin.getLazyPluginClass().getClassName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (obj instanceof LinkCrawlerLock) {
            final LinkCrawlerLock other = (LinkCrawlerLock) obj;
            return StringUtils.equals(getMatchingIdentifier(), other.getMatchingIdentifier());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return plugin.getLazyPluginClass().getClassName().hashCode();
    }

    public int getMaxConcurrency() {
        return Math.max(1, plugin.getMaxConcurrentInstances());
    }

    public boolean isSameLazyCrawlerPlugin(final LazyCrawlerPlugin x, final LazyCrawlerPlugin y) {
        return x != null && y != null && StringUtils.equals(x.getDisplayName(), y.getDisplayName()) && StringUtils.equals(x.getLazyPluginClass().getClassName(), y.getLazyPluginClass().getClassName());
    }

    public static boolean requiresLocking(final LazyCrawlerPlugin plugin) {
        final int maxConcurrency = plugin.getMaxConcurrentInstances();
        return maxConcurrency >= 1 && maxConcurrency < LinkCrawler.getMaxThreads();
    }

    public boolean requiresLocking() {
        return getMaxConcurrency() < LinkCrawler.getMaxThreads();
    }
}
