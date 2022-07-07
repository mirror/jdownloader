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
    final String                    text_PreferOriginal   = "Prefer original?";
    final String                    text_CrawlArchiveView = "Also crawl archive view?";
    final String                    text_BookImageQuality = "Set book image quality (0 = highest, 10 = lowest)";
    final String                    text_BookCrawlMode    = "Set book crawl mode";
    public static final TRANSLATION TRANSLATION           = new TRANSLATION();

    public static class TRANSLATION {
        public String getPreferOriginal_label() {
            return text_PreferOriginal;
        }

        public String getCrawlArchiveView_label() {
            return text_CrawlArchiveView;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_PreferOriginal)
    @Order(10)
    boolean isPreferOriginal();

    void setPreferOriginal(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_CrawlArchiveView)
    @Order(20)
    boolean isCrawlArchiveView();

    void setCrawlArchiveView(boolean b);

    @AboutConfig
    @SpinnerValidator(min = 0, max = 10, step = 1)
    @DefaultIntValue(0)
    @DescriptionForConfigEntry(text_BookImageQuality)
    @Order(30)
    int getBookImageQuality();

    void setBookImageQuality(int scaleFactor);

    public static enum BookCrawlMode implements LabelInterface {
        AUTO {
            @Override
            public String getLabel() {
                return "Original files if possible else lose book pages";
            }
        },
        ORIGINAL_AND_LOSE_PAGES {
            @Override
            public String getLabel() {
                return "Original files if possible and lose book pages";
            }
        },
        LOSE_PAGES {
            @Override
            public String getLabel() {
                return "Only lose book pages";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("AUTO")
    @Order(40)
    @DescriptionForConfigEntry(text_BookCrawlMode)
    BookCrawlMode getBookCrawlMode();

    void setBookCrawlMode(final BookCrawlMode bookCrawlerMode);
}