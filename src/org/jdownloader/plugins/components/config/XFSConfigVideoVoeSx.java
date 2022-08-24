package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "voe.sx", type = Type.HOSTER)
public interface XFSConfigVideoVoeSx extends XFSConfigVideo {
    final String text_CrawlSubtitle = "Crawl subtitle?";

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlSubtitle)
    @Order(200)
    boolean isCrawlSubtitle();

    void setCrawlSubtitle(boolean b);
}