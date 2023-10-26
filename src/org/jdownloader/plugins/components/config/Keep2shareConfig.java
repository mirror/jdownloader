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

@PluginHost(host = "keep2share.cc", type = Type.HOSTER)
public interface Keep2shareConfig extends PluginConfigInterface {
    public static class TRANSLATION {
        public String getEnableReconnectWorkaround_label() {
            return "Enable reconnect workaround for free mode?";
        }

        public String getReferer_label() {
            return "Custom referer e.g. 'refererexamplewebsite.tld'";
        }

        public String getForceCustomReferer_label() {
            return "Force custom referer even if referer is given in URL via '?site=refererexamplewebsite.tld'?";
        }

        public String getEnableSSL_label() {
            return "Use Secure Communication over SSL (HTTPS://)";
        }

        public String getMaxSimultaneousFreeDownloads_label() {
            return "Max. number of simultaneous downloads in free mode";
        }

        public String getEnableFolderWorkaround_label() {
            return "Enable folder workaround?";
        }
    }

    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    @AboutConfig
    @DefaultBooleanValue(false)
    @Order(10)
    @TakeValueFromSubconfig("EXPERIMENTALHANDLING")
    @DescriptionForConfigEntry("This may avoid unnecessary captchas when an IP limit is reached in free download mode.")
    boolean isEnableReconnectWorkaround();

    void setEnableReconnectWorkaround(boolean b);

    @AboutConfig
    @Order(20)
    @TakeValueFromSubconfig("CUSTOM_REFERER")
    @DescriptionForConfigEntry("Define custom referer value to be used.")
    String getReferer();

    void setReferer(String referer);

    @AboutConfig
    @DefaultBooleanValue(false)
    @Order(25)
    @DescriptionForConfigEntry("Always use custom referer even if added URL contains another referer.")
    boolean isForceCustomReferer();

    void setForceCustomReferer(boolean b);

    @AboutConfig
    @DefaultIntValue(1)
    @SpinnerValidator(min = 1, max = 20, step = 1)
    @Order(40)
    int getMaxSimultaneousFreeDownloads();

    void setMaxSimultaneousFreeDownloads(int maxFree);

    @AboutConfig
    @DefaultBooleanValue(false)
    @Order(50)
    @DescriptionForConfigEntry("Enable this if you want JDownloader to be able to handle folders which are added as single file URLs in with the pattern /http.../file/...'.")
    boolean isEnableFolderWorkaround();

    void setEnableFolderWorkaround(boolean b);
}