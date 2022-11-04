package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "m3u8", type = Type.CRAWLER)
public interface GenericM3u8DecrypterConfig extends PluginConfigInterface {
    public static final GenericM3u8DecrypterConfig.TRANSLATION TRANSLATION                       = new TRANSLATION();
    final String                                               text_CrawlSpeedMode               = "Crawl mode";
    final String                                               text_AddBandwidthValueToFilenames = "Add bandwidth value to filenames?";

    public static class TRANSLATION {
        public String getCrawlSpeedMode_label() {
            return text_CrawlSpeedMode;
        }

        public String getAddBandwidthValueToFilenames_label() {
            return text_AddBandwidthValueToFilenames;
        }
    }

    public static enum CrawlSpeedMode implements LabelInterface {
        SLOW {
            @Override
            public String getLabel() {
                return "Slow: Check individual streams in host plugin";
            }
        },
        FAST {
            @Override
            public String getLabel() {
                return "Fast: Trust crawler and obtain estimated filesizes";
            }
        },
        SUPERFAST {
            @Override
            public String getLabel() {
                return "Super fast: Trust crawler and do not obtain estimated filesize";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("FAST")
    @Order(10)
    @DescriptionForConfigEntry(text_CrawlSpeedMode)
    CrawlSpeedMode getCrawlSpeedMode();

    void setCrawlSpeedMode(final CrawlSpeedMode mode);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_AddBandwidthValueToFilenames)
    @Order(20)
    boolean isAddBandwidthValueToFilenames();

    void setAddBandwidthValueToFilenames(boolean b);
}