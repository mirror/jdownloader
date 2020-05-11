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
            return "Enable reconnect workaround for free mode (only for API[=default] mode)?";
        }

        public String getReferer_label() {
            return "Set custom Referer here";
        }

        public String getForceCustomReferer_label() {
            return "Force custom Referer even if Referer is given in URL via '?site=http://...'?";
        }

        public String getEnableSSL_label() {
            return "Use Secure Communication over SSL (HTTPS://)";
        }

        public String getMaxSimultaneousFreeDownloads_label() {
            return "Set max. number of simultaneous downloads in free mode";
        }
    }

    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    @AboutConfig
    @DefaultBooleanValue(false)
    @Order(10)
    @TakeValueFromSubconfig("EXPERIMENTALHANDLING")
    boolean isEnableReconnectWorkaround();

    void setEnableReconnectWorkaround(boolean b);

    @AboutConfig
    @Order(20)
    @TakeValueFromSubconfig("CUSTOM_REFERER")
    @DescriptionForConfigEntry("By default, source URL will be used as Referer which means most of the time no Referer will be used.")
    String getReferer();

    void setReferer(String referer);

    @AboutConfig
    @DefaultBooleanValue(false)
    @Order(25)
    boolean isForceCustomReferer();

    void setForceCustomReferer(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @Order(30)
    @TakeValueFromSubconfig("SSL_CONNECTION_2")
    boolean isEnableSSL();

    void setEnableSSL(boolean b);

    @AboutConfig
    @DefaultIntValue(1)
    @SpinnerValidator(min = 1, max = 20, step = 1)
    @Order(40)
    int getMaxSimultaneousFreeDownloads();

    void setMaxSimultaneousFreeDownloads(int maxFree);
}