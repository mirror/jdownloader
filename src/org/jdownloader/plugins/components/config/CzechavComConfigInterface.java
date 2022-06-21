package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "czechav.com", type = Type.HOSTER)
public interface CzechavComConfigInterface extends PluginConfigInterface {
    final String                                              text_QualitySelectionFallbackMode = "Define what to add if based on your selection no results are found";
    final String                                              text_CrawlImages                  = "Crawl images?";
    public static final CzechavComConfigInterface.TRANSLATION TRANSLATION                       = new TRANSLATION();

    public static class TRANSLATION {
        public String getQualitySelectionFallbackMode_label() {
            return text_QualitySelectionFallbackMode;
        }

        public String getCrawlImages_label() {
            return text_CrawlImages;
        }
    }

    @DefaultBooleanValue(false)
    @Order(10)
    boolean isGrabBestVideoVersionEnabled();

    void setGrabBestVideoVersionEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(20)
    boolean isGrab2160pVideoEnabled();

    void setGrab2160pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(30)
    boolean isGrab1080pVideoEnabled();

    void setGrab1080pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(40)
    boolean isGrab720pVideoEnabled();

    void setGrab720pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(50)
    boolean isGrab540pVideoEnabled();

    void setGrab540pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(60)
    boolean isGrab360pVideoEnabled();

    void setGrab360pVideoEnabled(boolean b);

    @DefaultBooleanValue(true)
    @Order(70)
    boolean isGrabOtherResolutionsVideoEnabled();

    void setGrabOtherResolutionsVideoEnabled(boolean b);

    public static enum QualitySelectionFallbackMode implements LabelInterface {
        BEST {
            @Override
            public String getLabel() {
                return "Best quality/qualities";
            }
        },
        ALL {
            @Override
            public String getLabel() {
                return "All qualities";
            }
        },
        NONE {
            @Override
            public String getLabel() {
                return "Nothing";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("ALL")
    @DescriptionForConfigEntry(text_QualitySelectionFallbackMode)
    @Order(80)
    QualitySelectionFallbackMode getQualitySelectionFallbackMode();

    void setQualitySelectionFallbackMode(QualitySelectionFallbackMode mode);

    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlImages)
    @Order(90)
    boolean isCrawlImages();

    void setCrawlImages(boolean b);
}