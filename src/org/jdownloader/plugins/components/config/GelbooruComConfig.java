package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "gelbooru.com", type = Type.HOSTER)
public interface GelbooruComConfig extends PluginConfigInterface {
    public static final GelbooruComConfig.TRANSLATION TRANSLATION = new TRANSLATION();

    public static class TRANSLATION {
        public String getPreferredFilenameScheme_label() {
            return "Preferred filename scheme";
        }
    }

    final FilenameScheme defaultFilenameScheme = FilenameScheme.PLUGIN_FILENAME;

    public static enum FilenameScheme implements LabelInterface {
        PLUGIN_FILENAME {
            @Override
            public String getLabel() {
                return "Plugin: *post_id**ext*";
            }
        },
        SERVER_FILENAME {
            @Override
            public String getLabel() {
                return "Serverside / original filename";
            }
        },
        DEFAULT {
            @Override
            public String getLabel() {
                return "Default: " + defaultFilenameScheme.getLabel();
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DEFAULT")
    @Order(10)
    @DescriptionForConfigEntry("")
    FilenameScheme getPreferredFilenameScheme();

    void setPreferredFilenameScheme(final FilenameScheme scheme);
}