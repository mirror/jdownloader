package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.TakeValueFromSubconfig;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "arte.tv", type = Type.CRAWLER)
public interface ArteMediathekConfig extends PluginConfigInterface {
    final String                                        text_CrawlThumbnail                                 = "Crawl thumbnail?";
    final String                                        text_ThumbnailFilenameMode                          = "Set thumbnail filename mode";
    final String                                        text_QualitySelectionMode                           = "Define how this plugin should pick your desired qualities";
    final String                                        text_CrawlSubtitledBurnedInVersionsFull             = "Crawl subtitled burned in versions (full)?";
    final String                                        text_CrawlSubtitledBurnedInVersionsPartial          = "Crawl subtitled burned in versions (partial/forced)?";
    final String                                        text_CrawlSubtitledBurnedInVersionsHearingImpaired  = "Crawl subtitled burned in versions (hearing impaired)?";
    final String                                        text_CrawlSubtitledBurnedInVersionsAudioDescription = "Crawl subtitled burned in versions (audio description)?";
    final String                                        text_CrawlHTTP240p                                  = "Crawl 240p?";
    final String                                        text_CrawlHTTP360p                                  = "Crawl 360p?";
    final String                                        text_CrawlHTTP480p                                  = "Crawl 480p?";
    final String                                        text_CrawlHTTP720p                                  = "Crawl 720p?";
    final String                                        text_CrawlHTTP1080p                                 = "Crawl 1080p?";
    final String                                        text_CrawlUnknownHTTPVideoQualities                 = "Crawl unknown http video qualities?";
    final String                                        text_LanguageSelectionMode                          = "Language selection mode";
    final String                                        text_CrawlLanguageEnglish                           = "Crawl language english?";
    final String                                        text_CrawlLanguageFrench                            = "Crawl language french?";
    final String                                        text_CrawlLanguageGerman                            = "Crawl language german?";
    final String                                        text_CrawlLanguageItalian                           = "Crawl language italian?";
    final String                                        text_CrawlLanguagePolish                            = "Crawl language polish?";
    final String                                        text_CrawlLanguageSpanish                           = "Crawl language spanish?";
    final String                                        text_CrawlLanguageUnknown                           = "Crawl unknown languages?";
    final String                                        text_QualitySelectionFallbackMode                   = "Define what to add if based on your selection no video results are found";
    final String                                        text_GetFilenameSchemeTypeV2                        = "Select filename scheme type";
    final String                                        text_GetFilenameScheme                              = "Enter custom filename scheme";
    final String                                        text_GetPackagenameSchemeType                       = "Select package name scheme type";
    final String                                        text_GetPackagenameScheme                           = "Enter custom package name scheme";
    public static final ArteMediathekConfig.TRANSLATION TRANSLATION                                         = new TRANSLATION();

    public static class TRANSLATION {
        public String getCrawlThumbnail_label() {
            return text_CrawlThumbnail;
        }

        public String getThumbnailFilenameMode_label() {
            return text_ThumbnailFilenameMode;
        }

        public String getCrawlSubtitledBurnedInVersionsFull_label() {
            return text_CrawlSubtitledBurnedInVersionsFull;
        }

        public String getCrawlSubtitledBurnedInVersionsPartial_label() {
            return text_CrawlSubtitledBurnedInVersionsPartial;
        }

        public String getCrawlSubtitledBurnedInVersionsHearingImpaired_label() {
            return text_CrawlSubtitledBurnedInVersionsHearingImpaired;
        }

        public String getCrawlSubtitledBurnedInVersionsAudioDescription_label() {
            return text_CrawlSubtitledBurnedInVersionsAudioDescription;
        }

        public String getQualitySelectionMode_label() {
            return text_QualitySelectionMode;
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

        public String getCrawlUnknownHTTPVideoQualities_label() {
            return text_CrawlUnknownHTTPVideoQualities;
        }

        public String getLanguageSelectionMode_label() {
            return text_LanguageSelectionMode;
        }

        public String getCrawlLanguageEnglish_label() {
            return text_CrawlLanguageEnglish;
        }

        public String getCrawlLanguageFrench_label() {
            return text_CrawlLanguageFrench;
        }

        public String getCrawlLanguageGerman_label() {
            return text_CrawlLanguageGerman;
        }

        public String getCrawlLanguageItalian_label() {
            return text_CrawlLanguageItalian;
        }

        public String getCrawlLanguagePolish_label() {
            return text_CrawlLanguagePolish;
        }

        public String getCrawlLanguageSpanish_label() {
            return text_CrawlLanguageSpanish;
        }

        public String getCrawlLanguageUnknown_label() {
            return text_CrawlLanguageUnknown;
        }

        public String getQualitySelectionFallbackMode_label() {
            return text_QualitySelectionFallbackMode;
        }

        public String getFilenameSchemeTypeV2_label() {
            return text_GetFilenameSchemeTypeV2;
        }

        public String getFilenameScheme_label() {
            return text_GetFilenameScheme;
        }

        public String getPackagenameSchemeType_label() {
            return text_GetPackagenameSchemeType;
        }

        public String getPackagenameScheme_label() {
            return text_GetPackagenameScheme;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlThumbnail)
    @Order(50)
    boolean isCrawlThumbnail();

    void setCrawlThumbnail(boolean b);

    public static enum ThumbnailFilenameMode implements LabelInterface {
        AUTO {
            @Override
            public String getLabel() {
                return "Auto: Same like video for single video packages else like package name";
            }
        },
        SHORT {
            @Override
            public String getLabel() {
                return "Package name";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("AUTO")
    @DescriptionForConfigEntry(text_ThumbnailFilenameMode)
    @Order(51)
    ThumbnailFilenameMode getThumbnailFilenameMode();

    void setThumbnailFilenameMode(ThumbnailFilenameMode thumbnailFilenameScheme);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlSubtitledBurnedInVersionsFull)
    @Order(60)
    boolean isCrawlSubtitledBurnedInVersionsFull();

    void setCrawlSubtitledBurnedInVersionsFull(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlSubtitledBurnedInVersionsPartial)
    @Order(61)
    boolean isCrawlSubtitledBurnedInVersionsPartial();

    void setCrawlSubtitledBurnedInVersionsPartial(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlSubtitledBurnedInVersionsHearingImpaired)
    @Order(62)
    boolean isCrawlSubtitledBurnedInVersionsHearingImpaired();

    void setCrawlSubtitledBurnedInVersionsHearingImpaired(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlSubtitledBurnedInVersionsAudioDescription)
    @Order(63)
    boolean isCrawlSubtitledBurnedInVersionsAudioDescription();

    void setCrawlSubtitledBurnedInVersionsAudioDescription(boolean b);

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

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlUnknownHTTPVideoQualities)
    @Order(110)
    boolean isCrawlUnknownHTTPVideoQualities();

    void setCrawlUnknownHTTPVideoQualities(boolean b);

    public static enum LanguageSelectionMode implements LabelInterface {
        AUTO {
            @Override
            public String getLabel() {
                return "Auto: e.g. URL contains arte.tv/de/... -> Allow german and original versions";
            }
        },
        ALL_SELECTED {
            @Override
            public String getLabel() {
                return "All selected languages";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("ALL_SELECTED")
    @DescriptionForConfigEntry(text_LanguageSelectionMode)
    @Order(200)
    LanguageSelectionMode getLanguageSelectionMode();

    void setLanguageSelectionMode(LanguageSelectionMode mode);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlLanguageEnglish)
    @Order(300)
    boolean isCrawlLanguageEnglish();

    void setCrawlLanguageEnglish(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlLanguageFrench)
    @Order(301)
    boolean isCrawlLanguageFrench();

    void setCrawlLanguageFrench(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlLanguageGerman)
    @Order(302)
    boolean isCrawlLanguageGerman();

    void setCrawlLanguageGerman(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlLanguageItalian)
    @Order(303)
    boolean isCrawlLanguageItalian();

    void setCrawlLanguageItalian(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlLanguagePolish)
    @Order(304)
    boolean isCrawlLanguagePolish();

    void setCrawlLanguagePolish(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlLanguageSpanish)
    @Order(305)
    boolean isCrawlLanguageSpanish();

    void setCrawlLanguageSpanish(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlLanguageUnknown)
    @Order(310)
    boolean isCrawlLanguageUnknown();

    void setCrawlLanguageUnknown(boolean b);

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
        },
        NONE {
            @Override
            public String getLabel() {
                return "Nothing (or thumbnail only)";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("ALL")
    @DescriptionForConfigEntry(text_QualitySelectionFallbackMode)
    @Order(400)
    QualitySelectionFallbackMode getQualitySelectionFallbackMode();

    void setQualitySelectionFallbackMode(QualitySelectionFallbackMode mode);

    @AboutConfig
    @DefaultEnumValue("DEFAULT")
    @DescriptionForConfigEntry(text_GetFilenameSchemeTypeV2)
    @Order(500)
    FilenameSchemeType getFilenameSchemeTypeV2();

    void setFilenameSchemeTypeV2(FilenameSchemeType mode);

    public static enum FilenameSchemeType implements LabelInterface {
        DEFAULT {
            @Override
            public String getLabel() {
                return "Default: *date*_*platform*_*title_and_subtitle*_*video_id*_*language*_*shortlanguage*_*resolution*_*bitrate**ext*";
            }
        },
        ORIGINAL {
            @Override
            public String getLabel() {
                return "Original";
            }
        },
        CUSTOM {
            @Override
            public String getLabel() {
                return "Custom";
            }
        },
        LEGACY {
            @Override
            public String getLabel() {
                return "Legacy (like old plugin: *date*_arte_*title_and_subtitle*_*video_id*_*language*_*shortlanguage*_*resolution*_*bitrate*)";
            }
        };
    }

    @AboutConfig
    @DefaultStringValue("*date*_*platform*_*title_and_subtitle*_*title*_*subtitle*_*video_id*_*language*_*shortlanguage*_*resolution*_*width*_*height*_*bitrate**original_filename**ext*")
    @TakeValueFromSubconfig("CUSTOM_FILE_NAME_PATTERN")
    @DescriptionForConfigEntry(text_GetFilenameScheme)
    @Order(501)
    String getFilenameScheme();

    void setFilenameScheme(String str);

    @AboutConfig
    @DefaultEnumValue("DEFAULT")
    @DescriptionForConfigEntry(text_GetFilenameSchemeTypeV2)
    @Order(600)
    PackagenameSchemeType getPackagenameSchemeType();

    void setPackagenameSchemeType(PackagenameSchemeType mode);

    public static enum PackagenameSchemeType implements LabelInterface {
        DEFAULT {
            @Override
            public String getLabel() {
                return "Default: *title_and_subtitle*";
            }
        },
        CUSTOM {
            @Override
            public String getLabel() {
                return "Custom";
            }
        },
        LEGACY {
            @Override
            public String getLabel() {
                return "Legacy (like old plugin: *date*_arte_*title_and_subtitle*)";
            }
        };
    }

    @AboutConfig
    @DefaultStringValue("*date*_*platform*_*title_and_subtitle*_*title*_*subtitle*_*video_id*")
    @TakeValueFromSubconfig("CUSTOM_PACKAGE_NAME_PATTERN")
    @DescriptionForConfigEntry(text_GetFilenameScheme)
    @Order(601)
    String getPackagenameScheme();

    void setPackagenameScheme(String str);
}