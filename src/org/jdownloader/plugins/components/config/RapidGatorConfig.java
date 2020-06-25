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
    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("EXPERIMENTALHANDLING")
    @DescriptionForConfigEntry("Activate experimental waittime handling to prevent 24-hours IP ban from rapidgator?")
    @Order(10)
    boolean isActivateExperimentalWaittimeHandling();

    void setActivateExperimentalWaittimeHandling(boolean b);

    /* Some users always get server error 500 via API thus website might work better for them. */
    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Enable API for premium downloads [recommended] (disabled = use website for premium downloads)?")
    @Order(20)
    boolean isEnableAPIPremium();

    void setEnableAPIPremium(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("EXPERIMENTAL_ENFORCE_SSL")
    @DescriptionForConfigEntry("Activate experimental forced SSL for downloads?")
    @Order(30)
    boolean isExperimentalEnforceSSL();

    void setExperimentalEnforceSSL(boolean b);

    @AboutConfig
    @TakeValueFromSubconfig("CUSTOM_REFERER")
    @DescriptionForConfigEntry("Define custom referer")
    @Order(40)
    String getReferer();

    void setReferer(String str);

    @AboutConfig
    @DefaultIntValue(120)
    @SpinnerValidator(min = 1, max = 300, step = 1)
    @Order(50)
    @DescriptionForConfigEntry("Define custom browser read-timeout (seconds)")
    int getReadTimeout();

    void setReadTimeout(int i);
}