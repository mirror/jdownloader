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

@PluginHost(host = "iwara.tv", type = Type.HOSTER)
public interface IwaraTvConfig extends PluginConfigInterface {
    public static final IwaraTvConfig.TRANSLATION TRANSLATION = new TRANSLATION();

    public static class TRANSLATION {
        public String getProfileCrawlerEnableFastLinkcheck_label() {
            return "Enable fast linkcheck for videos found via profile crawler?";
        }

        public String getPreferredFilenameScheme_label() {
            return "Select preferred filename scheme";
        }

        public String getFindFilesizeDuringAvailablecheck_label() {
            return "Find filesize during linkcheck?\r\nWarning: Can slow down linkcheck!";
        }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Enable fast linkcheck for videos found via profile crawler?")
    @Order(10)
    boolean isProfileCrawlerEnableFastLinkcheck();

    void setProfileCrawlerEnableFastLinkcheck(boolean b);

    public static enum FilenameScheme implements LabelInterface {
        UPLOADER_VIDEOID_TITLE {
            @Override
            public String getLabel() {
                return "uploader_videoid_title.ext";
            }
        },
        DATE_UPLOADER_VIDEOID {
            @Override
            public String getLabel() {
                return "date_uploader_videoid.ext";
            }
        },
        DATE_UPLOADER_SPACE_TITLE {
            @Override
            public String getLabel() {
                return "date_uploader title.ext";
            }
        },
        DATE_IN_BRACKETS_SPACE_TITLE {
            @Override
            public String getLabel() {
                return "[date] title.ext";
            }
        },
        TITLE {
            @Override
            public String getLabel() {
                return "title.ext";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DATE_UPLOADER_SPACE_TITLE")
    @Order(20)
    @DescriptionForConfigEntry("Select preferred filename scheme")
    FilenameScheme getPreferredFilenameScheme();

    void setPreferredFilenameScheme(final FilenameScheme quality);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Find filesize during linkcheck?\r\nWarning: Can slow down linkcheck!")
    @Order(30)
    boolean isFindFilesizeDuringAvailablecheck();

    void setFindFilesizeDuringAvailablecheck(boolean b);
}