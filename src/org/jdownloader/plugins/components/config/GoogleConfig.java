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
    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    public static class TRANSLATION {
        public String getUserAgent_label() {
            return "Enter User-Agent which will be used for all Google website http requests";
        }

        public String getPreferredQuality_label() {
            return "Select preferred stream-quality.\r\nIf your preferred stream quality is not found, best stream quality will be downloaded instead.";
        }

        public String getGoogleDriveAPIKey_label() {
            return "Enter Google Drive API key see: developers.google.com/drive/api/v3/enable-drive-api\r\nThis API key will be used for GDrive folder crawling, linkchecking and downloading.";
        }

        public String getAPIDownloadMode_label() {
            return "Set preferred API download mode (only relevant if API Key is provided.)";
        }

        public String getAddStreamQualityIdentifierToFilename_label() {
            return "Add quality identifier to filename if non-original video stream is downloaded?";
        }
    }

    @AboutConfig
    @DefaultStringValue("JDDEFAULT")
    @DescriptionForConfigEntry("Enter User-Agent which will be used for all Google website http requests")
    @Order(10)
    String getUserAgent();

    public void setUserAgent(final String userAgent);

    public static enum PreferredQuality implements LabelInterface {
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
    @DescriptionForConfigEntry("Select preferred stream-quality.\r\nIf your preferred stream quality is not found, best stream quality will be downloaded instead.")
    @Order(15)
    PreferredQuality getPreferredQuality();

    void setPreferredQuality(final PreferredQuality quality);

    @AboutConfig
    @DefaultStringValue("")
    @DescriptionForConfigEntry("Enter Google Drive API key see: developers.google.com/drive/api/v3/enable-drive-api\r\nThis API key will be used for GDrive folder crawling, linkchecking and downloading.")
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
    @DescriptionForConfigEntry("Set preferred API download mode (only relevant if API Key is provided.)")
    @Order(25)
    APIDownloadMode getAPIDownloadMode();

    void setAPIDownloadMode(final APIDownloadMode apiDownloadMode);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("If API Key is given: Prefer website for downloading if stream download is preferred and possible?")
    @Order(40)
    boolean isPreferWebsiteOverAPIIfStreamDownloadIsWantedAndPossible();

    void setPreferWebsiteOverAPIIfStreamDownloadIsWantedAndPossible(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Adds quality identifier to filename if video stream (= non-original file) is downloaded.")
    @Order(50)
    boolean isAddStreamQualityIdentifierToFilename();

    void setAddStreamQualityIdentifierToFilename(boolean b);
}