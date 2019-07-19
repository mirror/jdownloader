package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;
import org.jdownloader.translate._JDT;

@PluginHost(host = "brazzers.com", type = Type.HOSTER)
public interface BrazzersConfigInterface extends PluginConfigInterface {
    public static class TRANSLATION {
        public String getFastLinkcheckEnabled_label() {
            return _JDT.T.lit_enable_fast_linkcheck();
        }

        public String getUseServerFilenames_label() {
            return "Use original server filenames? If disabled, plugin-filenames will be used.";
        }

        public String getGrabBESTEnabled_label() {
            return _JDT.T.lit_add_only_the_best_video_quality();
        }

        public String getGrabHTTPMp4_1080pEnabled_label() {
            return "Grab 1080p HD MP4 1080P (mp4)?";
        }

        public String getGrabHTTPMp4_720pHDEnabled_label() {
            return "Grab 720p HD MP4 720P (mp4)?";
        }

        public String getGrabHTTPMp4_480pSDEnabled_label() {
            return "Grab 480p SD MP4 (mp4)?";
        }

        public String getGrabHTTPMp4_480pMPEG4Enabled_label() {
            return "Grab 480p MPEG4 (mp4)?";
        }

        public String getGrabHTTPMp4_270piPHONEMOBILEEnabled_label() {
            return "Grab 270p IPHONE/MOBILE (mp4)?";
        }
    }

    public static final BrazzersConfigInterface.TRANSLATION TRANSLATION = new TRANSLATION();

    @DefaultBooleanValue(true)
    @Order(9)
    boolean isFastLinkcheckEnabled();

    void setFastLinkcheckEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(9)
    boolean isUseServerFilenames();

    void setUseServerFilenames(boolean b);

    @DefaultBooleanValue(false)
    @Order(20)
    boolean isGrabBESTEnabled();

    void setGrabBESTEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(90)
    boolean isGrabHTTPMp4_1080pEnabled();

    void setGrabHTTPMp4_1080pEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(100)
    boolean isGrabHTTPMp4_720pHDEnabled();

    void setGrabHTTPMp4_720pHDEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(110)
    boolean isGrabHTTPMp4_480pSDEnabled();

    void setGrabHTTPMp4_480pSDEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(120)
    boolean isGrabHTTPMp4_480pMPEG4Enabled();

    void setGrabHTTPMp4_480pMPEG4Enabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(125)
    boolean isGrabHTTPMp4_320pMPEG4Enabled();

    void setGrabHTTPMp4_320pMPEG4Enabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(130)
    boolean isGrabHTTPMp4_270piPHONEMOBILEEnabled();

    void setGrabHTTPMp4_270piPHONEMOBILEEnabled(boolean b);
}