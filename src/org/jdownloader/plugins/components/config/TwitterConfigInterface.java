package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "twitter.com", type = Type.HOSTER)
public interface TwitterConfigInterface extends PluginConfigInterface {
    public static final TRANSLATION TRANSLATION                      = new TRANSLATION();
    final String                    text_UseOriginalFilenames        = "Use original filename instead of plugin filenames?";
    final String                    text_MarkTweetRepliesViaFilename = "Append '_reply' to filenames of tweets that are replies to other tweets?";
    final String                    text_AddTweetTextAsTextfile      = "Add tweet text as textfile?";
    final String                    text_CrawlURLsInsideTweetText    = "Crawl URLs inside post text?\r\nWarning: This may result in endless crawling activity!";
    final String                    text_CrawlRetweets               = "[BETA] Crawl retweets?";
    final String                    text_PreferHLSVideoDownload      = "Videos: Prefer HLS over http download?";

    public static class TRANSLATION {
        /* 2022-03-18: Not needed anymore for now. */
        // public String getForceGrabMediaOnlyEnabled_label() {
        // return "Force grab media? Disable this to also crawl media of retweets and other content from users' timelines (only if you
        // add URLs without '/media'!)";
        // }
        public String getUseOriginalFilenames_label() {
            return text_UseOriginalFilenames;
        }

        public String getMarkTweetRepliesViaFilename_label() {
            return text_MarkTweetRepliesViaFilename;
        }

        public String getAddTweetTextAsTextfile_label() {
            return text_AddTweetTextAsTextfile;
        }

        public String getCrawlURLsInsideTweetText_label() {
            return text_CrawlURLsInsideTweetText;
        }

        public String getCrawlRetweets_label() {
            return text_CrawlRetweets;
        }

        public String getPreferHLSVideoDownload_label() {
            return text_PreferHLSVideoDownload;
        }

        public String getGlobalRequestIntervalLimitApiTwitterComMilliseconds_label() {
            return "Define global request limit for api.twitter.com in milliseconds (0 = no limit)";
        }

        public String getGlobalRequestIntervalLimitTwimgComMilliseconds_label() {
            return "Define global request limit for twimg.com in milliseconds (0 = no limit)";
        }

        public String getProfileCrawlerWaittimeBetweenPaginationMilliseconds_label() {
            return "Profile crawler: Wait time between pagination requests in milliseconds";
        }
    }
    /* 2022-03-18: Not needed anymore for now. */
    // @DefaultBooleanValue(true)
    // @AboutConfig
    // @DescriptionForConfigEntry("Force grab media? Disable this to also crawl media of retweets and other content from users'
    // timelines (only if you add URLs without '/media'!)")
    // @Order(10)
    // boolean isForceGrabMediaOnlyEnabled();
    //
    // void setForceGrabMediaOnlyEnabled(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    @DescriptionForConfigEntry(text_UseOriginalFilenames)
    @Order(20)
    boolean isUseOriginalFilenames();

    void setUseOriginalFilenames(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry(text_MarkTweetRepliesViaFilename)
    @Order(25)
    boolean isMarkTweetRepliesViaFilename();

    void setMarkTweetRepliesViaFilename(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    @DescriptionForConfigEntry(text_AddTweetTextAsTextfile)
    @Order(30)
    boolean isAddTweetTextAsTextfile();

    void setAddTweetTextAsTextfile(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    @DescriptionForConfigEntry(text_CrawlRetweets)
    @Order(35)
    boolean isCrawlRetweets();

    void setCrawlRetweets(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry(text_CrawlURLsInsideTweetText)
    @Order(40)
    boolean isCrawlURLsInsideTweetText();

    void setCrawlURLsInsideTweetText(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry(text_PreferHLSVideoDownload)
    @Order(50)
    boolean isPreferHLSVideoDownload();

    void setPreferHLSVideoDownload(boolean b);

    @AboutConfig
    @SpinnerValidator(min = 0, max = 60000, step = 100)
    @DefaultIntValue(500)
    @DescriptionForConfigEntry("Define global request limit for api.twitter.com in milliseconds (0 = no limit)")
    @Order(60)
    int getGlobalRequestIntervalLimitApiTwitterComMilliseconds();

    void setGlobalRequestIntervalLimitApiTwitterComMilliseconds(int milliseconds);

    @AboutConfig
    @SpinnerValidator(min = 0, max = 60000, step = 100)
    @DefaultIntValue(500)
    @DescriptionForConfigEntry("Define global request limit for twimg.com in milliseconds (0 = no limit)")
    @Order(70)
    int getGlobalRequestIntervalLimitTwimgComMilliseconds();

    void setGlobalRequestIntervalLimitTwimgComMilliseconds(int milliseconds);

    @AboutConfig
    @SpinnerValidator(min = 0, max = 30000, step = 100)
    @DefaultIntValue(3000)
    @DescriptionForConfigEntry("Profile crawler: Wait time between pagination requests in milliseconds")
    @Order(80)
    int getProfileCrawlerWaittimeBetweenPaginationMilliseconds();

    void setProfileCrawlerWaittimeBetweenPaginationMilliseconds(int milliseconds);
}