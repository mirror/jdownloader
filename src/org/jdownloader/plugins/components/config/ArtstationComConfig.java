package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "artstation.com", type = Type.HOSTER)
public interface ArtstationComConfig extends PluginConfigInterface {
    final String                    text_Force4kWorkaroundForImages = "Force replace '/large/' with '/4k' for image URLs?";
    public static final TRANSLATION TRANSLATION                     = new TRANSLATION();

    public static class TRANSLATION {
        public String getForce4kWorkaroundForImages_label() {
            return text_Force4kWorkaroundForImages;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_Force4kWorkaroundForImages)
    @Order(10)
    boolean isForce4kWorkaroundForImages();

    void setForce4kWorkaroundForImages(boolean b);
}