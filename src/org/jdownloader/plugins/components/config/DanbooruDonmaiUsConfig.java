package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "danbooru.donmai.us", type = Type.HOSTER)
public interface DanbooruDonmaiUsConfig extends PluginConfigInterface {
    final String                    text_UserAgent = "Enter User-Agent which will be used for all Google website http requests";
    public static final TRANSLATION TRANSLATION    = new TRANSLATION();

    public static class TRANSLATION {
        public String getUserAgent_label() {
            return text_UserAgent;
        }
    }

    @AboutConfig
    @DefaultStringValue("JDDEFAULT")
    @DescriptionForConfigEntry(text_UserAgent)
    @Order(10)
    String getUserAgent();

    public void setUserAgent(final String userAgent);
}