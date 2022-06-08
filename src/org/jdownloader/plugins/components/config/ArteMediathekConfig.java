package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "arte.tv", type = Type.CRAWLER)
public interface ArteMediathekConfig extends PluginConfigInterface {
    final String text_CrawlThumbnail                 = "Crawl thumbnail?";
    final String text_QualitySelectionMode           = "Define how this plugin should pick your desired qualities";
    final String text_CrawlSubtitledBurnedInVersions = "Crawl subtitled burned in versions?";

    public static class TRANSLATION {
        public String getCrawlThumbnail_label() {
            return text_CrawlThumbnail;
        }

        public String getQualitySelectionMode_label() {
            return text_QualitySelectionMode;
        }

        public String getCrawlSubtitledBurnedInVersions_label() {
            return text_CrawlSubtitledBurnedInVersions;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlThumbnail)
    @Order(50)
    boolean isCrawlThumbnail();

    void setCrawlThumbnail(boolean b);
    // public static enum QualitySelectionMode implements LabelInterface {
    // BEST {
    // @Override
    // public String getLabel() {
    // return "Best quality";
    // }
    // },
    // // BEST_OF_SELECTED {
    // // @Override
    // // public String getLabel() {
    // // return "Best quality (use selected as highest)";
    // // }
    // // },
    // SELECTED_ONLY {
    // @Override
    // public String getLabel() {
    // return "Selected quality only (fallback = best)";
    // }
    // },
    // ALL {
    // @Override
    // public String getLabel() {
    // return "All available qualities";
    // }
    // };
    // }
    //
    // public static enum Quality implements LabelInterface {
    // Q1080 {
    // @Override
    // public String getLabel() {
    // return "1080p";
    // }
    // },
    // Q720 {
    // @Override
    // public String getLabel() {
    // return "720p";
    // }
    // },
    // Q480 {
    // @Override
    // public String getLabel() {
    // return "480p";
    // }
    // },
    // Q360 {
    // @Override
    // public String getLabel() {
    // return "360p";
    // }
    // },
    // Q240 {
    // @Override
    // public String getLabel() {
    // return "240p";
    // }
    // };
    // }
    //
    // @AboutConfig
    // @DefaultEnumValue("ALL")
    // @DescriptionForConfigEntry(text_QualitySelectionMode)
    // @Order(100)
    // QualitySelectionMode getQualitySelectionMode();
    //
    // void setQualitySelectionMode(QualitySelectionMode mode);
    //
    // @AboutConfig
    // @DefaultEnumValue("Q1080")
    // @Order(101)
    // @DescriptionForConfigEntry("Best will be used if selected preferred quality does not exist")
    // ArteMediathekConfig.Quality getPreferredQuality();
    //
    // void setPreferredQuality(ArteMediathekConfig.Quality quality);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlThumbnail)
    @Order(200)
    boolean isCrawlSubtitledBurnedInVersions();

    void setCrawlSubtitledBurnedInVersions(boolean b);
}