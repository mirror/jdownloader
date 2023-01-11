package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "avxhm.se", type = Type.HOSTER)
public interface AvxHmeWConfig extends PluginConfigInterface {
    final String                    text_DirectLinkCrawlerWaitSecondsBetweenLinks = "Wait time seconds between crawling single 'direct' links";
    public static final TRANSLATION TRANSLATION                                   = new TRANSLATION();

    public static class TRANSLATION {
        public String getDirectLinkCrawlerWaitSecondsBetweenLinks_label() {
            return text_DirectLinkCrawlerWaitSecondsBetweenLinks;
        }
    }

    @AboutConfig
    @SpinnerValidator(min = 1, max = 30, step = 1)
    @DefaultIntValue(10)
    @DescriptionForConfigEntry(text_DirectLinkCrawlerWaitSecondsBetweenLinks)
    @Order(10)
    int getDirectLinkCrawlerWaitSecondsBetweenLinks();

    void setDirectLinkCrawlerWaitSecondsBetweenLinks(int i);
}