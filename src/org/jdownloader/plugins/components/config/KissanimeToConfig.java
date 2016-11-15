package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "kissanime.to", type = Type.CRAWLER)
public interface KissanimeToConfig extends PluginConfigInterface {
    public static class TRANSLATION {

        public String selectQuality() {
            return "Select qualities to download:";
        }
    }

    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    @AboutConfig
    @DescriptionForConfigEntry("Select the qualities you like: 1080p,720p,480p,360p OR BEST")
    @DefaultStringValue("1080p,720p,480p,360p")
    String getQualities();

    void setQualities(final String s);
}