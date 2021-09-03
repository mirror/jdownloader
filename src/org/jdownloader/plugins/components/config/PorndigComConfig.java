package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "my.mail.ru", type = Type.HOSTER)
public interface PorndigComConfig extends PluginConfigInterface {
    public static enum PreferredQuality implements LabelInterface {
        BEST {
            @Override
            public String getLabel() {
                return "Best";
            }
        },
        Q270P {
            @Override
            public String getLabel() {
                return "270p";
            }
        },
        Q360P {
            @Override
            public String getLabel() {
                return "360p";
            }
        },
        Q540P {
            @Override
            public String getLabel() {
                return "540p";
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
        UHD4K {
            @Override
            public String getLabel() {
                return "UHD 4K";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("BEST")
    @DescriptionForConfigEntry("If your preferred quality is not found, best quality will be downloaded instead.")
    PreferredQuality getPreferredQuality();

    void setPreferredQuality(PreferredQuality quality);
}