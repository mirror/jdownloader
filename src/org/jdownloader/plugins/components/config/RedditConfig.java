package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
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
    final String                                 text_CrawlCompleteUserProfiles          = "Crawl complete user profiles?\r\nThis can lead to very time consuming crawl processes!";
    final String                                 text_CrawlCompleteSubreddits            = "Crawl complete subreddits?\r\nThis can cause very time consuming crawl processes!";

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

        public String getCrawlCompleteUserProfiles_label() {
            return text_CrawlCompleteUserProfiles;
        }

        public String getCrawlCompleteSubreddits_label() {
            return text_CrawlCompleteSubreddits;
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
    @DefaultStringValue("*date*_*subreddit_title*_*username*_*post_id*_*post_title*")
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
    @DefaultStringValue("*date*_*subreddit_title*_*username*_*post_id*_*post_title**original_filename_without_ext*_*index**ext*")
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
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_CrawlCompleteUserProfiles)
    @Order(30)
    boolean isCrawlCompleteUserProfiles();

    void setCrawlCompleteUserProfiles(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_CrawlCompleteSubreddits)
    @Order(40)
    boolean isCrawlCompleteSubreddits();

    void setCrawlCompleteSubreddits(boolean b);
}