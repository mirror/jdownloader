package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultOnNull;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "missav.com", type = Type.HOSTER)
public interface MissavComConfig extends PluginConfigInterface {
    public static final MissavComConfig.TRANSLATION  TRANSLATION  = new TRANSLATION();
    public static final MissavComConfig.VideoQuality DEFAULT_MODE = VideoQuality.BEST;

    public static class TRANSLATION {
        public String getVideoQuality_label() {
            return "Preferred video quality";
        }
    }

    public static enum VideoQuality implements LabelInterface {
        Q1080P {
            @Override
            public String getLabel() {
                return "1080p";
            }
        },
        Q720P {
            @Override
            public String getLabel() {
                return "720p";
            }
        },
        Q480P {
            @Override
            public String getLabel() {
                return "480p";
            }
        },
        Q360P {
            @Override
            public String getLabel() {
                return "360p";
            }
        },
        BEST {
            @Override
            public String getLabel() {
                return "Best";
            }
        },
        DEFAULT {
            @Override
            public String getLabel() {
                return "Default: " + BEST.getLabel();
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DEFAULT")
    @Order(10)
    @DescriptionForConfigEntry("Select preferred video quality")
    @DefaultOnNull
    MissavComConfig.VideoQuality getVideoQuality();

    void setVideoQuality(final MissavComConfig.VideoQuality mode);
}