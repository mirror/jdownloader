package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
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
        DATE_SUBREDDIT_SERVER_FILENAME {
            @Override
            public String getLabel() {
                return "Date + Subreddit + server internal filename";
            }
        },
        DATE_SUBREDDIT_TITLE {
            @Override
            public String getLabel() {
                return "Date + Subreddit + Title + File-extension";
            }
        },
        SERVER_FILENAME {
            @Override
            public String getLabel() {
                return "Server internal filename";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DATE_SUBREDDIT_SERVER_FILENAME")
    @Order(10)
    @DescriptionForConfigEntry("Select preferred filename scheme")
    FilenameScheme getPreferredFilenameScheme();

    void setPreferredFilenameScheme(FilenameScheme quality);
}