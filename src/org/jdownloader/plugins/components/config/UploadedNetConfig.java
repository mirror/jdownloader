package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.TakeValueFromSubconfig;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "uploaded.net", type = Type.HOSTER)
public interface UploadedNetConfig extends PluginConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(false)
    @Order(10)
    @TakeValueFromSubconfig("ACTIVATEACCOUNTERRORHANDLING")
    @DescriptionForConfigEntry("Activate experimental free account errorhandling: Reconnect and switch between free accounts, also prevents having to enter additional captchas in between downloads.")
    boolean isEnableReconnectWorkaroundAccount();

    void setEnableReconnectWorkaroundAccount(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @Order(20)
    @TakeValueFromSubconfig("EXPERIMENTALHANDLING")
    @DescriptionForConfigEntry("Activate free download reconnect workaround for freeusers: Prevents having to enter additional captchas in between downloads.")
    boolean isEnableReconnectWorkaround();

    void setEnableReconnectWorkaround(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @Order(25)
    @TakeValueFromSubconfig("SSL_CONNECTION")
    @DescriptionForConfigEntry("Use Secure Communication over SSL (HTTPS://)?")
    boolean isUseSSL();

    void setUseSSL(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @Order(30)
    @TakeValueFromSubconfig("PREFER_PREMIUM_DOWNLOAD_API_V2")
    @DescriptionForConfigEntry("Use API in account mode? [Recommended] Disabled = Use website mode")
    boolean isUseAPIInAccountMode();

    void setUseAPIInAccountMode(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @Order(40)
    @TakeValueFromSubconfig("DOWNLOAD_ABUSED")
    @DescriptionForConfigEntry("Activate download of DMCA blocked links? You need to deactivate the API in order for this to have an effect!\r\n-This function enables uploaders to download their own links which have a 'legacy takedown' status till uploaded irrevocably deletes them\r\nNote the following:\r\n-When activated, links which have the public status 'offline' will get an 'uncheckable' status instead\r\n--> If they're still downloadable, their filename- and size will be shown on downloadstart\r\n--> If they're really offline, the correct (offline) status will be shown on downloadstart.")
    boolean isAllowDMCAAbusedDownload();

    void setAllowDMCAAbusedDownload(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @Order(50)
    @TakeValueFromSubconfig("DISABLE_START_INTERVAL")
    @DescriptionForConfigEntry("Disable start interval? Warning: This may cause IP blocks from uploaded.net!")
    boolean isDisableStartInterval();

    void setDisableStartInterval(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @Order(60)
    @TakeValueFromSubconfig("EXPERIMENTAL_MULTIFREE2")
    @DescriptionForConfigEntry("Allow multiple free downloads? Warning! Experimental! This can lead to fatal errors and unnecessary captchas!")
    boolean isAllowUnlimitedFreeDownloads();

    void setAllowUnlimitedFreeDownloads(boolean b);
}