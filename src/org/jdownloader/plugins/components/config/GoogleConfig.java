package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "docs.google.com", type = Type.HOSTER)
public interface GoogleConfig extends PluginConfigInterface {
    @AboutConfig
    @DefaultStringValue("JDDEFAULT")
    @DescriptionForConfigEntry("Enter the User-Agent which should be used for all Google related http requests")
    @Order(10)
    String getUserAgent();

    void setUserAgent(String userAgent);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Enable experimental features")
    @Order(20)
    boolean isEnableExperimentalFeatures();

    void setEnableExperimentalFeatures(boolean b);
}