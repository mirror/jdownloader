package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "fembed.com", type = Type.CRAWLER)
public interface FEmbedComConfig extends PluginConfigInterface {
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
        Q360 {
            @Override
            public String getLabel() {
                return "360p";
            }
        },
        Q480 {
            @Override
            public String getLabel() {
                return "480p";
            }
        },
        Q720 {
            @Override
            public String getLabel() {
                return "720p";
            }
        },
        Q1080 {
            @Override
            public String getLabel() {
                return "1080p";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("BEST")
    @DescriptionForConfigEntry("Define how this plugin should pick your desired qualities?")
    @Order(50)
    QualitySelectionMode getQualitySelectionMode();

    void setQualitySelectionMode(QualitySelectionMode mode);

    @AboutConfig
    @DefaultEnumValue("Q1080")
    @DescriptionForConfigEntry("If your preferred stream quality is not found, best quality will be downloaded instead.")
    @Order(100)
    Quality getPreferredStreamQuality();

    void setPreferredStreamQuality(Quality quality);
}