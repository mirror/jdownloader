package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "tumblr.com", type = Type.HOSTER)
public interface TumblrComConfig extends PluginConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isUseOriginalFilenameEnabled();

    void setUseOriginalFilenameEnabled(boolean b);
}