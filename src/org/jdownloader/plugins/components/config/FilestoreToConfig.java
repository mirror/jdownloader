package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.TakeValueFromSubconfig;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "filestore.to", type = Type.HOSTER)
public interface FilestoreToConfig extends PluginConfigInterface {
    final String              TEXT_USER_AGENT                = "Enter User-Agent which will be used for all website http requests:";
    final String              TEXT_MODIFY_FINAL_DOWNLOADURLS = "Modify final downloadurls in free mode: Replace '/free' with '/premium'?";
    public static TRANSLATION TRANSLATION                    = new TRANSLATION();

    public static class TRANSLATION {
        public String getGlobalNoFreeSlotsBlockModeEnabled_label() {
            return "Block all links of this host on error 'No free slots available'?";
        }

        public String getWaittimeOnNoFreeSlotsMinutes_label() {
            return "Wait minutes on error 'no free slots available'";
        }

        public String getStartFreeDownloadsSequentially_label() {
            return "Start free downloads sequentially?";
        }

        public String getWaittimeBetweenDownloadStartsSeconds_label() {
            return "Wait seconds between download-starts";
        }

        public String getUserAgent_label() {
            return TEXT_USER_AGENT;
        }

        public String getModifyFinalDownloadurls_label() {
            return TEXT_MODIFY_FINAL_DOWNLOADURLS;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @Order(5)
    @DescriptionForConfigEntry("Block all links of this host on error 'No free slots available'?")
    public boolean isGlobalNoFreeSlotsBlockModeEnabled();

    public void setGlobalNoFreeSlotsBlockModeEnabled(boolean b);

    @AboutConfig
    @DefaultIntValue(10)
    @TakeValueFromSubconfig("WAIT_MINUTES_ON_NO_FREE_SLOTS")
    @SpinnerValidator(min = 1, max = 600, step = 1)
    @Order(10)
    @DescriptionForConfigEntry("Wait minutes on error 'no free slots available'")
    int getWaittimeOnNoFreeSlotsMinutes();

    void setWaittimeOnNoFreeSlotsMinutes(int wait);

    @AboutConfig
    @DefaultBooleanValue(true)
    @Order(15)
    @DescriptionForConfigEntry("Start free downloads sequentially?")
    public boolean isStartFreeDownloadsSequentially();

    public void setStartFreeDownloadsSequentially(boolean b);

    @AboutConfig
    @DefaultIntValue(10)
    @SpinnerValidator(min = 0, max = 30, step = 1)
    @Order(20)
    @DescriptionForConfigEntry("Wait seconds between download-starts")
    int getWaittimeBetweenDownloadStartsSeconds();

    void setWaittimeBetweenDownloadStartsSeconds(int wait);

    @AboutConfig
    @DefaultStringValue("JDDEFAULT")
    @DescriptionForConfigEntry(TEXT_USER_AGENT)
    @Order(30)
    String getUserAgent();

    public void setUserAgent(final String userAgent);

    @AboutConfig
    @DefaultBooleanValue(false)
    @Order(35)
    @DescriptionForConfigEntry(TEXT_MODIFY_FINAL_DOWNLOADURLS)
    public boolean isModifyFinalDownloadurls();

    public void setModifyFinalDownloadurls(boolean b);
}
