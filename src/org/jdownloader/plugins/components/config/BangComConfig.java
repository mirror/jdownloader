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

@PluginHost(host = "bang.com", type = Type.HOSTER)
public interface BangComConfig extends PluginConfigInterface {
    final String                    text_GrabPreviewVideo     = "Grab preview video?";
    final String                    text_GrabThumbnail        = "Grab thumbnail?";
    final String                    text_GrabPhotosZipArchive = "Grab photos zip archive?";
    final String                    text_QualitySelectionMode = "Define how this plugin should pick your desired qualities";
    final String                    text_Crawl360p            = "Crawl 360p?";
    final String                    text_Crawl480p            = "Crawl 480p?";
    final String                    text_Crawl540p            = "Crawl 540p?";
    final String                    text_Crawl720p            = "Crawl 720p?";
    final String                    text_Crawl1080p           = "Crawl 1080p?";
    final String                    text_Crawl2160p           = "Crawl 2160p?";
    public static final TRANSLATION TRANSLATION               = new TRANSLATION();

    public static class TRANSLATION {
        public String getGrabPreviewVideo_label() {
            return text_GrabPreviewVideo;
        }

        public String getGrabThumbnail_label() {
            return text_GrabThumbnail;
        }

        public String getGrabPhotosZipArchive_label() {
            return text_GrabPhotosZipArchive;
        }

        public String getQualitySelectionMode_label() {
            return text_QualitySelectionMode;
        }

        public String getCrawl360p_label() {
            return text_Crawl360p;
        }

        public String getCrawl480p_label() {
            return text_Crawl480p;
        }

        public String getCrawl540p_label() {
            return text_Crawl540p;
        }

        public String getCrawl720p_label() {
            return text_Crawl720p;
        }

        public String getCrawl1080p_label() {
            return text_Crawl1080p;
        }

        public String getCrawl2160p_label() {
            return text_Crawl2160p;
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

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_GrabPhotosZipArchive)
    @Order(30)
    boolean isGrabPhotosZipArchive();

    void setGrabPhotosZipArchive(boolean b);

    public static enum QualitySelectionMode implements LabelInterface {
        BEST {
            @Override
            public String getLabel() {
                return "Best quality";
            }
        },
        BEST_OF_SELECTED {
            @Override
            public String getLabel() {
                return "Best quality of selected";
            }
        },
        ALL_SELECTED {
            @Override
            public String getLabel() {
                return "All selected qualities";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("ALL_SELECTED")
    @DescriptionForConfigEntry(text_QualitySelectionMode)
    @Order(100)
    QualitySelectionMode getQualitySelectionMode();

    void setQualitySelectionMode(QualitySelectionMode mode);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_Crawl360p)
    @Order(103)
    boolean isCrawl360p();

    void setCrawl360p(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_Crawl480p)
    @Order(102)
    boolean isCrawl480p();

    void setCrawl480p(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_Crawl540p)
    @Order(104)
    boolean isCrawl540p();

    void setCrawl540p(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_Crawl720p)
    @Order(105)
    boolean isCrawl720p();

    void setCrawl720p(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_Crawl1080p)
    @Order(106)
    boolean isCrawl1080p();

    void setCrawl1080p(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_Crawl2160p)
    @Order(106)
    boolean isCrawl2160p();

    void setCrawl2160p(boolean b);
}