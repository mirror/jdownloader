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
    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    public static class TRANSLATION {
        /* 2022-03-18: Not needed anymore for now. */
        // public String getForceGrabMediaOnlyEnabled_label() {
        // return "Force grab media? Disable this to also crawl media of retweets and other content from users' timelines (only if you
        // add URLs without '/media'!)";
        // }
        public String getUseOriginalFilenames_label() {
            return "Use original filename instead of format *date*_*username*_*postID*_*mediaindex*.*ext*?";
        }

        public String getAddTweetTextAsTextfile_label() {
            return "Add tweet text as textfile?";
        }

        public String getCrawlURLsInsideTweetText_label() {
            return "Crawl URLs inside post text?\r\nWarning: This may result in endless crawling activity!";
        }

        public String getPreferHLSVideoDownload_label() {
            return "Videos: Prefer HLS over http download?";
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
    @DescriptionForConfigEntry("Use original filename instead of format *date*_*username*_*postID*_*mediaindex*.*ext*?")
    @Order(20)
    boolean isUseOriginalFilenames();

    void setUseOriginalFilenames(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    @DescriptionForConfigEntry("Add tweet text as textfile?")
    @Order(30)
    boolean isAddTweetTextAsTextfile();

    void setAddTweetTextAsTextfile(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry("Crawl URLs inside post text?\r\nWarning: This may result in endless crawling activity!")
    @Order(40)
    boolean isCrawlURLsInsideTweetText();

    void setCrawlURLsInsideTweetText(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry("Videos: Prefer HLS over http download?")
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