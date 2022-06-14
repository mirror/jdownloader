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
    final String                                        text_CrawlHTTP240p                  = "Crawl 240p?";
    final String                                        text_CrawlHTTP360p                  = "Crawl 360p?";
    final String                                        text_CrawlHTTP480p                  = "Crawl 480p?";
    final String                                        text_CrawlHTTP720p                  = "Crawl 720p?";
    final String                                        text_CrawlHTTP1080p                 = "Crawl 1080p?";
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

        public String getCrawlHTTP240p_label() {
            return text_CrawlHTTP240p;
        }

        public String getCrawlHTTP360p_label() {
            return text_CrawlHTTP360p;
        }

        public String getCrawlHTTP480p_label() {
            return text_CrawlHTTP480p;
        }

        public String getCrawlHTTP720p_label() {
            return text_CrawlHTTP720p;
        }

        public String getCrawlHTTP1080p_label() {
            return text_CrawlHTTP1080p;
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
    @DescriptionForConfigEntry(text_CrawlHTTP240p)
    @Order(102)
    boolean isCrawlHTTP240p();

    void setCrawlHTTP240p(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlHTTP360p)
    @Order(103)
    boolean isCrawlHTTP360p();

    void setCrawlHTTP360p(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlHTTP480p)
    @Order(104)
    boolean isCrawlHTTP480p();

    void setCrawlHTTP480p(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlHTTP720p)
    @Order(105)
    boolean isCrawlHTTP720p();

    void setCrawlHTTP720p(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlHTTP1080p)
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
    @Order(200)
    QualitySelectionFallbackMode getQualitySelectionFallbackMode();

    void setQualitySelectionFallbackMode(QualitySelectionFallbackMode mode);
}