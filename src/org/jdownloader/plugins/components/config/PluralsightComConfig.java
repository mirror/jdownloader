package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "pluralsight.com", type = Type.CRAWLER)
public interface PluralsightComConfig extends PluginConfigInterface {
    final String              text_UserAgent                                    = "Enter User-Agent which will be used for all website http requests:";
    final String              text_WaittimeBetweenDownloadsSeconds              = "Define wait time in seconds between downloads";
    final String              text_WaitMode                                     = "Wait between downloads mode";
    final String              text_AddRandomDelaySecondsBetweenDownloads        = "Add random delay in seconds to wait time between downloads?";
    final String              text_AdditionalWaittimeBetweenDownloadsMaxSeconds = "Define max additional random waittime seconds between downloads";
    /** 2021-07-21: Removed "fastLinkcheck" setting for now and so far we never had subtitles support --> Removed subtitle setting too */
    public static TRANSLATION TRANSLATION                                       = new TRANSLATION();

    public static class TRANSLATION {
        // public String isDownloadSubtitles_label() {
        // return "Download Subtitles : ";
        // }
        // public String isDownloadSubtitles_label() {
        // return _JDT.T.lit_add_subtitles();
        // }
        //
        // public String getDownloadSubtitles_label() {
        // return _JDT.T.lit_add_subtitles();
        // }
        public String getUserAgent_label() {
            return text_UserAgent;
        }

        public String getWaitMode_label() {
            return text_WaitMode;
        }

        public String getWaittimeBetweenDownloadsSeconds_label() {
            return text_WaittimeBetweenDownloadsSeconds;
        }

        public String getAddRandomDelaySecondsBetweenDownloads_label() {
            return text_AddRandomDelaySecondsBetweenDownloads;
        }

        public String getAdditionalWaittimeBetweenDownloadsMaxSeconds_label() {
            return text_AdditionalWaittimeBetweenDownloadsMaxSeconds;
        }
    }
    // @AboutConfig
    // @DefaultBooleanValue(true)
    // boolean isDownloadSubtitles();
    //
    // void setDownloadSubtitles(boolean b);
    //
    // @AboutConfig
    // @DefaultBooleanValue(true)
    // public boolean isFastLinkCheckEnabled();
    //
    // public void setFastLinkCheckEnabled(boolean b);

    @AboutConfig
    @DefaultStringValue("JDDEFAULT")
    @DescriptionForConfigEntry(text_UserAgent)
    @Order(10)
    String getUserAgent();

    public void setUserAgent(final String userAgent);

    public static enum WaitMode implements LabelInterface {
        LENGTH_OF_PREVIOUSLY_DOWNLOADED_VIDEO {
            @Override
            public String getLabel() {
                return "Length of previously downloaded video";
            }
        },
        CUSTOM_WAIT {
            @Override
            public String getLabel() {
                return "Custom wait time";
            }
        },
        LENGTH_OF_PREVIOUSLY_DOWNLOADED_VIDEO_AND_CUSTOM_WAIT {
            @Override
            public String getLabel() {
                return "Length of previously downloaded video and custom wait";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("LENGTH_OF_PREVIOUSLY_DOWNLOADED_VIDEO")
    @Order(15)
    @DescriptionForConfigEntry(text_WaitMode)
    WaitMode getWaitMode();

    void setWaitMode(final WaitMode mode);

    @AboutConfig
    @DefaultIntValue(120)
    @SpinnerValidator(min = 90, max = 900, step = 1)
    @Order(20)
    @DescriptionForConfigEntry(text_WaittimeBetweenDownloadsSeconds)
    int getWaittimeBetweenDownloadsSeconds();

    void setWaittimeBetweenDownloadsSeconds(final int seconds);

    @AboutConfig
    @DefaultBooleanValue(false)
    @Order(40)
    @DescriptionForConfigEntry(text_AddRandomDelaySecondsBetweenDownloads)
    boolean isAddRandomDelaySecondsBetweenDownloads();

    void setAddRandomDelaySecondsBetweenDownloads(boolean b);

    @AboutConfig
    @DefaultIntValue(30)
    @SpinnerValidator(min = 0, max = 120, step = 1)
    @Order(50)
    @DescriptionForConfigEntry(text_AdditionalWaittimeBetweenDownloadsMaxSeconds)
    int getAdditionalWaittimeBetweenDownloadsMaxSeconds();

    void setAdditionalWaittimeBetweenDownloadsMaxSeconds(final int seconds);
}
