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
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "twitter.com", type = Type.HOSTER)
public interface TwitterConfigInterface extends PluginConfigInterface {
    public static final TRANSLATION TRANSLATION                                              = new TRANSLATION();
    final String                    text_FilenameScheme                                      = "Filename scheme";
    final String                    text_MarkTweetRepliesViaFilename                         = "Append '_reply' to filenames of tweets that are replies to other tweets?";
    final String                    text_SingleTweetCrawlerAddTweetTextAsTextfile            = "Single Tweet crawler: Add tweet text as textfile?";
    final String                    text_SingleTweetCrawlerCrawlMode                         = "Single Tweet crawler: Crawl mode";
    final String                    text_CrawlURLsInsideTweetText                            = "Crawl URLs inside post text?\r\nWarning: This may result in endless crawling activity!";
    final String                    text_RegexWhitelistForCrawledUrlsInTweetText             = "RegEx whitelist for crawled URLs in tweet text e.g. '(?i).*(site\\.tld|site2\\.tld).*' [Empty = Allow all URLs]";
    final String                    text_CrawlRetweetsV2                                     = "Crawl retweets?";
    final String                    text_CrawlVideoThumbnail                                 = "Crawl video thumbnail?";
    final String                    text_PreferHLSVideoDownload                              = "Videos: Prefer HLS over http download?";
    final String                    text_GlobalRequestIntervalLimitApiTwitterComMilliseconds = "Define global request limit for api.twitter.com in milliseconds (0 = no limit)";
    final String                    text_GlobalRequestIntervalLimitTwimgComMilliseconds      = "Define global request limit for twimg.com in milliseconds (0 = no limit)";
    final String                    text_ProfileCrawlerWaittimeBetweenPaginationMilliseconds = "Profile crawler: Wait time between pagination requests in milliseconds";

    public static class TRANSLATION {
        /* 2022-03-18: Not needed anymore for now. */
        // public String getForceGrabMediaOnlyEnabled_label() {
        // return "Force grab media? Disable this to also crawl media of retweets and other content from users' timelines (only if you
        // add URLs without '/media'!)";
        // }
        public String getFilenameScheme_label() {
            return text_FilenameScheme;
        }

        public String getMarkTweetRepliesViaFilename_label() {
            return text_MarkTweetRepliesViaFilename;
        }

        public String getSingleTweetCrawlerAddTweetTextAsTextfile_label() {
            return text_SingleTweetCrawlerAddTweetTextAsTextfile;
        }

        public String getSingleTweetCrawlerCrawlMode_label() {
            return text_SingleTweetCrawlerCrawlMode;
        }

        public String getCrawlURLsInsideTweetText_label() {
            return text_CrawlURLsInsideTweetText;
        }

        public String getRegexWhitelistForCrawledUrlsInTweetText_label() {
            return text_RegexWhitelistForCrawledUrlsInTweetText;
        }

        public String getCrawlRetweetsV2_label() {
            return text_CrawlRetweetsV2;
        }

        public String getCrawlVideoThumbnail_label() {
            return text_CrawlVideoThumbnail;
        }

        public String getPreferHLSVideoDownload_label() {
            return text_PreferHLSVideoDownload;
        }

        public String getGlobalRequestIntervalLimitApiTwitterComMilliseconds_label() {
            return text_GlobalRequestIntervalLimitApiTwitterComMilliseconds;
        }

        public String getGlobalRequestIntervalLimitTwimgComMilliseconds_label() {
            return text_GlobalRequestIntervalLimitTwimgComMilliseconds;
        }

        public String getProfileCrawlerWaittimeBetweenPaginationMilliseconds_label() {
            return text_ProfileCrawlerWaittimeBetweenPaginationMilliseconds;
        }
    }

    public static enum FilenameScheme implements LabelInterface {
        AUTO {
            @Override
            public String getLabel() {
                return "Auto";
            }
        },
        ORIGINAL {
            @Override
            public String getLabel() {
                return "Original";
            }
        },
        ORIGINAL_PLUS {
            @Override
            public String getLabel() {
                return "Original+: <date>_<tweet_id>_<originalFilenameWithoutExt>.<ext>";
            }
        },
        ORIGINAL_PLUS_2 {
            @Override
            public String getLabel() {
                return "Original+2: <date>_<username>_<tweet_id>_<originalFilenameWithoutExt>.<ext>";
            }
        },
        PLUGIN {
            @Override
            public String getLabel() {
                return "Plugin: <date>_<username>_<tweet_id>_<reply>_<mediaIndex>.<ext>";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("AUTO")
    @Order(20)
    @DescriptionForConfigEntry(text_FilenameScheme)
    FilenameScheme getFilenameScheme();

    void setFilenameScheme(final FilenameScheme scheme);

    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry(text_MarkTweetRepliesViaFilename)
    @Order(25)
    boolean isMarkTweetRepliesViaFilename();

    void setMarkTweetRepliesViaFilename(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    @DescriptionForConfigEntry(text_SingleTweetCrawlerAddTweetTextAsTextfile)
    @Order(30)
    boolean isSingleTweetCrawlerAddTweetTextAsTextfile();

    void setSingleTweetCrawlerAddTweetTextAsTextfile(boolean b);

    public static enum SingleTweetCrawlerMode implements LabelInterface {
        AUTO {
            @Override
            public String getLabel() {
                return "Auto";
            }
        },
        OLD_API {
            @Override
            public String getLabel() {
                return "Old API";
            }
        },
        NEW_API {
            @Override
            public String getLabel() {
                return "New API";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("AUTO")
    @Order(31)
    @DescriptionForConfigEntry(text_SingleTweetCrawlerCrawlMode)
    SingleTweetCrawlerMode getSingleTweetCrawlerCrawlMode();

    void setSingleTweetCrawlerCrawlMode(final SingleTweetCrawlerMode mode);

    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry(text_CrawlRetweetsV2)
    @Order(35)
    boolean isCrawlRetweetsV2();

    void setCrawlRetweetsV2(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry(text_CrawlVideoThumbnail)
    @Order(36)
    boolean isCrawlVideoThumbnail();

    void setCrawlVideoThumbnail(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry(text_CrawlURLsInsideTweetText)
    @Order(40)
    boolean isCrawlURLsInsideTweetText();

    void setCrawlURLsInsideTweetText(boolean b);

    @AboutConfig
    @DefaultStringValue("")
    @DescriptionForConfigEntry(text_RegexWhitelistForCrawledUrlsInTweetText)
    @Order(45)
    String getRegexWhitelistForCrawledUrlsInTweetText();

    void setRegexWhitelistForCrawledUrlsInTweetText(String str);

    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry(text_PreferHLSVideoDownload)
    @Order(50)
    boolean isPreferHLSVideoDownload();

    void setPreferHLSVideoDownload(boolean b);

    @AboutConfig
    @SpinnerValidator(min = 0, max = 60000, step = 100)
    @DefaultIntValue(500)
    @DescriptionForConfigEntry(text_GlobalRequestIntervalLimitApiTwitterComMilliseconds)
    @Order(60)
    int getGlobalRequestIntervalLimitApiTwitterComMilliseconds();

    void setGlobalRequestIntervalLimitApiTwitterComMilliseconds(int milliseconds);

    @AboutConfig
    @SpinnerValidator(min = 0, max = 60000, step = 100)
    @DefaultIntValue(500)
    @DescriptionForConfigEntry(text_GlobalRequestIntervalLimitTwimgComMilliseconds)
    @Order(70)
    int getGlobalRequestIntervalLimitTwimgComMilliseconds();

    void setGlobalRequestIntervalLimitTwimgComMilliseconds(int milliseconds);

    @AboutConfig
    @SpinnerValidator(min = 0, max = 30000, step = 100)
    @DefaultIntValue(3000)
    @DescriptionForConfigEntry(text_ProfileCrawlerWaittimeBetweenPaginationMilliseconds)
    @Order(80)
    int getProfileCrawlerWaittimeBetweenPaginationMilliseconds();

    void setProfileCrawlerWaittimeBetweenPaginationMilliseconds(int milliseconds);
}