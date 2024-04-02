package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.TakeValueFromSubconfig;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "naughtyamerica.com", type = Type.HOSTER)
public interface NaughtyamericaConfig extends PluginConfigInterface {
    public static final NaughtyamericaConfig.TRANSLATION TRANSLATION = new TRANSLATION();

    public static class TRANSLATION {
        public String getVideoImageGalleryCrawlMode_label() {
            return "Select video image gallery crawl mode:";
        }

        public String getGrabBestVideoQualityOnly_label() {
            return "Grab best video quality only?";
        }

        public String getGrab480p_label() {
            return "Grab 480p?";
        }

        public String getGrab720p_label() {
            return "Grab 720p?";
        }

        public String getGrab1080p_label() {
            return "Grab 1080p?";
        }

        public String getGrab1440p_label() {
            return "Grab 1440p?";
        }

        public String getGrab4Kp_label() {
            return "Grab 4k?";
        }

        public String getGrab6Kp_label() {
            return "Grab 6k?";
        }

        public String getGrab8Kp_label() {
            return "Grab 8k?";
        }
    }

    public static enum VideoImageGalleryCrawlMode implements LabelInterface {
        NONE {
            @Override
            public String getLabel() {
                return "Don't download galleries";
            }
        },
        AS_ZIP {
            @Override
            public String getLabel() {
                return "Download as single .zip archive";
            }
        },
        AS_SINGLE_IMAGES {
            @Override
            public String getLabel() {
                return "Download single images";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("NONE")
    @DescriptionForConfigEntry("Select video image gallery crawl mode:")
    @Order(10)
    VideoImageGalleryCrawlMode getVideoImageGalleryCrawlMode();

    void setVideoImageGalleryCrawlMode(final VideoImageGalleryCrawlMode quality);

    @AboutConfig
    @DescriptionForConfigEntry("Grab best video quality only?")
    @DefaultBooleanValue(false)
    @Order(20)
    boolean isGrabBestVideoQualityOnly();

    void setGrabBestVideoQualityOnly(boolean b);
    // @AboutConfig
    // @DescriptionForConfigEntry("Grab unknown qualities?")
    // @DefaultBooleanValue(true)
    // @Order(21)
    // boolean isAddUnknownQualitiesEnabled();
    //
    // void setAddUnknownQualitiesEnabled(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Grab 480p?")
    @DefaultBooleanValue(true)
    @TakeValueFromSubconfig("GRAB_HD 480p")
    @Order(90)
    boolean isGrab480p();

    void setGrab480p(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Grab 720p?")
    @DefaultBooleanValue(true)
    @TakeValueFromSubconfig("GRAB_HD 720p")
    @Order(100)
    boolean isGrab720p();

    void setGrab720p(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Grab 1080p?")
    @DefaultBooleanValue(true)
    @TakeValueFromSubconfig("GRAB_HD 1080p")
    @Order(110)
    boolean isGrab1080p();

    void setGrab1080p(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Grab 1440p?")
    @DefaultBooleanValue(true)
    @Order(120)
    boolean isGrab1440p();

    void setGrab1440p(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Grab 4k?")
    @DefaultBooleanValue(true)
    @TakeValueFromSubconfig("GRAB_4K")
    @Order(130)
    boolean isGrab4K();

    void setGrab4K(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Grab 6k?")
    @DefaultBooleanValue(true)
    @Order(140)
    boolean isGrab6K();

    void setGrab6K(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Grab 8k?")
    @DefaultBooleanValue(true)
    @Order(150)
    boolean isGrab8K();

    void setGrab8K(boolean b);
}