package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "slexy.org", type = Type.CRAWLER)
public interface SlexyOrgPluginConfig extends PluginConfigInterface {
    public static enum MODE {
        CRAWL,
        DOWNLOAD,
        BOTH
    }

    @AboutConfig
    @DefaultEnumValue("CRAWL")
    MODE getMode();

    void setMode(MODE mode);
}