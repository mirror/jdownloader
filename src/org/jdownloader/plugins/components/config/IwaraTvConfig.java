package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "iwara.tv", type = Type.HOSTER)
public interface IwaraTvConfig extends PluginConfigInterface {
    public static class TRANSLATION {
        public String getProfileCrawlerEnableFastLinkcheck_label() {
            return "Enable fast linkcheck for videos found via profile crawler?";
        }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Enable fast linkcheck for videos found via profile crawler?")
    @Order(10)
    boolean isProfileCrawlerEnableFastLinkcheck();

    void setProfileCrawlerEnableFastLinkcheck(boolean b);
}