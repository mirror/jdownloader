package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "gfycat.com", type = Type.HOSTER)
public interface GfycatConfig extends PluginConfigInterface {
    public static enum PreferredFormat implements LabelInterface {
        WEBM {
            @Override
            public String getLabel() {
                return "WEBM";
            }
        },
        MP4 {
            @Override
            public String getLabel() {
                return "MP4";
            }
        },
        GIF {
            @Override
            public String getLabel() {
                return "GIF";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("WEBM")
    @DescriptionForConfigEntry("Select preferred format")
    @Order(10)
    PreferredFormat getPreferredFormat();

    void setPreferredFormat(PreferredFormat format);
}