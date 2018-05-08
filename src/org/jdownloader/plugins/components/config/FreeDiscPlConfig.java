package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "freedisc.pl", type = Type.CRAWLER)
public interface FreeDiscPlConfig extends PluginConfigInterface {
    public static class TRANSLATION {
        public String isCrawlSubfolders_label() {
            return "Crawl subfolders?";
        }
    }

    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isCrawlSubfolders();

    void setCrawlSubfolders(boolean b);
}