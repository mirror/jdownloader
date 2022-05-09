package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "srf.ch", type = Type.HOSTER)
public interface SrfChConfig extends PluginConfigInterface {
    public static enum Quality implements LabelInterface {
        BEST {
            @Override
            public String getLabel() {
                return "Best";
            }
        },
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
                return "320p";
            }
        },
        Q270 {
            @Override
            public String getLabel() {
                return "270p";
            }
        },
        Q180 {
            @Override
            public String getLabel() {
                return "180p";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("BEST")
    @Order(10)
    @DescriptionForConfigEntry("Best will be used if selected preferred quality does not exist")
    SrfChConfig.Quality getPreferredQuality();

    void setPreferredQuality(SrfChConfig.Quality quality);
}