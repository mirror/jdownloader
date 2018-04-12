package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;
import org.jdownloader.translate._JDT;

@PluginHost(host = "watchbox.de", type = Type.CRAWLER)
public interface WatchboxDeConfigInterface extends PluginConfigInterface {
    public static class TRANSLATION {
        public String getFastLinkcheckEnabled_label() {
            return _JDT.T.lit_enable_fast_linkcheck();
        }

        public String getGrabSubtitleEnabled_label() {
            return _JDT.T.lit_add_subtitles();
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
    }

    public static final WatchboxDeConfigInterface.TRANSLATION TRANSLATION = new TRANSLATION();

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

    @DefaultBooleanValue(true)
    @Order(21)
    boolean isAddUnknownQualitiesEnabled();

    void setAddUnknownQualitiesEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(29)
    boolean isGrabHLS144pVideoEnabled();

    void setGrabHLS144pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(30)
    boolean isGrabHLS180pVideoEnabled();

    void setGrabHLS180pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(45)
    boolean isGrabHLS360pLowerVideoEnabled();

    void setGrabHLS360pLowerVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(50)
    boolean isGrabHLS360pVideoEnabled();

    void setGrabHLS360pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(60)
    boolean isGrabHLS540pLowerVideoEnabled();

    void setGrabHLS540pLowerVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(70)
    boolean isGrabHLS540pVideoEnabled();

    void setGrabHLS540pVideoEnabled(boolean b);
}