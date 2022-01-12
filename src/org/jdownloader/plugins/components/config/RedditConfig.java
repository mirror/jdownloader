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
    public static enum FilenameScheme implements LabelInterface {
        DATE_SUBREDDIT_POSTID_SERVER_FILENAME {
            @Override
            public String getLabel() {
                return "Date + Subreddit + Post-ID + server internal filename";
            }
        },
        DATE_SUBREDDIT_POSTID_TITLE {
            @Override
            public String getLabel() {
                return "Date + Subreddit + Post-ID + Title + File-extension";
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
        DATE_SUBREDDIT_ID_SLUG {
            @Override
            public String getLabel() {
                return "Date + Subreddit + Post-ID + slug";
            }
        },
        DATE_SUBREDDIT_ID_TITLE {
            @Override
            public String getLabel() {
                return "Date + Subreddit + Post-ID + Post title";
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
    @DefaultEnumValue("DATE_SUBREDDIT_ID_SLUG")
    @Order(10)
    @DescriptionForConfigEntry("Select preferred filename scheme")
    CommentsPackagenameScheme getPreferredCommentsPackagenameScheme();

    @AboutConfig
    @DefaultEnumValue("DATE_SUBREDDIT_SERVER_FILENAME")
    @Order(15)
    @DescriptionForConfigEntry("Select preferred filename scheme")
    FilenameScheme getPreferredFilenameScheme();

    void setPreferredFilenameScheme(final FilenameScheme quality);

    void setPreferredCommentsPackagename(final CommentsPackagenameScheme quality);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Crawl URLs inside post text?")
    @Order(20)
    boolean isCrawlUrlsInsidePostText();

    void setCrawlUrlsInsidePostText(boolean b);
}