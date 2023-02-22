package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "magentamusik.de", type = Type.CRAWLER)
public interface MagentamusikDeConfig extends PluginConfigInterface {
    final String text_CrawlVR = "Crawl VR videos?";

    public static class TRANSLATION {
        public String getCrawlVR_label() {
            return text_CrawlVR;
        }
    }

    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_CrawlVR)
    @Order(10)
    boolean isCrawlVR();

    void setCrawlVR(boolean b);
}