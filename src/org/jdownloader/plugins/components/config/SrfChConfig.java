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

@PluginHost(host = "srf.ch", type = Type.HOSTER)
public interface SrfChConfig extends PluginConfigInterface {
    final String                    text_CrawlThumbnail               = "Crawl thumbnail?";
    final String                    text_QualitySelectionMode         = "Define how this plugin should pick your desired qualities";
    final String                    text_Crawl180p                    = "Crawl 180?";
    final String                    text_Crawl270p                    = "Crawl 270?";
    final String                    text_Crawl360p                    = "Crawl 360p?";
    final String                    text_Crawl540p                    = "Crawl 540?";
    final String                    text_Crawl720p                    = "Crawl 720p?";
    final String                    text_Crawl1080p                   = "Crawl 1080p?";
    final String                    text_QualitySelectionFallbackMode = "Define what to add if based on your selection no results are found";
    public static final TRANSLATION TRANSLATION                       = new TRANSLATION();

    public static class TRANSLATION {
        public String getCrawlThumbnail_label() {
            return text_CrawlThumbnail;
        }

        public String getQualitySelectionMode_label() {
            return text_QualitySelectionMode;
        }

        public String getCrawl180p_label() {
            return text_Crawl180p;
        }

        public String getCrawl270p_label() {
            return text_Crawl270p;
        }

        public String getCrawl360p_label() {
            return text_Crawl360p;
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

        public String getQualitySelectionFallbackMode_label() {
            return text_QualitySelectionFallbackMode;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlThumbnail)
    @Order(90)
    boolean isCrawlThumbnail();

    void setCrawlThumbnail(boolean b);

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
    @DescriptionForConfigEntry(text_Crawl180p)
    @Order(102)
    boolean isCrawl180p();

    void setCrawl180p(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_Crawl270p)
    @Order(102)
    boolean isCrawl270p();

    void setCrawl270p(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_Crawl360p)
    @Order(103)
    boolean isCrawl360p();

    void setCrawl360p(boolean b);

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
                return "Nothing";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("ALL")
    @DescriptionForConfigEntry(text_QualitySelectionFallbackMode)
    @Order(400)
    QualitySelectionFallbackMode getQualitySelectionFallbackMode();

    void setQualitySelectionFallbackMode(QualitySelectionFallbackMode mode);
}