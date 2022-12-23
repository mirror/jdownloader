package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "youtv.de", type = Type.HOSTER)
public interface YoutvDeConfig extends PluginConfigInterface {
    final String                    text_PreferredQuality = "Select preferred quality";
    public static final TRANSLATION TRANSLATION           = new TRANSLATION();

    public static class TRANSLATION {
        public String getPreferredQuality_label() {
            return text_PreferredQuality;
        }
    }

    public static enum PreferredQuality implements LabelInterface {
        HD {
            @Override
            public String getLabel() {
                return "HD - High Definition";
            }
        },
        HQ {
            @Override
            public String getLabel() {
                return "HQ - High Quality";
            }
        },
        NQ {
            @Override
            public String getLabel() {
                return "NQ - Normal Quality";
            }
        },
        AD {
            @Override
            public String getLabel() {
                return "AD - Hörfilmspur";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("HD")
    @DescriptionForConfigEntry(text_PreferredQuality)
    @Order(10)
    PreferredQuality getPreferredQuality();

    void setPreferredQuality(final PreferredQuality quality);
}