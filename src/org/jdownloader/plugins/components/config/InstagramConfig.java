package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
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
    public static final InstagramConfig.TRANSLATION TRANSLATION = new TRANSLATION();

    public static class TRANSLATION {
        public String getPostCrawlerAddPostDescriptionAsTextfile_label() {
            return "Post crawler: Add post description as textfile?";
        }

        public String getPostCrawlerPackagenameType_label() {
            return "Post crawler: Select package name type for instagram.com/p/<id> URLs";
        }

        public String getPostCrawlerPackagenameScheme_label() {
            return "Post crawler: Enter custom package name scheme for instagram.com/p/<id>";
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

        public String getAttemptToDownloadOriginalQuality_label() {
            return "Try to download original quality (bigger filesize, without image-effects)? [This can slow down the download-process!]";
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

        public String getProfileCrawlerPreferAlternativeAPI_label() {
            return "Profile crawler: Prefer usage of alternative API? Can be slower, only works when an Instagram account is active!";
        }

        public String getProfileTaggedCrawledMaxItemsLimit_label() {
            return "Tagged profile crawler: How many items shall be grabbed (applies for '/profile/tagged/')? [0 = disable tagged profile crawler]";
        }

        public String getHashtagCrawlerMaxItemsLimit_label() {
            return "Hashtag crawler: How many items shall be grabbed (applies for '/explore/tags/example')? [0 = disable hashtag crawler]";
        }

        public String getHashtagCrawlerUseAlternativeAPI_label() {
            return "Hashtag crawler: Use alternative API? Can be slower, only works when an Instagram account is active!";
        }

        public String getCrawlerAbortOnRateLimitReached_label() {
            return "Crawler: Abort crawl process once rate limit is reached?";
        }

        public String getGlobalRequestIntervalLimitMilliseconds_label() {
            return "Define global request limit in milliseconds (0 = no limit)";
        }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Post crawler: Add post description as textfile?")
    @Order(1)
    boolean isPostCrawlerAddPostDescriptionAsTextfile();

    void setPostCrawlerAddPostDescriptionAsTextfile(boolean b);

    public static enum SinglePostPackagenameType implements LabelInterface {
        DEFAULT {
            @Override
            public String getLabel() {
                return "Default";
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
    @Order(2)
    @DescriptionForConfigEntry("Post crawler: Select package name type for instagram.com/p/<id> URLs")
    SinglePostPackagenameType getPostCrawlerPackagenameType();

    void setPostCrawlerPackagenameType(final SinglePostPackagenameType namingSchemeType);

    @AboutConfig
    @DefaultStringValue("*date*_*uploader* - *main_content_id*")
    @DescriptionForConfigEntry("Post crawler: Enter custom package name scheme for instagram.com/p/<id>")
    @Order(3)
    String getPostCrawlerPackagenameScheme();

    void setPostCrawlerPackagenameScheme(String str);

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

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("ATTEMPT_TO_DOWNLOAD_ORIGINAL_QUALITY")
    @DescriptionForConfigEntry("Try to download original quality (bigger filesize, without image-effects)? [This can slow down the download-process!]")
    @Order(50)
    boolean isAttemptToDownloadOriginalQuality();

    void setAttemptToDownloadOriginalQuality(boolean b);

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
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("PROFILE_CRAWLER_PREFER_ALTERNATIVE_API")
    @DescriptionForConfigEntry("Profile crawler: Prefer usage of alternative API? Can be slower, only works when an Instagram account is active!")
    @Order(80)
    boolean isProfileCrawlerPreferAlternativeAPI();

    void setProfileCrawlerPreferAlternativeAPI(boolean b);

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
    @DescriptionForConfigEntry("Hashtag crawler: Use alternative API? Can be slower, only works when an Instagram account is active!")
    @Order(100)
    boolean isHashtagCrawlerUseAlternativeAPI();

    void setHashtagCrawlerUseAlternativeAPI(boolean b);

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
    @DescriptionForConfigEntry("Define global request limit in milliseconds (0 = no limit)")
    @Order(510)
    int getGlobalRequestIntervalLimitMilliseconds();

    void setGlobalRequestIntervalLimitMilliseconds(int milliseconds);
}