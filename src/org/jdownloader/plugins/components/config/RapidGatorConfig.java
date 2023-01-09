package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.TakeValueFromSubconfig;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "rapidgator.net", type = Type.HOSTER)
public interface RapidGatorConfig extends PluginConfigInterface {
    final String                    text_ActivateExperimentalWaittimeHandling             = "Activate experimental waittime handling to prevent 24-hours IP ban from rapidgator?";
    final String                    text_EnableAPIPremium                                 = "Enable API for premium downloads [recommended] (disabled = use website for premium downloads)?";
    final String                    text_EnableResumeFree                                 = "Attempt to resume stopped downloads in free (& free-account) mode? Rapidgator sometimes allows resume in free mode for some files and sometimes doesn't.";
    final String                    text_ExperimentalEnforceSSL                           = "Activate experimental forced SSL for downloads?";
    final String                    text_Referer                                          = "Define custom referer";
    final String                    text_ReadTimeout                                      = "Define custom browser read-timeout (seconds)";
    final String                    text_WaitSecondsOnErrorYouCantDownloadMoreThanOneFile = "Wait time on error 'You can't download more than one file at the same time' (seconds)";
    public static final TRANSLATION TRANSLATION                                           = new TRANSLATION();

    public static class TRANSLATION {
        public String getActivateExperimentalWaittimeHandling_label() {
            return text_ActivateExperimentalWaittimeHandling;
        }

        public String getEnableAPIPremium_label() {
            return text_EnableAPIPremium;
        }

        public String getEnableResumeFree_label() {
            return text_EnableResumeFree;
        }

        public String getExperimentalEnforceSSL_label() {
            return text_ExperimentalEnforceSSL;
        }

        public String getReferer_label() {
            return text_Referer;
        }

        public String getReadTimeout_label() {
            return text_ReadTimeout;
        }

        public String getWaitSecondsOnErrorYouCantDownloadMoreThanOneFile_label() {
            return text_WaitSecondsOnErrorYouCantDownloadMoreThanOneFile;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("EXPERIMENTALHANDLING")
    @DescriptionForConfigEntry(text_ActivateExperimentalWaittimeHandling)
    @Order(10)
    boolean isActivateExperimentalWaittimeHandling();

    void setActivateExperimentalWaittimeHandling(boolean b);

    /* Some users always get server error 500 via API thus website might work better for them. */
    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_EnableAPIPremium)
    @Order(20)
    boolean isEnableAPIPremium();

    void setEnableAPIPremium(boolean b);

    /**
     * 2020-08-05: Resume in free mode is sometimes working, sometimes not. This setting allows users to disable resuming so they e.g.
     * always get the "non resumable downloads active" warning when stopping their downloads.
     */
    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_EnableResumeFree)
    @Order(21)
    boolean isEnableResumeFree();

    void setEnableResumeFree(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("EXPERIMENTAL_ENFORCE_SSL")
    @DescriptionForConfigEntry(text_ExperimentalEnforceSSL)
    @Order(30)
    boolean isExperimentalEnforceSSL();

    void setExperimentalEnforceSSL(boolean b);

    @AboutConfig
    @TakeValueFromSubconfig("CUSTOM_REFERER")
    @DescriptionForConfigEntry(text_Referer)
    @Order(40)
    String getReferer();

    void setReferer(String str);

    @AboutConfig
    @DefaultIntValue(120)
    @SpinnerValidator(min = 1, max = 300, step = 1)
    @Order(50)
    @DescriptionForConfigEntry(text_ReadTimeout)
    int getReadTimeout();

    void setReadTimeout(int i);

    @AboutConfig
    @DefaultIntValue(300)
    @SpinnerValidator(min = 15, max = 900, step = 1)
    @Order(60)
    @DescriptionForConfigEntry(text_WaitSecondsOnErrorYouCantDownloadMoreThanOneFile)
    int getWaitSecondsOnErrorYouCantDownloadMoreThanOneFile();

    void setWaitSecondsOnErrorYouCantDownloadMoreThanOneFile(int i);
}