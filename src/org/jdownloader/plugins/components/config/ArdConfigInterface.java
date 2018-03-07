package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;
import org.jdownloader.translate._JDT;

@PluginHost(host = "ardmediathek.de", type = Type.CRAWLER)
public interface ArdConfigInterface extends PluginConfigInterface {
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

        public String getAddUnknownQualitiesEnabled_label() {
            return _JDT.T.lit_add_unknown_formats();
        }

        public String getGrabHLS270pLowerVideoEnabled_label() {
            return "Grab hls 270p lower (bandwidth = 317000)";
        }

        public String getGrabHTTP270pLowerVideoEnabled_label() {
            return "Grab http 270p lower (bandwidth = 317000)";
        }
    }

    public static final ArdConfigInterface.TRANSLATION TRANSLATION = new TRANSLATION();

    @DefaultBooleanValue(true)
    @Order(9)
    boolean isFastLinkcheckEnabled();

    void setFastLinkcheckEnabled(boolean b);

    @DefaultBooleanValue(false)
    @Order(10)
    boolean isGrabSubtitleEnabled();

    void setGrabSubtitleEnabled(boolean b);

    @DefaultBooleanValue(false)
    @Order(11)
    boolean isGrabAudio();

    void setGrabAudio(boolean b);

    @DefaultBooleanValue(false)
    @Order(20)
    boolean isGrabBESTEnabled();

    void setGrabBESTEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @Order(21)
    boolean isOnlyBestVideoQualityOfSelectedQualitiesEnabled();

    void setOnlyBestVideoQualityOfSelectedQualitiesEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(21)
    boolean isAddUnknownQualitiesEnabled();

    void setAddUnknownQualitiesEnabled(boolean b);

    @DefaultBooleanValue(false)
    @Order(30)
    boolean isGrabHLS180pVideoEnabled();

    void setGrabHLS180pVideoEnabled(boolean b);

    @DefaultBooleanValue(false)
    @Order(40)
    boolean isGrabHLS270pVideoEnabled();

    void setGrabHLS270pVideoEnabled(boolean b);

    @DefaultBooleanValue(false)
    @Order(41)
    boolean isGrabHLS270pLowerVideoEnabled();

    void setGrabHLS270pLowerVideoEnabled(boolean b);

    @DefaultBooleanValue(false)
    @Order(42)
    boolean isGrabHLS280pVideoEnabled();

    void setGrabHLS280pVideoEnabled(boolean b);

    @DefaultBooleanValue(false)
    @Order(50)
    boolean isGrabHLS360pVideoEnabled();

    void setGrabHLS360pVideoEnabled(boolean b);

    @DefaultBooleanValue(false)
    @Order(60)
    boolean isGrabHLS480pVideoEnabled();

    void setGrabHLS480pVideoEnabled(boolean b);

    @DefaultBooleanValue(false)
    @Order(70)
    boolean isGrabHLS540pVideoEnabled();

    void setGrabHLS540pVideoEnabled(boolean b);

    @DefaultBooleanValue(false)
    @Order(71)
    boolean isGrabHLS576pVideoEnabled();

    void setGrabHLS576pVideoEnabled(boolean b);

    @DefaultBooleanValue(false)
    @Order(80)
    boolean isGrabHLS720pVideoEnabled();

    void setGrabHLS720pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(90)
    boolean isGrabHTTP180pVideoEnabled();

    void setGrabHTTP180pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(100)
    boolean isGrabHTTP270pVideoEnabled();

    void setGrabHTTP270pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(101)
    boolean isGrabHTTP270pLowerVideoEnabled();

    void setGrabHTTP270pLowerVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(110)
    boolean isGrabHTTP280pVideoEnabled();

    void setGrabHTTP280pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(120)
    boolean isGrabHTTP360pVideoEnabled();

    void setGrabHTTP360pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(130)
    boolean isGrabHTTP480pVideoEnabled();

    void setGrabHTTP480pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(140)
    boolean isGrabHTTP540pVideoEnabled();

    void setGrabHTTP540pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(141)
    boolean isGrabHTTP576pVideoEnabled();

    void setGrabHTTP576pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(150)
    boolean isGrabHTTP720pVideoEnabled();

    void setGrabHTTP720pVideoEnabled(boolean b);
}