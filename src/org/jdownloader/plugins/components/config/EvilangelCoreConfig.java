package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;

public interface EvilangelCoreConfig extends PluginConfigInterface {
    public static enum Quality implements LabelInterface {
        BEST {
            @Override
            public String getLabel() {
                return "Best";
            }
        },
        Q2160 {
            @Override
            public String getLabel() {
                return "4k 2160p";
            }
        },
        Q1080 {
            @Override
            public String getLabel() {
                return "Full HD 1080p";
            }
        },
        Q720 {
            @Override
            public String getLabel() {
                return "HD 720p";
            }
        },
        Q540 {
            @Override
            public String getLabel() {
                return "540p";
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
        },
        Q160 {
            @Override
            public String getLabel() {
                return "160p";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("BEST")
    @Order(10)
    @DescriptionForConfigEntry("Best will be used if selected preferred quality does not exist")
    EvilangelComConfig.Quality getPreferredQuality();

    void setPreferredQuality(EvilangelComConfig.Quality quality);
}