package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "aparat.com", type = Type.CRAWLER)
public interface AparatComConfig extends PluginConfigInterface {
    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    public static class TRANSLATION {
        public String getPreferredStreamQuality_label() {
            return "Preferred quality";
        }
    }

    public static enum PreferredStreamQuality implements LabelInterface {
        BEST {
            @Override
            public String getLabel() {
                return "Best";
            }
        },
        Q144P {
            @Override
            public String getLabel() {
                return "144p";
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
        Q2160P {
            @Override
            public String getLabel() {
                return "2160p (4k)";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("BEST")
    @Order(100)
    PreferredStreamQuality getPreferredStreamQuality();

    void setPreferredStreamQuality(PreferredStreamQuality quality);
}