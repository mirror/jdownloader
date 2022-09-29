package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "raiplay.tv", type = Type.CRAWLER)
public interface RaiplayItConfig extends PluginConfigInterface {
    final String              text_QualitySelectionMode = "Quality selection mode";
    public static TRANSLATION TRANSLATION               = new TRANSLATION();

    public static class TRANSLATION {
        public String getQualitySelectionMode_label() {
            return text_QualitySelectionMode;
        }
    }

    public static enum QualitySelectionMode implements LabelInterface {
        BEST {
            @Override
            public String getLabel() {
                return "Best quality";
            }
        },
        ALL {
            @Override
            public String getLabel() {
                return "All qualities";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("ALL")
    @DescriptionForConfigEntry(text_QualitySelectionMode)
    @Order(10)
    QualitySelectionMode getQualitySelectionMode();

    void setQualitySelectionMode(QualitySelectionMode mode);
}
