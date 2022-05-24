package org.jdownloader.plugins.components.config;

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

@PluginHost(host = "tiktok.com", type = Type.HOSTER)
public interface TiktokConfig extends PluginConfigInterface {
    public static final TiktokConfig.TRANSLATION TRANSLATION                                                = new TRANSLATION();
    final String                                 text_EnableFastLinkcheck                                   = "Enable fast linkcheck? If enabled, filenames may contain less information and filesize will be missing until download is started.";
    final String                                 text_MaxSimultaneousDownloads                              = "Set max. simultaneous downloads. The higher the value the higher is the chance that your IP gets blocked by tiktok!";
    final String                                 text_AddDummyURLProfileCrawlerWebsiteModeMissingPagination = "Add dummy URL when user profile is crawled in website mode and crawler fails to find all items due to missing pagination?";
    final String                                 text_getDownloadMode                                       = "Select download mode";
    final String                                 text_getCrawlMode                                          = "Select crawl mode";

    public static class TRANSLATION {
        public String getEnableFastLinkcheck_label() {
            return text_EnableFastLinkcheck;
        }

        public String getMaxSimultaneousDownloads_label() {
            return text_MaxSimultaneousDownloads;
        }

        public String getAddDummyURLProfileCrawlerWebsiteModeMissingPagination_label() {
            return text_AddDummyURLProfileCrawlerWebsiteModeMissingPagination;
        }

        public String getDownloadMode_label() {
            return text_getDownloadMode;
        }
        // public String getCrawlMode_label() {
        // return text_getCrawlMode;
        // }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_EnableFastLinkcheck)
    @Order(10)
    boolean isEnableFastLinkcheck();

    void setEnableFastLinkcheck(boolean b);

    @AboutConfig
    @DefaultIntValue(1)
    @SpinnerValidator(min = 1, max = 20, step = 1)
    @Order(20)
    @DescriptionForConfigEntry(text_MaxSimultaneousDownloads)
    int getMaxSimultaneousDownloads();

    void setMaxSimultaneousDownloads(int i);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_AddDummyURLProfileCrawlerWebsiteModeMissingPagination)
    @Order(30)
    boolean isAddDummyURLProfileCrawlerWebsiteModeMissingPagination();

    void setAddDummyURLProfileCrawlerWebsiteModeMissingPagination(boolean b);

    public static enum DownloadMode implements LabelInterface {
        WEBSITE {
            @Override
            public String getLabel() {
                return "Website [With watermark]";
            }
        },
        API {
            @Override
            public String getLabel() {
                return "API [Without watermark]";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("WEBSITE")
    @Order(40)
    @DescriptionForConfigEntry(text_getDownloadMode)
    DownloadMode getDownloadMode();

    void setDownloadMode(final DownloadMode mode);
    // public static enum CrawlMode implements LabelInterface {
    // WEBSITE {
    // @Override
    // public String getLabel() {
    // return "Website [Max first ~30 items]";
    // }
    // },
    // API {
    // @Override
    // public String getLabel() {
    // return "API [All items]";
    // }
    // };
    // }
    //
    // @AboutConfig
    // @DefaultEnumValue("API")
    // @Order(50)
    // @DescriptionForConfigEntry(text_getCrawlMode)
    // CrawlMode getCrawlerMode();
    //
    // void setCrawlerMode(final CrawlMode mode);
}