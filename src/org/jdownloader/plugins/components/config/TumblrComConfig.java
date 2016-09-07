package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "tumblr.com", type = Type.CRAWLER)
public interface TumblrComConfig extends PluginConfigInterface {

    public static class Translation {
        public String isUseOriginalFilenameEnabled_label() {
            return "test";
        }

    }

    public static final Translation TRANSLATION = new Translation();

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isUseOriginalFilenameEnabled();

    void setUseOriginalFilenameEnabled(boolean b);
}