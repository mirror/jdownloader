package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "free-mp3-download.net", type = Type.HOSTER)
public interface FreeM3DownloadNetConfig extends PluginConfigInterface {
    final String                    text_PreferFLAC = "Prefer FLAC instead of MP3?";
    // final String text_PreferredAudioQuality = "Preferred audio download quality";
    public static final TRANSLATION TRANSLATION     = new TRANSLATION();

    public static class TRANSLATION {
        // public String getPreferredAudioQuality_label() {
        // return text_PreferredAudioQuality;
        // }
        public String getPreferFLAC_label() {
            return text_PreferFLAC;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_PreferFLAC)
    @Order(30)
    boolean isPreferFLAC();

    void setPreferFLAC(boolean b);
    // public static enum PreferredAudioQuality implements LabelInterface {
    // FLAC {
    // @Override
    // public String getLabel() {
    // return "FLAC";
    // }
    // },
    // MP3_320 {
    // @Override
    // public String getLabel() {
    // return "MP3 320kbps";
    // }
    // },
    // MP3_128 {
    // @Override
    // public String getLabel() {
    // return "MP3 128kbps";
    // }
    // };
    // }
    //
    // @AboutConfig
    // @DefaultEnumValue("FLAC")
    // @DescriptionForConfigEntry(text_PreferredAudioQuality)
    // @Order(10)
    // PreferredAudioQuality getPreferredAudioQuality();
}