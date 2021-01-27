package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.TakeValueFromSubconfig;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "xvideos.com", type = Type.HOSTER)
public interface XvideosComConfig extends PluginConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Enable fast linkcheck for host plugin? If enabled, filesize won't be displayed until download is started!")
    @Order(15)
    boolean isEnableFastLinkcheckForHostPlugin();

    void setEnableFastLinkcheckForHostPlugin(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("Prefer HLS")
    @DescriptionForConfigEntry("Prefer HLS download?")
    @Order(30)
    boolean isPreferHLSDownload();

    void setPreferHLSDownload(boolean b);

    public static enum PreferredHLSQuality implements LabelInterface {
        BEST {
            @Override
            public String getLabel() {
                return "Best";
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
        Q2160P {
            @Override
            public String getLabel() {
                return "2160p (4k)";
            }
        };
    }

    public static enum PreferredHTTPQuality implements LabelInterface {
        HIGH {
            @Override
            public String getLabel() {
                return "High quality";
            }
        },
        LOW {
            @Override
            public String getLabel() {
                return "Low quality";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("BEST")
    @DescriptionForConfigEntry("Select preferred HLS download quality. If your preferred HLS quality is not found, best quality will be downloaded instead.")
    @Order(100)
    PreferredHLSQuality getPreferredHLSQuality();

    void setPreferredHLSQuality(PreferredHLSQuality quality);

    @AboutConfig
    @DefaultEnumValue("HIGH")
    @DescriptionForConfigEntry("Select preferred HTTP download quality. If your preferred HTTP quality is not found, best quality will be downloaded instead.")
    @Order(120)
    PreferredHTTPQuality getPreferredHTTPQuality();

    void setPreferredHTTPQuality(PreferredHTTPQuality quality);

    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("xvideos.com can 'shadow ban' users who download a lot. This will limit the max. available quality to 240p. This experimental setting will make JD try to detect this limit.")
    @Order(140)
    boolean isTryToRecognizeLimit();

    void setTryToRecognizeLimit(boolean b);
}