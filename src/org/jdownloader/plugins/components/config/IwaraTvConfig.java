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
    public static final IwaraTvConfig.TRANSLATION TRANSLATION                            = new TRANSLATION();
    final String                                  text_ProfileCrawlerEnableFastLinkcheck = "Enable fast linkcheck for videos found via profile crawler?";
    final String                                  text_PreferredFilenameSchemeType       = "Select preferred filename scheme type";
    final String                                  text_PreferredFilenameScheme           = "Select preferred filename scheme";
    final String                                  text_FindFilesizeDuringAvailablecheck  = "Find filesize during linkcheck?\r\nWarning: Can slow down linkcheck!";

    public static class TRANSLATION {
        public String getProfileCrawlerEnableFastLinkcheck_label() {
            return text_ProfileCrawlerEnableFastLinkcheck;
        }

        public String getPreferredFilenameSchemeType_label() {
            return text_PreferredFilenameSchemeType;
        }

        public String getPreferredFilenameScheme_label() {
            return text_PreferredFilenameScheme;
        }

        public String getFindFilesizeDuringAvailablecheck_label() {
            return text_FindFilesizeDuringAvailablecheck;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_ProfileCrawlerEnableFastLinkcheck)
    @Order(10)
    boolean isProfileCrawlerEnableFastLinkcheck();

    void setProfileCrawlerEnableFastLinkcheck(boolean b);

    public static enum FilenameSchemeType implements LabelInterface {
        PLUGIN {
            @Override
            public String getLabel() {
                return "Plugin / Customized";
            }
        },
        ORIGINAL_SERVER_FILENAMES {
            @Override
            public String getLabel() {
                return "Server/Original e.g. '1234567890_xxxYYY_540.mp4'";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("PLUGIN")
    @Order(15)
    @DescriptionForConfigEntry(text_PreferredFilenameSchemeType)
    FilenameSchemeType getPreferredFilenameSchemeType();

    void setPreferredFilenameSchemeType(final FilenameSchemeType quality);

    public static enum FilenameScheme implements LabelInterface {
        DATE_UPLOADER_VIDEOID_TITLE {
            @Override
            public String getLabel() {
                return "date_uploader_videoid_title.ext";
            }
        },
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
    @DescriptionForConfigEntry(text_PreferredFilenameScheme)
    FilenameScheme getPreferredFilenameScheme();

    void setPreferredFilenameScheme(final FilenameScheme scheme);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_FindFilesizeDuringAvailablecheck)
    @Order(30)
    boolean isFindFilesizeDuringAvailablecheck();

    void setFindFilesizeDuringAvailablecheck(boolean b);
}