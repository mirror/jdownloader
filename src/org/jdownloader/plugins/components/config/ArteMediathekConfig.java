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

@PluginHost(host = "arte.tv", type = Type.CRAWLER)
public interface ArteMediathekConfig extends PluginConfigInterface {
    final String                                        text_CrawlThumbnail                 = "Crawl thumbnail?";
    final String                                        text_QualitySelectionMode           = "Define how this plugin should pick your desired qualities";
    final String                                        text_CrawlSubtitledBurnedInVersions = "Crawl subtitled burned in versions?";
    final String                                        text_QualitySelectionFallbackMode   = "Define what to add if based on your selection no results are found";
    // final String text_CrawlSubtitledBurnedInVersionsHearingImpaired = "Crawl subtitled burned in versions for hearing impaired?";
    public static final ArteMediathekConfig.TRANSLATION TRANSLATION                         = new TRANSLATION();

    public static class TRANSLATION {
        public String getCrawlThumbnail_label() {
            return text_CrawlThumbnail;
        }

        public String getCrawlSubtitledBurnedInVersions_label() {
            return text_CrawlSubtitledBurnedInVersions;
        }
        // public String getCrawlSubtitledBurnedInVersionsHearingImpaired_label() {
        // return text_CrawlSubtitledBurnedInVersionsHearingImpaired;
        // }

        public String getQualitySelectionMode_label() {
            return text_QualitySelectionMode;
        }

        public String getQualitySelectionFallbackMode_label() {
            return text_QualitySelectionFallbackMode;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlThumbnail)
    @Order(50)
    boolean isCrawlThumbnail();

    void setCrawlThumbnail(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlSubtitledBurnedInVersions)
    @Order(60)
    boolean isCrawlSubtitledBurnedInVersions();

    void setCrawlSubtitledBurnedInVersions(boolean b);

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
    // @DescriptionForConfigEntry(text_CrawlSubtitledBurnedInVersions)
    @Order(102)
    boolean isCrawlHTTP240p();

    void setCrawlHTTP240p(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    // @DescriptionForConfigEntry(text_CrawlSubtitledBurnedInVersions)
    @Order(103)
    boolean isCrawlHTTP360p();

    void setCrawlHTTP360p(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    // @DescriptionForConfigEntry(text_CrawlSubtitledBurnedInVersions)
    @Order(104)
    boolean isCrawlHTTP480p();

    void setCrawlHTTP480p(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    // @DescriptionForConfigEntry(text_CrawlSubtitledBurnedInVersions)
    @Order(105)
    boolean isCrawlHTTP720p();

    void setCrawlHTTP720p(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    // @DescriptionForConfigEntry(text_CrawlSubtitledBurnedInVersions)
    @Order(106)
    boolean isCrawlHTTP1080p();

    void setCrawlHTTP1080p(boolean b);
    // @AboutConfig
    // @DefaultBooleanValue(true)
    // @DescriptionForConfigEntry(text_CrawlSubtitledBurnedInVersionsHearingImpaired)
    // @Order(200)
    // boolean isCrawlSubtitledBurnedInVersionsHearingImpaired();
    //
    // void setCrawlSubtitledBurnedInVersionsHearingImpaired(boolean b);

    public static enum QualitySelectionFallbackMode implements LabelInterface {
        BEST {
            @Override
            public String getLabel() {
                return "Best quality";
            }
        },
        ALL {
            @Override
            public String getLabel() {
                return "All qualities";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("ALL")
    @DescriptionForConfigEntry(text_QualitySelectionFallbackMode)
    @Order(200)
    QualitySelectionFallbackMode getQualitySelectionFallbackMode();

    void setQualitySelectionFallbackMode(QualitySelectionFallbackMode mode);
}