package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "hitomi.la", type = Type.CRAWLER)
public interface HitomiLaConfig extends PluginConfigInterface {
    final String                    text_PreferredImageFormat = "Select preferred image format";
    public static final TRANSLATION TRANSLATION               = new TRANSLATION();

    public static class TRANSLATION {
        public String getPreferredQuality_label() {
            return text_PreferredImageFormat;
        }
    }

    public static enum PreferredImageFormat implements LabelInterface {
        WEBP {
            @Override
            public String getLabel() {
                return "WEBP";
            }
        },
        AVIF {
            @Override
            public String getLabel() {
                return "AVIF";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("WEBP")
    @DescriptionForConfigEntry(text_PreferredImageFormat)
    @Order(10)
    PreferredImageFormat getPreferredImageFormat();

    void setPreferredImageFormat(final PreferredImageFormat quality);
}