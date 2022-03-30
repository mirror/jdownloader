package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
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
        public String getAddPostDescriptionAsTextfile_label() {
            return "Add post description as textfile?";
        }

        public String getPreferServerFilenames_label() {
            return "Use server-filenames whenever possible?";
        }

        public String getAddDateToFilenames_label() {
            return "Include date (yyyy-MM-dd) in filenames?";
        }

        public String getAddOrderidToFilenames_label() {
            return "Include order-ID in filenames if an album contains more than one element?\r\nCan be useful to keep the original order of multiple elements of an album/story.";
        }

        public String getAddShortcodeToFilenames_label() {
            return "Include 'shortcode' in filenames if it is available?";
        }

        public String getAttemptToDownloadOriginalQuality_label() {
            return "Try to download original quality (bigger filesize, without image-effects)? [This will slow down the download-process!]";
        }

        public String getHashtagCrawlerFindUsernames_label() {
            return "Crawl- and set usernames for filenames when crawling '/explore/tags/<hashtag>' URLs? (slows down crawl-process!)";
        }

        public String getProfileCrawlerMaxItemsLimit_label() {
            return "Profile crawler: Only grab X latest items? [0 = disable profile crawler, -1 = unlimited]";
        }

        public String getProfileCrawlerPreferAlternativeAPI_label() {
            return "Profile crawler: Use alternative API? Can be slower, only works when an Instagram account is active and doesn't crawl reposts!";
        }

        public String getHashtagCrawlerMaxItemsLimit_label() {
            return "Hashtag crawler: How many items shall be grabbed (applies for '/explore/tags/example')? [0 = disable hashtag crawling]";
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
    @DescriptionForConfigEntry("Add post description as textfile?")
    @Order(5)
    boolean isAddPostDescriptionAsTextfile();

    void setAddPostDescriptionAsTextfile(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("PREFER_SERVER_FILENAMES")
    @DescriptionForConfigEntry("Use server-filenames whenever possible?")
    @Order(10)
    boolean isPreferServerFilenames();

    void setPreferServerFilenames(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("ADD_DATE_TO_FILENAMES")
    @DescriptionForConfigEntry("Include date (yyyy-MM-dd) in filenames?")
    @Order(20)
    boolean isAddDateToFilenames();

    void setAddDateToFilenames(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("ADD_ORDERID_TO_FILENAMES")
    @DescriptionForConfigEntry("Include order-ID in filenames if an album contains more than one element?\r\nCan be useful to keep the original order of multiple elements of an album/story.")
    @Order(30)
    boolean isAddOrderidToFilenames();

    void setAddOrderidToFilenames(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("ADD_SHORTCODE_TO_FILENAMES")
    @DescriptionForConfigEntry("Include 'shortcode' in filenames if it is available?")
    @Order(40)
    boolean isAddShortcodeToFilenames();

    void setAddShortcodeToFilenames(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("ATTEMPT_TO_DOWNLOAD_ORIGINAL_QUALITY")
    @DescriptionForConfigEntry("Try to download original quality (bigger filesize, without image-effects)? [This will slow down the download-process!]")
    @Order(50)
    boolean isAttemptToDownloadOriginalQuality();

    void setAttemptToDownloadOriginalQuality(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("HASHTAG_CRAWLER_FIND_USERNAMES")
    @DescriptionForConfigEntry("Crawl- and set usernames for filenames when crawling '/explore/tags/<hashtag>' URLs? (slows down crawl-process!)")
    @Order(60)
    boolean isHashtagCrawlerFindUsernames();

    void setHashtagCrawlerFindUsernames(boolean b);

    @AboutConfig
    @SpinnerValidator(min = -1, max = 1024, step = 1)
    @DefaultIntValue(-1)
    @DescriptionForConfigEntry("Profile crawler: Only grab X latest items? [0 = disable profile crawler, -1 = unlimited]")
    @Order(70)
    int getProfileCrawlerMaxItemsLimit();

    void setProfileCrawlerMaxItemsLimit(int items);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("PROFILE_CRAWLER_PREFER_ALTERNATIVE_API")
    @DescriptionForConfigEntry("Profile crawler: Use alternative API? Can be slower, only works when an Instagram account is active and doesn't crawl reposts!")
    @Order(80)
    boolean isProfileCrawlerPreferAlternativeAPI();

    void setProfileCrawlerPreferAlternativeAPI(boolean b);

    @AboutConfig
    @SpinnerValidator(min = 0, max = 10000, step = 25)
    @DefaultIntValue(25)
    @TakeValueFromSubconfig("ONLY_GRAB_X_ITEMS_HASHTAG_CRAWLER_NUMBER")
    @DescriptionForConfigEntry("Hashtag crawler: How many items shall be grabbed (applies for '/explore/tags/example')? [0 = disable hashtag crawling]")
    @Order(90)
    int getHashtagCrawlerMaxItemsLimit();

    void setHashtagCrawlerMaxItemsLimit(int items);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("QUIT_ON_RATE_LIMIT_REACHED")
    @DescriptionForConfigEntry("Crawler: Abort crawl process once rate limit is reached?")
    @Order(100)
    boolean isCrawlerAbortOnRateLimitReached();

    void setCrawlerAbortOnRateLimitReached(boolean b);

    @AboutConfig
    @SpinnerValidator(min = 0, max = 60000, step = 100)
    @DefaultIntValue(400)
    @DescriptionForConfigEntry("Define global request limit in milliseconds (0 = no limit)")
    @Order(110)
    int getGlobalRequestIntervalLimitMilliseconds();

    void setGlobalRequestIntervalLimitMilliseconds(int milliseconds);
}