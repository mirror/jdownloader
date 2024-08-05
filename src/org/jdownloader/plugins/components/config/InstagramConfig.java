package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultOnNull;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.TakeValueFromSubconfig;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "instagram.com", type = Type.HOSTER)
public interface InstagramConfig extends PluginConfigInterface {
    final String                    text_PostCrawlerAddPostDescriptionAsTextfile      = "Post crawler: Add post description as textfile?";
    final String                    text_PostCrawlerPackagenameSchemeType             = "Post crawler: Package name scheme type for instagram.com/p/<id>";
    final String                    text_PostCrawlerPackagenameScheme                 = "Post crawler: Custom package name scheme for instagram.com/p/<id>";
    final String                    text_StoryPackagenameSchemeType                   = "Story crawler: Package name scheme type for instagram.com/stories/username/<storyID>/ URLs";
    final String                    text_StoryPackagenameScheme                       = "Story crawler: Custom package name scheme for instagram.com/stories/username/<storyID>/";
    final String                    text_StoriesHighlightsPackagenameSchemeType       = "Story highlights crawler: Package name scheme type for instagram.com/stories/highlights/<storyID>/";
    final String                    text_StoriesHighlightsPackagenameScheme           = "Story highlights crawler: Custom package name scheme for instagram.com/stories/highlights/<storyID>/";
    final String                    text_FilenameType                                 = "Filename type for all crawled media items";
    final String                    text_FilenameScheme                               = "Custom filenames: Filename scheme";
    final String                    text_AddDateToFilenames                           = "Default filenames: Include date (yyyy-MM-dd) in filenames?";
    final String                    text_AddOrderidToFilenames                        = "Default filenames: Include 'order-ID' in filenames if an album contains more than one element?";
    final String                    text_AddShortcodeToFilenames                      = "Default filenames: Include 'shortcode' in filenames if it is available?";
    final String                    text_MediaQualityDownloadMode                     = "Media quality download mode.\r\nOriginal quality = bigger filesize, without image-effects, works only when an account is available.";
    final String                    text_ProfileCrawlerMaxItemsLimit                  = "Profile crawler: Only grab X latest posts? [0 = disable, -1 = crawl all]";
    final String                    text_ProfileCrawlerCrawlStory                     = "Profile crawler: Crawl story?";
    final String                    text_ProfileCrawlerCrawlStoryHighlights           = "Profile crawler: Crawl story highlights?";
    final String                    text_ProfileCrawlerCrawlProfilePicture            = "Profile crawler: Crawl profile picture?";
    final String                    text_ProfileCrawlerReelsPaginationMaxItemsPerPage = "Profile reels crawler: Max items per pagination (higher value = faster crawl process, can result in account ban!)";
    final String                    text_ProfileTaggedCrawledMaxItemsLimit            = "Tagged profile crawler: How many items shall be grabbed for instagram.com/profile/tagged/? [0 = disable tagged profile crawler]";
    final String                    text_HashtagCrawlerMaxItemsLimit                  = "Hashtag crawler: How many items shall be grabbed for instagram.com/explore/tags/<tagName>? [0 = disable]";
    final String                    text_ActionOnRateLimitReached                     = "Crawler: Action on rate limit reached";
    final String                    text_GlobalRequestIntervalLimitMilliseconds       = "Define global request limit for domains 'instagram.com' and 'cdninstagram.com' in milliseconds [0 = no limit]";
    final String                    text_EnforceLoginIfAccountIsAvailable             = "Debug: Enforce login if account is available?";
    public static final TRANSLATION TRANSLATION                                       = new TRANSLATION();

    public static class TRANSLATION {
        public String getPostCrawlerAddPostDescriptionAsTextfile_label() {
            return text_PostCrawlerAddPostDescriptionAsTextfile;
        }

        public String getPostCrawlerPackagenameSchemeType_label() {
            return text_PostCrawlerPackagenameSchemeType;
        }

        public String getPostCrawlerPackagenameScheme_label() {
            return text_PostCrawlerPackagenameScheme;
        }

        public String getStoryPackagenameSchemeType_label() {
            return text_StoryPackagenameSchemeType;
        }

        public String getStoryPackagenameScheme_label() {
            return text_StoryPackagenameScheme;
        }

        public String getStoriesHighlightsPackagenameSchemeType_label() {
            return text_StoriesHighlightsPackagenameSchemeType;
        }

        public String getStoriesHighlightsPackagenameScheme_label() {
            return text_StoriesHighlightsPackagenameScheme;
        }

        public String getFilenameType_label() {
            return text_FilenameType;
        }

        public String getFilenameScheme_label() {
            return text_FilenameScheme;
        }

        public String getAddDateToFilenames_label() {
            return text_AddDateToFilenames;
        }

        public String getAddOrderidToFilenames_label() {
            return text_AddOrderidToFilenames;
        }

        public String getAddShortcodeToFilenames_label() {
            return text_AddShortcodeToFilenames;
        }

        public String getMediaQualityDownloadMode_label() {
            return text_MediaQualityDownloadMode;
        }

        public String getProfileCrawlerMaxItemsLimit_label() {
            return text_ProfileCrawlerMaxItemsLimit;
        }

        public String getProfileCrawlerCrawlStory_label() {
            return text_ProfileCrawlerCrawlStory;
        }

        public String getProfileCrawlerCrawlStoryHighlights_label() {
            return text_ProfileCrawlerCrawlStoryHighlights;
        }

        public String getProfileCrawlerCrawlProfilePicture_label() {
            return text_ProfileCrawlerCrawlProfilePicture;
        }

        public String getProfileCrawlerReelsPaginationMaxItemsPerPage_label() {
            return text_ProfileCrawlerReelsPaginationMaxItemsPerPage;
        }

        public String getProfileTaggedCrawledMaxItemsLimit_label() {
            return text_ProfileTaggedCrawledMaxItemsLimit;
        }

        public String getHashtagCrawlerMaxItemsLimit_label() {
            return text_HashtagCrawlerMaxItemsLimit;
        }

        public String getActionOnRateLimitReached_label() {
            return text_ActionOnRateLimitReached;
        }

        public String getGlobalRequestIntervalLimitMilliseconds_label() {
            return text_GlobalRequestIntervalLimitMilliseconds;
        }

        public String getEnforceLoginIfAccountIsAvailable_label() {
            return text_EnforceLoginIfAccountIsAvailable;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_PostCrawlerAddPostDescriptionAsTextfile)
    @Order(1)
    boolean isPostCrawlerAddPostDescriptionAsTextfile();

    void setPostCrawlerAddPostDescriptionAsTextfile(boolean b);

    public static enum SinglePostPackagenameSchemeType implements LabelInterface {
        UPLOADER {
            @Override
            public String getLabel() {
                return "*uploader*";
            }
        },
        UPLOADER_MAIN_CONTENT_ID {
            @Override
            public String getLabel() {
                return "*uploader* - *main_content_id*";
            }
        },
        CUSTOM {
            @Override
            public String getLabel() {
                return "Custom";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("UPLOADER")
    @Order(2)
    @DescriptionForConfigEntry(text_PostCrawlerPackagenameSchemeType)
    SinglePostPackagenameSchemeType getPostCrawlerPackagenameSchemeType();

    void setPostCrawlerPackagenameSchemeType(final SinglePostPackagenameSchemeType namingSchemeType);

    @AboutConfig
    @DefaultStringValue("*date*_*uploader* - *main_content_id*")
    @DescriptionForConfigEntry(text_PostCrawlerPackagenameScheme)
    @Order(3)
    String getPostCrawlerPackagenameScheme();

    void setPostCrawlerPackagenameScheme(String str);

    public static enum StoryPackagenameSchemeType implements LabelInterface {
        DEFAULT_1 {
            @Override
            public String getLabel() {
                return "story - *uploader*";
            }
        },
        CUSTOM {
            @Override
            public String getLabel() {
                return "Custom";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DEFAULT_1")
    @Order(4)
    @DescriptionForConfigEntry(text_StoryPackagenameSchemeType)
    StoryPackagenameSchemeType getStoryPackagenameSchemeType();

    void setStoryPackagenameSchemeType(final StoryPackagenameSchemeType namingSchemeType);

    @AboutConfig
    @DefaultStringValue("*date*_*uploader*")
    @DescriptionForConfigEntry(text_StoryPackagenameScheme)
    @Order(5)
    String getStoryPackagenameScheme();

    void setStoryPackagenameScheme(String str);

    public static enum StoriesHighlightsPackagenameSchemeType implements LabelInterface {
        DEFAULT_1 {
            @Override
            public String getLabel() {
                return "story highlights - *uploader* - *title*";
            }
        },
        CUSTOM {
            @Override
            public String getLabel() {
                return "Custom";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DEFAULT_1")
    @Order(6)
    @DescriptionForConfigEntry(text_StoriesHighlightsPackagenameSchemeType)
    StoriesHighlightsPackagenameSchemeType getStoriesHighlightsPackagenameSchemeType();

    void setStoriesHighlightsPackagenameSchemeType(final StoriesHighlightsPackagenameSchemeType namingSchemeType);

    @AboutConfig
    @DefaultStringValue("*date*_*uploader* - *title*")
    @DescriptionForConfigEntry(text_StoriesHighlightsPackagenameScheme)
    @Order(7)
    String getStoriesHighlightsPackagenameScheme();

    void setStoriesHighlightsPackagenameScheme(String str);

    public static enum FilenameType implements LabelInterface {
        DEFAULT {
            @Override
            public String getLabel() {
                return "Default";
            }
        },
        SERVER {
            @Override
            public String getLabel() {
                return "Server filenames";
            }
        },
        CUSTOM {
            @Override
            public String getLabel() {
                return "Custom";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DEFAULT")
    @Order(10)
    @DescriptionForConfigEntry(text_FilenameType)
    FilenameType getFilenameType();

    void setFilenameType(final FilenameType filenameNamingSchemeType);

    @AboutConfig
    @DefaultStringValue("*date*_*uploader* - *main_content_id* *orderid*_of_*orderid_max* - *shortcode**ext*")
    @DescriptionForConfigEntry(text_FilenameScheme)
    @Order(15)
    String getFilenameScheme();

    void setFilenameScheme(String str);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("ADD_DATE_TO_FILENAMES")
    @DescriptionForConfigEntry(text_AddDateToFilenames)
    @Order(20)
    boolean isAddDateToFilenames();

    void setAddDateToFilenames(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("ADD_ORDERID_TO_FILENAMES")
    @DescriptionForConfigEntry(text_AddOrderidToFilenames)
    @Order(30)
    boolean isAddOrderidToFilenames();

    void setAddOrderidToFilenames(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("ADD_SHORTCODE_TO_FILENAMES")
    @DescriptionForConfigEntry(text_AddShortcodeToFilenames)
    @Order(40)
    boolean isAddShortcodeToFilenames();

    void setAddShortcodeToFilenames(boolean b);

    public static enum MediaQualityDownloadMode implements LabelInterface {
        DEFAULT_QUALITY {
            @Override
            public String getLabel() {
                return "Default Instagram quality";
            }
        },
        PREFER_ORIGINAL_QUALITY {
            @Override
            public String getLabel() {
                return "Prefer original quality (account required, on failure = fallback to default quality)";
            }
        },
        ENFORCE_ORIGINAL_QUALITY {
            @Override
            public String getLabel() {
                return "Enforce original quality (account required, on failure = display error message)";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DEFAULT_QUALITY")
    @Order(50)
    @DescriptionForConfigEntry(text_MediaQualityDownloadMode)
    @DefaultOnNull
    MediaQualityDownloadMode getMediaQualityDownloadMode();

    void setMediaQualityDownloadMode(final MediaQualityDownloadMode mediaQualityDownloadMode);

    @AboutConfig
    @SpinnerValidator(min = -1, max = 1024, step = 1)
    @DefaultIntValue(-1)
    @DescriptionForConfigEntry(text_ProfileCrawlerMaxItemsLimit)
    @Order(70)
    int getProfileCrawlerMaxItemsLimit();

    void setProfileCrawlerMaxItemsLimit(int items);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_ProfileCrawlerCrawlStory)
    @Order(71)
    boolean isProfileCrawlerCrawlStory();

    void setProfileCrawlerCrawlStory(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_ProfileCrawlerCrawlStoryHighlights)
    @Order(72)
    boolean isProfileCrawlerCrawlStoryHighlights();

    void setProfileCrawlerCrawlStoryHighlights(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_ProfileCrawlerCrawlProfilePicture)
    @Order(73)
    boolean isProfileCrawlerCrawlProfilePicture();

    void setProfileCrawlerCrawlProfilePicture(boolean b);

    @AboutConfig
    @SpinnerValidator(min = 1, max = 100, step = 1)
    @DefaultIntValue(12)
    @DescriptionForConfigEntry(text_ProfileCrawlerReelsPaginationMaxItemsPerPage)
    @Order(85)
    int getProfileCrawlerReelsPaginationMaxItemsPerPage();

    void setProfileCrawlerReelsPaginationMaxItemsPerPage(int items);

    @AboutConfig
    @SpinnerValidator(min = 0, max = 10000, step = 25)
    @DefaultIntValue(25)
    @DescriptionForConfigEntry(text_ProfileTaggedCrawledMaxItemsLimit)
    @Order(85)
    int getProfileTaggedCrawledMaxItemsLimit();

    void setProfileTaggedCrawledMaxItemsLimit(int items);

    @AboutConfig
    @SpinnerValidator(min = 0, max = 10000, step = 25)
    @DefaultIntValue(25)
    @TakeValueFromSubconfig("ONLY_GRAB_X_ITEMS_HASHTAG_CRAWLER_NUMBER")
    @DescriptionForConfigEntry(text_HashtagCrawlerMaxItemsLimit)
    @Order(90)
    int getHashtagCrawlerMaxItemsLimit();

    void setHashtagCrawlerMaxItemsLimit(int items);

    public static enum ActionOnRateLimitReached implements LabelInterface {
        CONTINUE {
            @Override
            public String getLabel() {
                return "Wait and try again";
            }
        },
        ABORT {
            @Override
            public String getLabel() {
                return "Abort crawl process";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("CONTINUE")
    @Order(500)
    @DescriptionForConfigEntry(text_ActionOnRateLimitReached)
    @DefaultOnNull
    ActionOnRateLimitReached getActionOnRateLimitReached();

    void setActionOnRateLimitReached(final ActionOnRateLimitReached action);

    @AboutConfig
    @SpinnerValidator(min = 0, max = 60000, step = 100)
    @DefaultIntValue(400)
    @DescriptionForConfigEntry(text_GlobalRequestIntervalLimitMilliseconds)
    @Order(510)
    int getGlobalRequestIntervalLimitMilliseconds();

    void setGlobalRequestIntervalLimitMilliseconds(int milliseconds);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_EnforceLoginIfAccountIsAvailable)
    @Order(600)
    boolean isEnforceLoginIfAccountIsAvailable();

    void setEnforceLoginIfAccountIsAvailable(boolean b);
}