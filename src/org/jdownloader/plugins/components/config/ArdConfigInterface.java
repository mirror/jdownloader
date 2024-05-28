package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;
import org.jdownloader.translate._JDT;

@PluginHost(host = "ardmediathek.de", type = Type.CRAWLER)
public interface ArdConfigInterface extends PluginConfigInterface {
    final String text_PreferredSubtitleType = "Preferred subtitle type";

    public static class TRANSLATION {
        public String getFastLinkcheckEnabled_label() {
            return _JDT.T.lit_enable_fast_linkcheck();
        }

        public String getGrabSubtitleEnabled_label() {
            return _JDT.T.lit_add_subtitles();
        }

        public String getPreferredSubtitleType_label() {
            return text_PreferredSubtitleType;
        }

        public String getPreferAudioDescription_label() {
            return "Prefer audiodescription";
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

    public static final ArdConfigInterface.TRANSLATION TRANSLATION = new TRANSLATION();

    @DefaultBooleanValue(true)
    @Order(9)
    boolean isFastLinkcheckEnabled();

    void setFastLinkcheckEnabled(boolean b);

    @DefaultBooleanValue(false)
    @Order(10)
    boolean isGrabSubtitleEnabled();

    void setGrabSubtitleEnabled(boolean b);

    public static enum SubtitleType implements LabelInterface {
        WEBVTT {
            @Override
            public String getLabel() {
                return "WEBVTT (.vtt)";
            }
        },
        XML {
            @Override
            public String getLabel() {
                return "EBU-TT XML (.xml)";
            }
        },
        SRT {
            @Override
            public String getLabel() {
                return "SRT [EBU-TT XML converted to srt (.srt)]";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("WEBVTT")
    @Order(11)
    SubtitleType getPreferredSubtitleType();

    void setPreferredSubtitleType(final SubtitleType type);

    @DefaultBooleanValue(false)
    @Order(12)
    boolean isPreferAudioDescription();

    void setPreferAudioDescription(boolean b);

    @DefaultBooleanValue(false)
    @Order(20)
    boolean isGrabBESTEnabled();

    void setGrabBESTEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @Order(21)
    boolean isOnlyBestVideoQualityOfSelectedQualitiesEnabled();

    void setOnlyBestVideoQualityOfSelectedQualitiesEnabled(boolean b);

    /* 2022-01-20: Grabbing unknown qualities is not supported anymore for now. */
    // @DefaultBooleanValue(true)
    // @Order(22)
    // boolean isAddUnknownQualitiesEnabled();
    //
    // void setAddUnknownQualitiesEnabled(boolean b);
    @DefaultBooleanValue(true)
    @Order(25)
    boolean isGrabHLS144pVideoEnabled();

    void setGrabHLS144pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(30)
    boolean isGrabHLS180pVideoEnabled();

    void setGrabHLS180pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(40)
    boolean isGrabHLS270pVideoEnabled();

    void setGrabHLS270pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(42)
    boolean isGrabHLS280pVideoEnabled();

    void setGrabHLS280pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(43)
    boolean isGrabHLS288pVideoEnabled();

    void setGrabHLS288pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(50)
    boolean isGrabHLS360pVideoEnabled();

    void setGrabHLS360pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(60)
    boolean isGrabHLS480pVideoEnabled();

    void setGrabHLS480pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(70)
    boolean isGrabHLS540pVideoEnabled();

    void setGrabHLS540pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(80)
    boolean isGrabHLS720pVideoEnabled();

    void setGrabHLS720pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(81)
    boolean isGrabHLS1080pVideoEnabled();

    void setGrabHLS1080pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(85)
    boolean isGrabHTTP144pVideoEnabled();

    void setGrabHTTP144pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(90)
    boolean isGrabHTTP180pVideoEnabled();

    void setGrabHTTP180pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(100)
    boolean isGrabHTTP270pVideoEnabled();

    void setGrabHTTP270pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(110)
    boolean isGrabHTTP280pVideoEnabled();

    void setGrabHTTP280pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(115)
    boolean isGrabHTTP288pVideoEnabled();

    void setGrabHTTP288pVideoEnabled(boolean b);

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
    @Order(150)
    boolean isGrabHTTP720pVideoEnabled();

    void setGrabHTTP720pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(151)
    boolean isGrabHTTP1080pVideoEnabled();

    void setGrabHTTP1080pVideoEnabled(boolean b);
}