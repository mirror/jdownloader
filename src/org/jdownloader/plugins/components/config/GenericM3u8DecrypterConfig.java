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
    final static String                                        text_CrawlSpeedMode               = "Crawl mode";
    final static String                                        text_AddBandwidthValueToFilenames = "Add bandwidth value to filenames?";
    final static String                                        text_GroupByResolution            = "Group by resolution?";

    public static class TRANSLATION {
        public String getCrawlSpeedMode_label() {
            return text_CrawlSpeedMode;
        }

        public String getAddBandwidthValueToFilenames_label() {
            return text_AddBandwidthValueToFilenames;
        }

        public String getGroupByResolution_label() {
            return text_GroupByResolution;
        }
    }

    public static enum CrawlSpeedMode implements LabelInterface {
        SLOW {
            @Override
            public String getLabel() {
                return "Slow: Check individual streams in host plugin. Host Plugin probes for additional information like resolution, codec and more.";
            }
        },
        FAST {
            @Override
            public String getLabel() {
                return "Fast: Trust crawler and obtain estimated filesizes. Additional information, like resolution, might not yet be available/known.";
            }
        },
        SUPERFAST {
            @Override
            public String getLabel() {
                return "Super fast: Trust crawler and do not obtain estimated filesize. Additional information, like resolution, might not yet be available/known.";
            }
        },
        AUTOMATIC_FAST {
            @Override
            public String getLabel() {
                return "Automatic: Prefer FAST if non video or resolution is available/known, else fall back to SLOW";
            }
        },
        AUTOMATIC_SUPERFAST {
            @Override
            public String getLabel() {
                return "Automatic: Prefer SUPERFAST if non video or resolution is available/known, else fall back to SLOW";
            }
        }
    }

    @AboutConfig
    @DefaultEnumValue("AUTOMATIC_SUPERFAST")
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

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_GroupByResolution)
    @Order(30)
    boolean isGroupByResolution();

    void setGroupByResolution(boolean b);
}