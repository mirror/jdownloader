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
    final String                                    text_EnforceLoginIfAccountIsAvailable       = "Only for debugging purposes: Enforce login if account is available?";
    final String                                    text_GlobalRequestIntervalLimitMilliseconds = "Define global request limit for domain 'instagram.com' in milliseconds (0 = no limit)";
    public static final InstagramConfig.TRANSLATION TRANSLATION                                 = new TRANSLATION();

    public static class TRANSLATION {
        public String getPostCrawlerAddPostDescriptionAsTextfile_label() {
            return "Post crawler: Add post description as textfile?";
        }

        public String getPostCrawlerPackagenameSchemeType_label() {
            return "Post crawler: Select package name scheme type for instagram.com/p/<id> URLs";
        }

        public String getPostCrawlerPackagenameScheme_label() {
            return "Post crawler: Enter custom package name scheme for instagram.com/p/<id>";
        }

        public String getStoryPackagenameSchemeType_label() {
            return "Story crawler: Select package name scheme type for instagram.com/stories/username/123456789..../ URLs";
        }

        public String getStoryPackagenameScheme_label() {
            return "Story crawler: Enter custom package name scheme for instagram.com/stories/username/123456789..../";
        }

        public String getStoriesHighlightsPackagenameSchemeType_label() {
            return "Story highlights crawler: Select package name scheme type for instagram.com/stories/highlights/123456789..../ URLs";
        }

        public String getStoriesHighlightsPackagenameScheme_label() {
            return "Story highlights crawler: Enter custom package name scheme for instagram.com/stories/highlights/123456789..../";
        }

        public String getFilenameType_label() {
            return "Select filename type for all crawled media items";
        }

        public String getFilenameScheme_label() {
            return "Custom filenames: Enter filename scheme";
        }

        public String getAddDateToFilenames_label() {
            return "Default filenames: Include date (yyyy-MM-dd) in filenames?";
        }

        public String getAddOrderidToFilenames_label() {
            return "Default filenames: Include order-ID in filenames if an album contains more than one element?\r\nCan be useful to keep the original order of multiple elements of an album/story.";
        }

        public String getAddShortcodeToFilenames_label() {
            return "Default filenames: Include 'shortcode' in filenames if it is available?";
        }

        public String getMediaQualityDownloadMode_label() {
            return "Select media quality download mode.\r\nOriginal quality = bigger filesize, without image-effects, works only when an account is available.";
        }

        public String getProfileCrawlerMaxItemsLimit_label() {
            return "Profile crawler: Only grab X latest posts? [0 = do not crawl posts, -1 = crawl all posts]";
        }

        public String getProfileCrawlerCrawlStory_label() {
            return "Profile crawler: Crawl story?";
        }

        public String getProfileCrawlerCrawlStoryHighlights_label() {
            return "Profile crawler: Crawl story highlights?";
        }

        public String getProfileCrawlerCrawlProfilePicture_label() {
            return "Profile crawler: Crawl profile picture?";
        }

        public String getProfileTaggedCrawledMaxItemsLimit_label() {
            return "Tagged profile crawler: How many items shall be grabbed (applies for '/profile/tagged/')? [0 = disable tagged profile crawler]";
        }

        public String getHashtagCrawlerMaxItemsLimit_label() {
            return "Hashtag crawler: How many items shall be grabbed (applies for '/explore/tags/example')? [0 = disable hashtag crawler]";
        }

        public String getCrawlerAbortOnRateLimitReached_label() {
            return "Crawler: Abort crawl process once rate limit is reached?";
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
    @DescriptionForConfigEntry("Post crawler: Add post description as textfile?")
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
    @DescriptionForConfigEntry("Post crawler: Select package name scheme type for instagram.com/p/<id> URLs")
    SinglePostPackagenameSchemeType getPostCrawlerPackagenameSchemeType();

    void setPostCrawlerPackagenameSchemeType(final SinglePostPackagenameSchemeType namingSchemeType);

    @AboutConfig
    @DefaultStringValue("*date*_*uploader* - *main_content_id*")
    @DescriptionForConfigEntry("Post crawler: Enter custom package name scheme for instagram.com/p/<id>")
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
    @DescriptionForConfigEntry("Story crawler: Select package name scheme type for instagram.com/stories/username/123456789..../ URLs")
    StoryPackagenameSchemeType getStoryPackagenameSchemeType();

    void setStoryPackagenameSchemeType(final StoryPackagenameSchemeType namingSchemeType);

    @AboutConfig
    @DefaultStringValue("*date*_*uploader*")
    @DescriptionForConfigEntry("Story crawler: Enter custom package name scheme for instagram.com/stories/username/123456789..../")
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
    @DescriptionForConfigEntry("Story highlights crawler: Select package name scheme type for instagram.com/stories/highlights/123456789..../ URLs")
    StoriesHighlightsPackagenameSchemeType getStoriesHighlightsPackagenameSchemeType();

    void setStoriesHighlightsPackagenameSchemeType(final StoriesHighlightsPackagenameSchemeType namingSchemeType);

    @AboutConfig
    @DefaultStringValue("*date*_*uploader* - *title*")
    @DescriptionForConfigEntry("Story highlights crawler: Enter custom package name scheme for instagram.com/stories/highlights/123456789..../")
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
    @DescriptionForConfigEntry("Select filename type for all crawled media items")
    FilenameType getFilenameType();

    void setFilenameType(final FilenameType filenameNamingSchemeType);

    @AboutConfig
    @DefaultStringValue("*date*_*uploader* - *main_content_id* *orderid*_of_*orderid_max* - *shortcode**ext*")
    @DescriptionForConfigEntry("Custom filenames: Enter filename scheme")
    @Order(15)
    String getFilenameScheme();

    void setFilenameScheme(String str);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("ADD_DATE_TO_FILENAMES")
    @DescriptionForConfigEntry("Default filenames: Include date (yyyy-MM-dd) in filenames?")
    @Order(20)
    boolean isAddDateToFilenames();

    void setAddDateToFilenames(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("ADD_ORDERID_TO_FILENAMES")
    @DescriptionForConfigEntry("Default filenames: Include order-ID in filenames if an album contains more than one element?\r\nCan be useful to keep the original order of multiple elements of an album/story.")
    @Order(30)
    boolean isAddOrderidToFilenames();

    void setAddOrderidToFilenames(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("ADD_SHORTCODE_TO_FILENAMES")
    @DescriptionForConfigEntry("Default filenames: Include 'shortcode' in filenames if it is available?")
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
                return "Enforce original quality (account required, on failure = display error)";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DEFAULT_QUALITY")
    @Order(50)
    @DescriptionForConfigEntry("Select media quality download mode.\r\nOriginal quality = bigger filesize, without image-effects, works only when an account is available.")
    @DefaultOnNull
    MediaQualityDownloadMode getMediaQualityDownloadMode();

    void setMediaQualityDownloadMode(final MediaQualityDownloadMode mediaQualityDownloadMode);

    @AboutConfig
    @SpinnerValidator(min = -1, max = 1024, step = 1)
    @DefaultIntValue(-1)
    @DescriptionForConfigEntry("Profile crawler: Only grab X latest posts? [0 = do not crawl posts, -1 = crawl all posts]")
    @Order(70)
    int getProfileCrawlerMaxItemsLimit();

    void setProfileCrawlerMaxItemsLimit(int items);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Profile crawler: Crawl story?")
    @Order(71)
    boolean isProfileCrawlerCrawlStory();

    void setProfileCrawlerCrawlStory(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Profile crawler: Crawl story highlights?")
    @Order(72)
    boolean isProfileCrawlerCrawlStoryHighlights();

    void setProfileCrawlerCrawlStoryHighlights(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Profile crawler: Crawl profile picture?")
    @Order(73)
    boolean isProfileCrawlerCrawlProfilePicture();

    void setProfileCrawlerCrawlProfilePicture(boolean b);

    @AboutConfig
    @SpinnerValidator(min = 0, max = 10000, step = 25)
    @DefaultIntValue(25)
    @DescriptionForConfigEntry("Tagged profile crawler: How many items shall be grabbed (applies for '/profile/tagged/')? [0 = disable tagged profile crawler]")
    @Order(85)
    int getProfileTaggedCrawledMaxItemsLimit();

    void setProfileTaggedCrawledMaxItemsLimit(int items);

    @AboutConfig
    @SpinnerValidator(min = 0, max = 10000, step = 25)
    @DefaultIntValue(25)
    @TakeValueFromSubconfig("ONLY_GRAB_X_ITEMS_HASHTAG_CRAWLER_NUMBER")
    @DescriptionForConfigEntry("Hashtag crawler: How many items shall be grabbed (applies for '/explore/tags/example')? [0 = disable hashtag crawler]")
    @Order(90)
    int getHashtagCrawlerMaxItemsLimit();

    void setHashtagCrawlerMaxItemsLimit(int items);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("QUIT_ON_RATE_LIMIT_REACHED")
    @DescriptionForConfigEntry("Crawler: Abort crawl process once rate limit is reached?")
    @Order(500)
    boolean isCrawlerAbortOnRateLimitReached();

    void setCrawlerAbortOnRateLimitReached(boolean b);

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