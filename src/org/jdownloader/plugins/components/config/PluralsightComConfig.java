package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "pluralsight.com", type = Type.CRAWLER)
public interface PluralsightComConfig extends PluginConfigInterface {
    /** 2021-07-21: Removed "fastLinkcheck" setting for now and so far we never had subtitles support --> Removed subtitle setting too */
    public static TRANSLATION TRANSLATION = new TRANSLATION();

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
            return "Enter User-Agent which will be used for all website http requests:";
        }

        public String getWaittimeBetweenDownloadsSeconds_label() {
            return "Define waittime seconds between downloads";
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
    @DescriptionForConfigEntry("Enter User-Agent which will be used for all website http requests:")
    @Order(10)
    String getUserAgent();

    public void setUserAgent(final String userAgent);

    @AboutConfig
    @DefaultIntValue(90)
    @SpinnerValidator(min = 90, max = 900, step = 1)
    @Order(20)
    @DescriptionForConfigEntry("Define waittime seconds between downloads")
    int getWaittimeBetweenDownloadsSeconds();

    void setWaittimeBetweenDownloadsSeconds(int wait);
}
