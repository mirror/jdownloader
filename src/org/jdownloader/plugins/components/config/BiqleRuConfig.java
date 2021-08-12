package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "biqle.ru", type = Type.CRAWLER)
public interface BiqleRuConfig extends PluginConfigInterface {
    public static enum QualitySelectionMode implements LabelInterface {
        BEST {
            @Override
            public String getLabel() {
                return "Best quality";
            }
        },
        // BEST_OF_SELECTED {
        // @Override
        // public String getLabel() {
        // return "Best quality (use selected as highest)";
        // }
        // },
        SELECTED_ONLY {
            @Override
            public String getLabel() {
                return "Selected quality only (fallback = best)";
            }
        },
        ALL {
            @Override
            public String getLabel() {
                return "All available qualities";
            }
        };
    }

    public static enum Quality implements LabelInterface {
        Q1080 {
            @Override
            public String getLabel() {
                return "1080p";
            }
        },
        Q720 {
            @Override
            public String getLabel() {
                return "720p";
            }
        },
        Q480 {
            @Override
            public String getLabel() {
                return "480p";
            }
        },
        Q360 {
            @Override
            public String getLabel() {
                return "360p";
            }
        },
        Q240 {
            @Override
            public String getLabel() {
                return "240p";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("ALL")
    @DescriptionForConfigEntry("Define how this plugin should pick your desired qualities?")
    @Order(10)
    QualitySelectionMode getQualitySelectionMode();

    void setQualitySelectionMode(QualitySelectionMode mode);

    @AboutConfig
    @DefaultEnumValue("Q720")
    @Order(20)
    @DescriptionForConfigEntry("Best will be used if selected preferred quality does not exist")
    BiqleRuConfig.Quality getPreferredQuality();

    void setPreferredQuality(BiqleRuConfig.Quality quality);
}