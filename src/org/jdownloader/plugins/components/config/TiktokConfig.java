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
    final String                                 text_MediaCrawlMode                                        = "Media crawl mode";
    final String                                 text_ProfileCrawlMode                                      = "Profile crawl mode";
    final String                                 text_AddDummyURLProfileCrawlerWebsiteModeMissingPagination = "Profile crawler website mode: Add dummy URL when user profile is crawled and crawler fails to find all items due to missing pagination support?";
    final String                                 text_ProfileCrawlerMaxItemsLimit                           = "Profile crawler: Define max number of items to be fetched: 0 = disable profile crawler, -1 = fetch all items";
    final String                                 text_TagCrawlerMaxItemsLimit                               = "Tag crawler: Define max number of items to be fetched: 0 = disable tag crawler, -1 = fetch all items";
    final String                                 text_VideoCrawlerCrawlAudioSeparately                      = "Video crawler: Crawl soundtrack separately?";
    final String                                 text_ImageCrawlerCrawlImagesWithoutWatermark               = "Image crawler: Crawl images without watermark?";
    final String                                 text_PreferredImageFormat                                  = "Image crawler: Choose preferred image format";

    public static class TRANSLATION {
        public String getEnableFastLinkcheck_label() {
            return text_EnableFastLinkcheck;
        }

        public String getMaxSimultaneousDownloads_label() {
            return text_MaxSimultaneousDownloads;
        }

        public String getMediaCrawlModeV2_label() {
            return text_MediaCrawlMode;
        }

        public String getProfileCrawlMode_label() {
            return text_ProfileCrawlMode;
        }

        public String getAddDummyURLProfileCrawlerWebsiteModeMissingPagination_label() {
            return text_AddDummyURLProfileCrawlerWebsiteModeMissingPagination;
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

    final MediaCrawlMode defaultMediaCrawlMode = MediaCrawlMode.AUTO;

    public static enum MediaCrawlMode implements LabelInterface {
        DEFAULT {
            @Override
            public String getLabel() {
                return "Default: " + defaultMediaCrawlMode.getLabel();
            }
        },
        AUTO {
            @Override
            public String getLabel() {
                return "Auto";
            }
        },
        WEBSITE {
            @Override
            public String getLabel() {
                return "Website [Usually without watermark, also private videos if account is given]";
            }
        },
        WEBSITE_EMBED {
            @Override
            public String getLabel() {
                return "Website embed [Usually with watermark, also private videos if account is given]";
            }
        },
        API {
            @Override
            public String getLabel() {
                return "[!BROKEN!] API [Without watermark, only public videos]";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("API")
    @Order(40)
    @DescriptionForConfigEntry(text_MediaCrawlMode)
    MediaCrawlMode getMediaCrawlModeV2();

    void setMediaCrawlModeV2(final MediaCrawlMode mode);

    public static enum ProfileCrawlMode implements LabelInterface {
        WEBSITE {
            @Override
            public String getLabel() {
                return "[!BROKEN!] Website [Max first ~30 items, also private profiles]";
            }
        },
        API {
            @Override
            public String getLabel() {
                return "[!BROKEN!] API [All items, only public profiles]";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("API")
    @Order(50)
    @DescriptionForConfigEntry(text_ProfileCrawlMode)
    ProfileCrawlMode getProfileCrawlMode();

    void setProfileCrawlMode(final ProfileCrawlMode mode);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_AddDummyURLProfileCrawlerWebsiteModeMissingPagination)
    @Order(55)
    boolean isAddDummyURLProfileCrawlerWebsiteModeMissingPagination();

    void setAddDummyURLProfileCrawlerWebsiteModeMissingPagination(boolean b);

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
    @DescriptionForConfigEntry(text_VideoCrawlerCrawlAudioSeparately)
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