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

        public String getPreferWebsiteOverAPIIfAccountIsAvailable_label() {
            return "If API Key is given and valid account is available: Prefer website for downloading to avoid 'Quota reached' errors?";
        }
    }

    @AboutConfig
    @DefaultStringValue("JDDEFAULT")
    @DescriptionForConfigEntry("Enter User-Agent which will be used for all Google website http requests")
    @Order(10)
    String getUserAgent();

    public void setUserAgent(String userAgent);

    @AboutConfig
    @DefaultStringValue("")
    @DescriptionForConfigEntry("Enter Google Drive API key see: developers.google.com/drive/api/v3/enable-drive-api\r\nThis API key will be used for GDrive folder crawling, linkchecking and downloading.")
    @Order(15)
    String getGoogleDriveAPIKey();

    public void setGoogleDriveAPIKey(String apikey);

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
    @Order(20)
    PreferredQuality getPreferredQuality();

    void setPreferredQuality(final PreferredQuality quality);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("If API Key is given and valid account is available: Prefer website for downloading to avoid 'Quota reached' errors?")
    @Order(30)
    boolean isPreferWebsiteOverAPIIfAccountIsAvailable();

    void setPreferWebsiteOverAPIIfAccountIsAvailable(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("If API Key is given: Prefer website for downloading if stream download is preferred and possible?")
    @Order(40)
    boolean isPreferWebsiteOverAPIIfStreamDownloadIsWantedAndPossible();

    void setPreferWebsiteOverAPIIfStreamDownloadIsWantedAndPossible(boolean b);
}