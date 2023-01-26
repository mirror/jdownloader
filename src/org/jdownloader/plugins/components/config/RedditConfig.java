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

@PluginHost(host = "reddit.com", type = Type.HOSTER)
public interface RedditConfig extends PluginConfigInterface {
    public static final RedditConfig.TRANSLATION TRANSLATION                             = new TRANSLATION();
    final String                                 text_PreferredCommentsPackagenameScheme = "Select preferred package name scheme for single comments";
    final String                                 text_CustomCommentsPackagenameScheme    = "Define custom packagename scheme";
    final String                                 text_PreferredFilenameScheme            = "Select preferred filename scheme";
    final String                                 text_CrawlerTextDownloadMode            = "Crawler: Select text download mode";
    final String                                 text_CrawlUrlsInsidePostText            = "Crawl URLs inside post-text?";
    final String                                 text_ProfileCrawlerMaxPages             = "User crawler: Crawl max X last pages (-1 = unlimited, 0 = disable user crawler)";
    final String                                 text_SubredditCrawlerMaxPages           = "Subreddit crawler: Crawl max X last pages (-1 = unlimited, 0 = disable subreddit crawler)";
    final String                                 text_VideoDownloadStreamType            = "Preferred video download stream type";
    final String                                 text_VideoUseDirecturlAsContentURL      = "Videos: Use direct URL as content URL (URL you get when doing CTRL + C)?";

    public static class TRANSLATION {
        public String getPreferredCommentsPackagenameScheme_label() {
            return text_PreferredCommentsPackagenameScheme;
        }

        public String getPreferredFilenameScheme_label() {
            return text_PreferredFilenameScheme;
        }

        public String getCustomCommentsPackagenameScheme_label() {
            return text_CustomCommentsPackagenameScheme;
        }

        public String getCrawlerTextDownloadMode_label() {
            return text_CrawlerTextDownloadMode;
        }

        public String getCrawlUrlsInsidePostText_label() {
            return text_CrawlUrlsInsidePostText;
        }

        public String getProfileCrawlerMaxPages_label() {
            return text_ProfileCrawlerMaxPages;
        }

        public String getSubredditCrawlerMaxPages_label() {
            return text_SubredditCrawlerMaxPages;
        }

        public String getVideoUseDirecturlAsContentURL_label() {
            return text_VideoUseDirecturlAsContentURL;
        }
    }

    public static enum FilenameScheme implements LabelInterface {
        DATE_SUBREDDIT_POSTID_SERVER_FILENAME {
            @Override
            public String getLabel() {
                return "*date*_*subreddit_title*_*post_id*_*original_filename_without_ext**ext*";
            }
        },
        DATE_SUBREDDIT_POSTID_TITLE {
            @Override
            public String getLabel() {
                return "*date*_*subreddit_title*_*post_id*_*post_title**ext*";
            }
        },
        DATE_SUBREDDIT_POSTID_SLUG {
            @Override
            public String getLabel() {
                return "Default: *date*_*subreddit_title*_*post_id*_*post_slug**ext*";
            }
        },
        SERVER_FILENAME {
            @Override
            public String getLabel() {
                return "*original_filename_without_ext**ext*";
            }
        },
        CUSTOM {
            @Override
            public String getLabel() {
                return "Custom";
            }
        };
    }

    public static enum CommentsPackagenameScheme implements LabelInterface {
        DATE_SUBREDDIT_POSTID_SLUG {
            @Override
            public String getLabel() {
                return "Default: *date*_*subreddit_title*_*post_id*_*post_slug*";
            }
        },
        DATE_SUBREDDIT_POSTID_TITLE {
            @Override
            public String getLabel() {
                return "*date*_*subreddit_title*_*post_id*_*post_title*";
            }
        },
        TITLE {
            @Override
            public String getLabel() {
                return "*post_title*";
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
    @DefaultEnumValue("DATE_SUBREDDIT_POSTID_SLUG")
    @Order(10)
    @DescriptionForConfigEntry(text_PreferredCommentsPackagenameScheme)
    CommentsPackagenameScheme getPreferredCommentsPackagenameScheme();

    void setPreferredCommentsPackagenameScheme(final CommentsPackagenameScheme quality);

    @AboutConfig
    @DefaultStringValue("*date*_*date_timestamp*_*date_timedelta_formatted*_*subreddit_title*_*username*_*post_id*_*post_title*")
    @DescriptionForConfigEntry(text_CustomCommentsPackagenameScheme)
    @Order(11)
    String getCustomCommentsPackagenameScheme();

    public void setCustomCommentsPackagenameScheme(final String str);

    @AboutConfig
    @DefaultEnumValue("DATE_SUBREDDIT_POSTID_SLUG")
    @Order(15)
    @DescriptionForConfigEntry(text_PreferredFilenameScheme)
    FilenameScheme getPreferredFilenameScheme();

    void setPreferredFilenameScheme(final FilenameScheme quality);

    @AboutConfig
    @DefaultStringValue("*date*_*date_timestamp*_*date_timedelta_formatted*_*subreddit_title*_*username*_*post_id*_*post_title**original_filename_without_ext*_*index**ext*")
    @DescriptionForConfigEntry(text_CustomCommentsPackagenameScheme)
    @Order(16)
    String getCustomFilenameScheme();

    public void setCustomFilenameScheme(final String str);

    public static enum TextCrawlerMode implements LabelInterface {
        ALWAYS {
            @Override
            public String getLabel() {
                return "Always";
            }
        },
        ONLY_IF_NO_MEDIA_AVAILABLE {
            @Override
            public String getLabel() {
                return "Only if no media is found";
            }
        },
        NEVER {
            @Override
            public String getLabel() {
                return "Never";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("ALWAYS")
    @Order(17)
    @DescriptionForConfigEntry(text_CrawlerTextDownloadMode)
    TextCrawlerMode getCrawlerTextDownloadMode();

    void setCrawlerTextDownloadMode(final TextCrawlerMode mode);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_CrawlUrlsInsidePostText)
    @Order(20)
    boolean isCrawlUrlsInsidePostText();

    void setCrawlUrlsInsidePostText(boolean b);

    @AboutConfig
    @SpinnerValidator(min = -1, max = 2000, step = 1)
    @DefaultIntValue(1)
    @DescriptionForConfigEntry(text_SubredditCrawlerMaxPages)
    @Order(30)
    int getProfileCrawlerMaxPages();

    void setProfileCrawlerMaxPages(int i);

    @AboutConfig
    @SpinnerValidator(min = -1, max = 2000, step = 1)
    @DefaultIntValue(1)
    @DescriptionForConfigEntry(text_SubredditCrawlerMaxPages)
    @Order(40)
    int getSubredditCrawlerMaxPages();

    void setSubredditCrawlerMaxPages(int i);

    public static enum VideoDownloadStreamType implements LabelInterface {
        DASH {
            @Override
            public String getLabel() {
                return "DASH (possibly higher max. quality)";
            }
        },
        HLS {
            @Override
            public String getLabel() {
                return "HLS";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DASH")
    @Order(45)
    @DescriptionForConfigEntry(text_VideoDownloadStreamType)
    VideoDownloadStreamType getVideoDownloadStreamType();

    void setVideoDownloadStreamType(final VideoDownloadStreamType mode);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_VideoUseDirecturlAsContentURL)
    @Order(50)
    boolean isVideoUseDirecturlAsContentURL();

    void setVideoUseDirecturlAsContentURL(boolean b);
}