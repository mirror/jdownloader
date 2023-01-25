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

@PluginHost(host = "youtv.de", type = Type.HOSTER)
public interface YoutvDeConfig extends PluginConfigInterface {
    final String                    text_PreferredQuality                      = "Select preferred quality";
    final String                    text_RecordingsCrawlerCrawlItemsRecorded   = "Recordings crawler: Crawl recorded items?";
    final String                    text_RecordingsCrawlerCrawlItemsArchived   = "Recordings crawler: Crawl archived items?";
    final String                    text_RecordingsCrawlerCrawlItemsProgrammed = "Recordings crawler: Also add queued items?";
    final String                    text_DeleteRecordingsAfterDownload         = "Delete recordings after download?";
    public static final TRANSLATION TRANSLATION                                = new TRANSLATION();

    public static class TRANSLATION {
        public String getPreferredQuality_label() {
            return text_PreferredQuality;
        }

        public String getRecordingsCrawlerCrawlItemsRecorded_label() {
            return text_RecordingsCrawlerCrawlItemsRecorded;
        }

        public String getRecordingsCrawlerCrawlItemsArchived_label() {
            return text_RecordingsCrawlerCrawlItemsArchived;
        }

        public String getRecordingsCrawlerCrawlItemsProgrammed_label() {
            return text_RecordingsCrawlerCrawlItemsProgrammed;
        }

        public String getDeleteRecordingsAfterDownload_label() {
            return text_DeleteRecordingsAfterDownload;
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

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_RecordingsCrawlerCrawlItemsRecorded)
    @Order(20)
    boolean isRecordingsCrawlerCrawlItemsRecorded();

    void setRecordingsCrawlerCrawlItemsRecorded(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_RecordingsCrawlerCrawlItemsArchived)
    @Order(21)
    boolean isRecordingsCrawlerCrawlItemsArchived();

    void setRecordingsCrawlerCrawlItemsArchived(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_RecordingsCrawlerCrawlItemsProgrammed)
    @Order(22)
    boolean isRecordingsCrawlerCrawlItemsProgrammed();

    void setRecordingsCrawlerCrawlItemsProgrammed(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_DeleteRecordingsAfterDownload)
    @Order(40)
    boolean isDeleteRecordingsAfterDownload();

    void setDeleteRecordingsAfterDownload(boolean b);
}