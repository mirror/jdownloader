package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;

public interface BangComConfig extends PluginConfigInterface {
    final String                    text_GrabPreviewVideo = "Grab preview video?";
    final String                    text_GrabThumbnail    = "Grab thumbnail?";
    public static final TRANSLATION TRANSLATION           = new TRANSLATION();

    public static class TRANSLATION {
        public String getGrabPreviewVideo_label() {
            return text_GrabPreviewVideo;
        }

        public String getGrabThumbnail_label() {
            return text_GrabThumbnail;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_GrabPreviewVideo)
    @Order(10)
    boolean isGrabPreviewVideo();

    void setGrabPreviewVideo(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_GrabThumbnail)
    @Order(20)
    boolean isGrabThumbnail();

    void setGrabThumbnail(boolean b);
}