package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "imgsrc.ru", type = Type.HOSTER)
public interface ImgsrcConfig extends PluginConfigInterface {
    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    public static class TRANSLATION {
        public String getPreferredImageFormat_label() {
            return "Preferred image format";
        }
    }

    public static enum ImageFormat implements LabelInterface {
        JPEG {
            @Override
            public String getLabel() {
                return "JPEG";
            }
        },
        WEBP {
            @Override
            public String getLabel() {
                return "WEBP";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("JPEG")
    @Order(10)
    @DescriptionForConfigEntry("If the preferred format is not found, another one will be chosen. Animated images are only available as in GIF format.")
    ImageFormat getPreferredImageFormat();

    void setPreferredImageFormat(final ImageFormat format);
}