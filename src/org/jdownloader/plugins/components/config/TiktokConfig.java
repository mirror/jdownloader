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
    final String                                 text_EnableFastLinkcheck                                   = "Enable fast linkcheck? If enabled, filenames may contain less information in website mode and filesize might be missing until download is started.";
    final String                                 text_MaxSimultaneousDownloads                              = "Set max. simultaneous downloads. The higher the value the higher is the chance that your IP gets blocked by tiktok!";
    final String                                 text_AddDummyURLProfileCrawlerWebsiteModeMissingPagination = "Profile crawler website mode: Add dummy URL when user profile is crawled in website mode and crawler fails to find all items due to missing pagination?";
    final String                                 text_DownloadMode                                          = "Select download mode";
    final String                                 text_CrawlMode                                             = "Select profile crawl mode";
    final String                                 text_ProfileCrawlerMaxItemsLimit                           = "Profile crawler: Define max number of items to be fetched: 0 = disable profile crawler, -1 = fetch all items";
    final String                                 text_TagCrawlerMaxItemsLimit                               = "Tag crawler: Define max number of items to be fetched: 0 = disable tag crawler, -1 = fetch all items";
    final String                                 text_VideoCrawlerCrawlAudioSeparately                      = "Video crawler: Crawl audio separately?";
    final String                                 text_ImageCrawlerCrawlImagesWithoutWatermark               = "Image crawler: Crawl images without watermark?";
    final String                                 text_PreferredImageFormat                                  = "Image crawler: Choose preferred image format";

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
            return text_DownloadMode;
        }

        public String getCrawlMode_label() {
            return text_CrawlMode;
        }

        public String getProfileCrawlerMaxItemsLimit_label() {
            return text_ProfileCrawlerMaxItemsLimit;
        }

        public String getTagCrawlerMaxItemsLimit_label() {
            return text_TagCrawlerMaxItemsLimit;
        }

        public String getVideoCrawlerCrawlAudioSeparately_label() {
            return text_VideoCrawlerCrawlAudioSeparately;
        }

        public String getImageCrawlerCrawlImagesWithoutWatermark_label() {
            return text_ImageCrawlerCrawlImagesWithoutWatermark;
        }

        public String getPreferredImageFormat_label() {
            return text_PreferredImageFormat;
        }
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
                return "Website [Usually with watermark, also private videos]";
            }
        },
        API {
            @Override
            public String getLabel() {
                return "API [Usually without watermark, only public videos]";
            }
        },
        API_HD {
            @Override
            public String getLabel() {
                return "API HD [Usually without watermark, only public videos]";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("API")
    @Order(40)
    @DescriptionForConfigEntry(text_DownloadMode)
    DownloadMode getDownloadMode();

    void setDownloadMode(final DownloadMode mode);

    public static enum CrawlMode implements LabelInterface {
        WEBSITE {
            @Override
            public String getLabel() {
                return "Website [Max first ~30 items, also private profiles]";
            }
        },
        API {
            @Override
            public String getLabel() {
                return "API [All items, only public profiles]";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("API")
    @Order(50)
    @DescriptionForConfigEntry(text_CrawlMode)
    CrawlMode getCrawlMode();

    void setCrawlMode(final CrawlMode mode);

    @AboutConfig
    @SpinnerValidator(min = -1, max = 10000, step = 25)
    @DefaultIntValue(-1)
    @DescriptionForConfigEntry(text_ProfileCrawlerMaxItemsLimit)
    @Order(60)
    int getProfileCrawlerMaxItemsLimit();

    void setProfileCrawlerMaxItemsLimit(int items);

    @AboutConfig
    @SpinnerValidator(min = -1, max = 10000, step = 20)
    @DefaultIntValue(20)
    @DescriptionForConfigEntry(text_TagCrawlerMaxItemsLimit)
    @Order(70)
    int getTagCrawlerMaxItemsLimit();

    void setTagCrawlerMaxItemsLimit(int items);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_EnableFastLinkcheck)
    @Order(80)
    boolean isVideoCrawlerCrawlAudioSeparately();

    void setVideoCrawlerCrawlAudioSeparately(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_ImageCrawlerCrawlImagesWithoutWatermark)
    @Order(90)
    boolean isImageCrawlerCrawlImagesWithoutWatermark();

    void setImageCrawlerCrawlImagesWithoutWatermark(boolean b);

    public static enum ImageFormat implements LabelInterface {
        JPEG {
            @Override
            public String getLabel() {
                return "jpeg";
            }
        },
        WEBP {
            @Override
            public String getLabel() {
                return "webp";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("JPEG")
    @Order(91)
    @DescriptionForConfigEntry(text_PreferredImageFormat)
    ImageFormat getPreferredImageFormat();

    void setPreferredImageFormat(final ImageFormat format);
}