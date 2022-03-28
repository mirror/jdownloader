package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "reddit.com", type = Type.HOSTER)
public interface RedditConfig extends PluginConfigInterface {
    public static final RedditConfig.TRANSLATION TRANSLATION = new TRANSLATION();

    public static class TRANSLATION {
        public String getPreferredCommentsPackagenameScheme_label() {
            return "Select preferred package name scheme for comments";
        }

        public String getPreferredFilenameScheme_label() {
            return "Select preferred filename scheme";
        }

        public String getTextCrawlerMode_label() {
            return "Crawler: Select text download mode";
        }

        public String getCrawlUrlsInsidePostText_label() {
            return "Crawl URLs inside post-text?";
        }

        public String getCrawlCompleteUserProfiles_label() {
            return "Crawl complete user profiles?\r\nThis can cause very time consuming crawl processes!";
        }

        public String getCrawlCompleteSubreddits_label() {
            return "Crawl complete subreddits?\r\nThis can cause very time consuming crawl processes!";
        }
    }

    public static enum FilenameScheme implements LabelInterface {
        DATE_SUBREDDIT_POSTID_SERVER_FILENAME {
            @Override
            public String getLabel() {
                return "Date_Subreddit_Post-ID_server internal filename";
            }
        },
        DATE_SUBREDDIT_POSTID_TITLE {
            @Override
            public String getLabel() {
                return "Date_Subreddit_Post-ID - Title.File-extension";
            }
        },
        DATE_SUBREDDIT_POSTID_SLUG {
            @Override
            public String getLabel() {
                return "Date_Subreddit_Post-ID_slug.File-extension";
            }
        },
        SERVER_FILENAME {
            @Override
            public String getLabel() {
                return "Server internal filename";
            }
        };
    }

    public static enum CommentsPackagenameScheme implements LabelInterface {
        DATE_SUBREDDIT_POSTID_SLUG {
            @Override
            public String getLabel() {
                return "Date_Subreddit_Post-ID_slug";
            }
        },
        DATE_SUBREDDIT_POSTID_TITLE {
            @Override
            public String getLabel() {
                return "Date_Subreddit_Post-ID_Post title";
            }
        },
        TITLE {
            @Override
            public String getLabel() {
                return "Post title";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DATE_SUBREDDIT_POSTID_SLUG")
    @Order(10)
    @DescriptionForConfigEntry("Select preferred package name scheme for comments")
    CommentsPackagenameScheme getPreferredCommentsPackagenameScheme();

    void setPreferredCommentsPackagenameScheme(final CommentsPackagenameScheme quality);

    @AboutConfig
    @DefaultEnumValue("DATE_SUBREDDIT_POSTID_SLUG")
    @Order(15)
    @DescriptionForConfigEntry("Select preferred filename scheme")
    FilenameScheme getPreferredFilenameScheme();

    void setPreferredFilenameScheme(final FilenameScheme quality);

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
    @DescriptionForConfigEntry("Crawler: Select text download mode")
    TextCrawlerMode getCrawlerTextDownloadMode();

    void setCrawlerTextDownloadMode(final TextCrawlerMode mode);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Crawl URLs inside post-text?")
    @Order(20)
    boolean isCrawlUrlsInsidePostText();

    void setCrawlUrlsInsidePostText(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Crawl complete user profiles?\r\nThis can cause very time consuming crawl processes!")
    @Order(30)
    boolean isCrawlCompleteUserProfiles();

    void setCrawlCompleteUserProfiles(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Crawl complete subreddits?\r\nThis can cause very time consuming crawl processes!")
    @Order(40)
    boolean isCrawlCompleteSubreddits();

    void setCrawlCompleteSubreddits(boolean b);
}