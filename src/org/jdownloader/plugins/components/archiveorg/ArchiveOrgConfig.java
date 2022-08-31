package org.jdownloader.plugins.components.archiveorg;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "archive.org", type = Type.CRAWLER)
public interface ArchiveOrgConfig extends PluginConfigInterface {
    final String                    text_FileCrawlerCrawlOnlyOriginalVersions = "File crawler: Download only original versions of files?";
    final String                    text_FileCrawlerCrawlArchiveView          = "File crawler: Also crawl archive view?";
    final String                    text_BookImageQuality                     = "Set book image quality (0 = highest, 10 = lowest)";
    final String                    text_BookCrawlMode                        = "Set book crawl mode";
    public static final TRANSLATION TRANSLATION                               = new TRANSLATION();

    public static class TRANSLATION {
        public String getFileCrawlerCrawlOnlyOriginalVersions_label() {
            return text_FileCrawlerCrawlOnlyOriginalVersions;
        }

        public String getFileCrawlerCrawlArchiveView_label() {
            return text_FileCrawlerCrawlArchiveView;
        }

        public String getBookImageQuality_label() {
            return text_BookImageQuality;
        }

        public String getBookCrawlMode_label() {
            return text_BookCrawlMode;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_FileCrawlerCrawlOnlyOriginalVersions)
    @Order(10)
    boolean isFileCrawlerCrawlOnlyOriginalVersions();

    void setFileCrawlerCrawlOnlyOriginalVersions(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_FileCrawlerCrawlArchiveView)
    @Order(20)
    boolean isFileCrawlerCrawlArchiveView();

    void setFileCrawlerCrawlArchiveView(boolean b);

    @AboutConfig
    @SpinnerValidator(min = 0, max = 10, step = 1)
    @DefaultIntValue(0)
    @DescriptionForConfigEntry(text_BookImageQuality)
    @Order(30)
    int getBookImageQuality();

    void setBookImageQuality(int scaleFactor);

    public static enum BookCrawlMode implements LabelInterface {
        PREFER_ORIGINAL {
            @Override
            public String getLabel() {
                return "Original files if possible else loose book pages";
            }
        },
        ORIGINAL_AND_LOOSE_PAGES {
            @Override
            public String getLabel() {
                return "Original files if possible and loose book pages";
            }
        },
        LOOSE_PAGES {
            @Override
            public String getLabel() {
                return "Only loose book pages";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("PREFER_ORIGINAL")
    @Order(40)
    @DescriptionForConfigEntry(text_BookCrawlMode)
    BookCrawlMode getBookCrawlMode();

    void setBookCrawlMode(final BookCrawlMode bookCrawlerMode);
}