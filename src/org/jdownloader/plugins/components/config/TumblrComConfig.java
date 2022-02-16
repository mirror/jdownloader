package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "tumblr.com", type = Type.HOSTER)
public interface TumblrComConfig extends PluginConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(true)
    @Order(10)
    @DescriptionForConfigEntry("Use original filename?")
    boolean isUseOriginalFilenameEnabled();

    void setUseOriginalFilenameEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @Order(20)
    @DescriptionForConfigEntry("Download mp4 files instead of gifv images?")
    boolean isPreferMp4OverGifv();

    void setPreferMp4OverGifv(boolean b);
}