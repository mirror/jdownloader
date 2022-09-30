package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;

public interface XFSConfig extends PluginConfigInterface {
    final String                    text_PreferHTTP = "Prefer http instead of https (not recommended, use only as workaround)?";
    final String                    text_Apikey     = "Enter your API key which will be used for linkchecking in case there is no apikey available in any account of this host";
    public static final TRANSLATION TRANSLATION     = new TRANSLATION();

    public static class TRANSLATION {
        public String getPreferHTTP_label() {
            return text_PreferHTTP;
        }

        public String getApikey_label() {
            return text_Apikey;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_PreferHTTP)
    @Order(30)
    boolean isPreferHTTP();

    void setPreferHTTP(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry(text_Apikey)
    @Order(31)
    String getApikey();

    void setApikey(String apiKey);
}