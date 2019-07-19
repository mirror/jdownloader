package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;
import org.jdownloader.translate._JDT;

@PluginHost(host = "br-online.de", type = Type.HOSTER)
public interface BrDeConfigInterface extends PluginConfigInterface {
    public static class TRANSLATION {
        public String getFastLinkcheckEnabled_label() {
            return _JDT.T.lit_enable_fast_linkcheck();
        }

        public String getGrabSubtitleEnabled_label() {
            return _JDT.T.lit_add_subtitles();
        }

        public String getGrabAudio_label() {
            return _JDT.T.lit_add_audio();
        }

        public String getGrabBESTEnabled_label() {
            return _JDT.T.lit_add_only_the_best_video_quality();
        }

        public String getOnlyBestVideoQualityOfSelectedQualitiesEnabled_label() {
            return _JDT.T.lit_add_only_the_best_video_quality_within_user_selected_formats();
        }
        // public String getAddUnknownQualitiesEnabled_label() {
        // return _JDT.T.lit_add_unknown_formats();
        // }
    }

    public static final BrDeConfigInterface.TRANSLATION TRANSLATION = new TRANSLATION();

    @DefaultBooleanValue(true)
    @Order(9)
    boolean isFastLinkcheckEnabled();

    void setFastLinkcheckEnabled(boolean b);

    @DefaultBooleanValue(false)
    @Order(10)
    boolean isGrabSubtitleEnabled();

    void setGrabSubtitleEnabled(boolean b);

    @DefaultBooleanValue(false)
    @Order(20)
    boolean isGrabBESTEnabled();

    void setGrabBESTEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @Order(21)
    boolean isOnlyBestVideoQualityOfSelectedQualitiesEnabled();

    void setOnlyBestVideoQualityOfSelectedQualitiesEnabled(boolean b);

    // @DefaultBooleanValue(true)
    // @Order(21)
    // boolean isAddUnknownQualitiesEnabled();
    //
    // void setAddUnknownQualitiesEnabled(boolean b);
    @DefaultBooleanValue(true)
    @Order(90)
    boolean isGrabHTTPMp4XSVideoEnabled();

    void setGrabHTTPMp4XSVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(100)
    boolean isGrabHTTPMp4SVideoEnabled();

    void setGrabHTTPMp4SVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(110)
    boolean isGrabHTTPMp4MVideoEnabled();

    void setGrabHTTPMp4MVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(111)
    boolean isGrabHTTPMp4LVideoEnabled();

    void setGrabHTTPMp4LVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(112)
    boolean isGrabHTTPMp4XLVideoEnabled();

    void setGrabHTTPMp4XLVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(113)
    boolean isGrabHTTPMp4XXLVideoEnabled();

    void setGrabHTTPMp4XXLVideoEnabled(boolean b);
}