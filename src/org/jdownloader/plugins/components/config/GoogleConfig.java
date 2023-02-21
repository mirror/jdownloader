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

@PluginHost(host = "drive.google.com", type = Type.HOSTER)
public interface GoogleConfig extends PluginConfigInterface {
    final String                    text_UserAgent                                               = "User-Agent which will be used for all Google website http requests";
    final String                    text_PreferredVideoQuality                                   = "Preferred video quality.\r\nIf you prefer stream download and the preferred stream quality is not found, best stream quality will be downloaded instead.";
    final String                    text_AllowStreamDownloadAsFallbackOnQuotaLimitReached        = "Allow stream download if original file can't be downloaded due to a quota limit?";
    final String                    text_GoogleDriveAPIKey                                       = "Google Drive API key see: developers.google.com/drive/api/v3/enable-drive-api\r\nIt will be used for GDrive folder crawling, linkchecking and downloading.";
    final String                    text_APIDownloadMode                                         = "API download mode (only relevant if API Key is provided.)";
    final String                    text_PreferWebsiteOverAPIIfStreamDownloadIsWantedAndPossible = "If API key is available: Prefer website for downloading if stream download is preferred and possible?";
    final String                    text_AddStreamQualityIdentifierToFilename                    = "Add quality identifier to filename if video stream (= non-original file) is downloaded?";
    public static final TRANSLATION TRANSLATION                                                  = new TRANSLATION();

    public static class TRANSLATION {
        public String getUserAgent_label() {
            return text_UserAgent;
        }

        public String getPreferredVideoQuality_label() {
            return text_PreferredVideoQuality;
        }

        public String getAllowStreamDownloadAsFallbackOnQuotaLimitReached_label() {
            return text_AllowStreamDownloadAsFallbackOnQuotaLimitReached;
        }

        public String getGoogleDriveAPIKey_label() {
            return text_GoogleDriveAPIKey;
        }

        public String getAPIDownloadMode_label() {
            return text_APIDownloadMode;
        }

        public String getPreferWebsiteOverAPIIfStreamDownloadIsWantedAndPossible_label() {
            return text_PreferWebsiteOverAPIIfStreamDownloadIsWantedAndPossible;
        }

        public String getAddStreamQualityIdentifierToFilename_label() {
            return text_AddStreamQualityIdentifierToFilename;
        }
    }

    @AboutConfig
    @DefaultStringValue("JDDEFAULT")
    @DescriptionForConfigEntry(text_UserAgent)
    @Order(10)
    String getUserAgent();

    public void setUserAgent(final String userAgent);

    public static enum PreferredVideoQuality implements LabelInterface {
        ORIGINAL {
            @Override
            public String getLabel() {
                return "Original file";
            }
        },
        STREAM_BEST {
            @Override
            public String getLabel() {
                return "Best stream";
            }
        },
        STREAM_360P {
            @Override
            public String getLabel() {
                return "360p";
            }
        },
        STREAM_480P {
            @Override
            public String getLabel() {
                return "480p";
            }
        },
        STREAM_720P {
            @Override
            public String getLabel() {
                return "720p";
            }
        },
        STREAM_1080P {
            @Override
            public String getLabel() {
                return "1080p";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("ORIGINAL")
    @DescriptionForConfigEntry(text_PreferredVideoQuality)
    @Order(15)
    PreferredVideoQuality getPreferredVideoQuality();

    void setPreferredVideoQuality(final PreferredVideoQuality quality);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_AllowStreamDownloadAsFallbackOnQuotaLimitReached)
    @Order(16)
    boolean isAllowStreamDownloadAsFallbackOnQuotaLimitReached();

    void setAllowStreamDownloadAsFallbackOnQuotaLimitReached(boolean b);

    @AboutConfig
    @DefaultStringValue("")
    @DescriptionForConfigEntry(text_GoogleDriveAPIKey)
    @Order(20)
    String getGoogleDriveAPIKey();

    public void setGoogleDriveAPIKey(String apikey);

    public static enum APIDownloadMode implements LabelInterface {
        API_ONLY {
            @Override
            public String getLabel() {
                return "API only (except for stream downloads)";
            }
        },
        WEBSITE_IF_ACCOUNT_AVAILABLE {
            @Override
            public String getLabel() {
                return "Use website if account is available";
            }
        },
        WEBSITE_IF_ACCOUNT_AVAILABLE_AND_FILE_IS_QUOTA_LIMITED {
            @Override
            public String getLabel() {
                return "Use website if account is available and file is quota limited";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("WEBSITE_IF_ACCOUNT_AVAILABLE_AND_FILE_IS_QUOTA_LIMITED")
    @DescriptionForConfigEntry(text_APIDownloadMode)
    @Order(25)
    APIDownloadMode getAPIDownloadMode();

    void setAPIDownloadMode(final APIDownloadMode apiDownloadMode);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_PreferWebsiteOverAPIIfStreamDownloadIsWantedAndPossible)
    @Order(40)
    boolean isPreferWebsiteOverAPIIfStreamDownloadIsWantedAndPossible();

    void setPreferWebsiteOverAPIIfStreamDownloadIsWantedAndPossible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_AddStreamQualityIdentifierToFilename)
    @Order(50)
    boolean isAddStreamQualityIdentifierToFilename();

    void setAddStreamQualityIdentifierToFilename(boolean b);
}