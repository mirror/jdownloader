package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "wallpaperscraft.com", type = Type.CRAWLER)
public interface WallPapersCraftComConfig extends PluginConfigInterface {
    public static class TRANSLATION {

        public String isPreferOriginalResolution_label() {
            return "Prefer original resolution?";
        }
    }

    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isPreferOriginalResolution();

    void setPreferOriginalResolution(boolean b);
}