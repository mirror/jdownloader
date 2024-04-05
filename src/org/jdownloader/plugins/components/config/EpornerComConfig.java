package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "eporner.com", type = Type.HOSTER)
public interface EpornerComConfig extends PluginConfigInterface {
    public static enum PreferredStreamQuality implements LabelInterface {
        BEST {
            @Override
            public String getLabel() {
                return "Best";
            }
        },
        Q240P {
            @Override
            public String getLabel() {
                return "240p";
            }
        },
        Q360P {
            @Override
            public String getLabel() {
                return "360p";
            }
        },
        Q480P {
            @Override
            public String getLabel() {
                return "480p";
            }
        },
        Q720P {
            @Override
            public String getLabel() {
                return "720p";
            }
        },
        Q1080P {
            @Override
            public String getLabel() {
                return "1080p";
            }
        },
        Q1440P {
            @Override
            public String getLabel() {
                return "1440p";
            }
        },
        Q2160P {
            @Override
            public String getLabel() {
                return "2160p (4k)";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("BEST")
    @DescriptionForConfigEntry("If your preferred stream quality is not found, best quality will be downloaded instead.")
    @Order(100)
    PreferredStreamQuality getPreferredStreamQuality();

    void setPreferredStreamQuality(PreferredStreamQuality quality);

    final PreferredVideoCodec defaultPreferredVideoCodec = PreferredVideoCodec.H264;

    public static enum PreferredVideoCodec implements LabelInterface {
        AV1 {
            @Override
            public String getLabel() {
                return "AV1";
            }
        },
        H264 {
            @Override
            public String getLabel() {
                return "H.264";
            }
        },
        DEFAULT {
            @Override
            public String getLabel() {
                return "Default: " + defaultPreferredVideoCodec.getLabel();
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DEFAULT")
    @DescriptionForConfigEntry("If your video codec is not found, first/random codec will be used.")
    @Order(110)
    PreferredVideoCodec getPreferredVideoCodec();

    void setPreferredVideoCodec(PreferredVideoCodec quality);
}