package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "playout.3qsdn.com", type = Type.CRAWLER)
public interface ThreeQVideoConfig extends PluginConfigInterface {
    public static final ThreeQVideoConfig.TRANSLATION TRANSLATION = new TRANSLATION();

    public static class TRANSLATION {
        public String getOnlyGrabBestQuality_label() {
            return "Only grab best quality?";
        }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @Order(10)
    boolean isOnlyGrabBestQuality();

    void setOnlyGrabBestQuality(boolean b);
}