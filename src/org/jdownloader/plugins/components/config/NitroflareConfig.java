package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.TakeValueFromSubconfig;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "nitroflare.com", type = Type.HOSTER)
public interface NitroflareConfig extends PluginConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("allowMultipleFreeDownloads")
    @DescriptionForConfigEntry("Allow multiple free downloads?\r\nThis might result in fatal errors!")
    @Order(10)
    boolean isAllowMultipleFreeDownloads();

    void setAllowMultipleFreeDownloads(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @TakeValueFromSubconfig("trustAPIPremiumOnly")
    @DescriptionForConfigEntry("Trust API about Premium Only flag?")
    @Order(20)
    boolean isTrustAPIAboutPremiumOnlyFlag();

    void setTrustAPIAboutPremiumOnlyFlag(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Use API for account check and premium account downloading [recommended]?")
    @Order(30)
    boolean isUsePremiumAPIEnabled();

    void setUsePremiumAPIEnabled(boolean b);
    /** 2020-07-03: Doesn't work (yet) thus I've removed this setting RE: psp */
    // @AboutConfig
    // @DefaultBooleanValue(false)
    // @DescriptionForConfigEntry("Use API for free- and free account downloading?")
    // @Order(40)
    // boolean isUseFreeAPIEnabled();
    //
    // void setUseFreeAPIEnabled(boolean b);
}