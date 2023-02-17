package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "fshare.vn", type = Type.HOSTER)
public interface FshareVnConfig extends PluginConfigInterface {
    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    public static class TRANSLATION {
        public String getApiAppKey_label() {
            return "App Key";
        }

        public String getApiUserAgent_label() {
            return "API User-Agent";
        }
    }

    @AboutConfig
    @DescriptionForConfigEntry("App Key")
    @Order(10)
    @DefaultStringValue("JDDEFAULT")
    String getApiAppKey();

    void setApiAppKey(String apiAppKey);

    @AboutConfig
    @DescriptionForConfigEntry("User-Agent used for API requests.")
    @Order(20)
    @DefaultStringValue("JDDEFAULT")
    String getApiUserAgent();

    void setApiUserAgent(String apiUserAgent);
}