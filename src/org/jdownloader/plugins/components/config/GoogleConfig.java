package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "drive.google.com", type = Type.HOSTER)
public interface GoogleConfig extends PluginConfigInterface {
    @AboutConfig
    @DefaultStringValue("JDDEFAULT")
    @DescriptionForConfigEntry("Enter the User-Agent which should be used for all Google related http requests")
    @Order(10)
    String getUserAgent();

    public static enum PreferredQuality implements LabelInterface {
        ORIGINAL {
            @Override
            public String getLabel() {
                return "Original file";
            }
        },
        STREAM_BEST {
            @Override
            public String getLabel() {
                return "Best stream";
            }
        },
        STREAM_360P {
            @Override
            public String getLabel() {
                return "360p";
            }
        },
        STREAM_480P {
            @Override
            public String getLabel() {
                return "480p";
            }
        },
        STREAM_720P {
            @Override
            public String getLabel() {
                return "720p";
            }
        },
        STREAM_1080P {
            @Override
            public String getLabel() {
                return "1080p";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("ORIGINAL")
    @DescriptionForConfigEntry("If your preferred stream quality is not found, best stream quality will be downloaded instead.")
    @Order(20)
    PreferredQuality getPreferredQuality();

    void setPreferredQuality(final PreferredQuality quality);
}