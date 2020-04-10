package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "uptobox.com", type = Type.HOSTER)
public interface UpToBoxComConfig extends PluginConfigInterface {
    public static enum PreferredQuality implements LabelInterface {
        DEFAULT {
            @Override
            public String getLabel() {
                return "Source/Original/Best";
            }
        },
        QUALITY1 {
            @Override
            public String getLabel() {
                return "240p";
            }
        },
        QUALITY2 {
            @Override
            public String getLabel() {
                return "360p";
            }
        },
        QUALITY3 {
            @Override
            public String getLabel() {
                return "720p";
            }
        },
        QUALITY4 {
            @Override
            public String getLabel() {
                return "1080p";
            }
        },
        QUALITY5 {
            @Override
            public String getLabel() {
                return "4k";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DEFAULT")
    PreferredQuality getPreferredQuality();

    void setPreferredQuality(PreferredQuality domain);

    @DefaultBooleanValue(true)
    // @DescriptionForConfigEntry("If enabled, JD will try to crawl a subtitle file for all uptostream URLs.")
    @Order(50)
    boolean isGrabSubtitle();

    void setGrabSubtitle(boolean b);
}