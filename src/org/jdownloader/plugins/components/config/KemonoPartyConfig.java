package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "kemono.party", type = Type.CRAWLER)
public interface KemonoPartyConfig extends PluginConfigInterface {
    final String                    text_CrawlHttpLinks = "Crawl http links in post text?";
    public static final TRANSLATION TRANSLATION         = new TRANSLATION();

    public static class TRANSLATION {
        public String getCrawlHttpLinks_label() {
            return text_CrawlHttpLinks;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_CrawlHttpLinks)
    @Order(10)
    boolean isCrawlHttpLinks();

    void setCrawlHttpLinks(boolean b);
}