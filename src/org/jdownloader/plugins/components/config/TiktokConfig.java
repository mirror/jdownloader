package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "tiktok.com", type = Type.HOSTER)
public interface TiktokConfig extends PluginConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Activate experimental forced SSL for downloads?")
    @Order(10)
    boolean isEnableFastLinkcheck();

    void setEnableFastLinkcheck(boolean b);

    @AboutConfig
    @DefaultIntValue(1)
    @SpinnerValidator(min = 1, max = 20, step = 1)
    @Order(20)
    @DescriptionForConfigEntry("Set max. simultaneous downloads. Don't set this value too much otherwise you might get blocked.")
    int getMaxSimultaneousDownloads();

    void setMaxSimultaneousDownloads(int i);
}