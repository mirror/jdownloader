package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;

public interface XFSConfig extends PluginConfigInterface {
    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    public static class TRANSLATION {
        public String getPreferHTTP_label() {
            return "Prefer http protocol instead of https (not recommended, use only as workaround)?";
        }

        public String getApikey_label() {
            return "API key";
        }

        public String getCustomReferer_label() {
            return "Referer";
        }
    }

    @AboutConfig
    @DefaultBooleanValue(false)
    // @DescriptionForConfigEntry("Prefer http protocol instead of https (not recommended, use only as workaround)?")
    @Order(30)
    boolean isPreferHTTP();

    void setPreferHTTP(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("API key which will be used for linkchecking in case there is no apikey available in any account of this host")
    @Order(31)
    String getApikey();

    void setApikey(String apiKey);

    @AboutConfig
    @Order(32)
    @DescriptionForConfigEntry("Custom referer value to be used.")
    String getCustomReferer();

    void setCustomReferer(String referer);
}