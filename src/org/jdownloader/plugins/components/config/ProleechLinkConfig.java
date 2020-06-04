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

@PluginHost(host = "proleech.link", type = Type.HOSTER)
public interface ProleechLinkConfig extends PluginConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Enable API only mode (2020-06-04: Warning! Still under development!)")
    @Order(30)
    boolean isEnableBetaAPIOnly();

    void setEnableBetaAPIOnly(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Delete download history after every successful download and on every account check (all successfully downloaded entries & all older than 24 hours)? [Does NOT work in API only mode]")
    @Order(40)
    @TakeValueFromSubconfig("CLEAR_DOWNLOAD_HISTORY_AFTER_EACH_SUCCESSFUL_DOWNLOAD_AND_ON_ACCOUNTCHECK")
    boolean isClearDownloadHistoryAfterEachDownload();

    void setClearDownloadHistoryAfterEachDownload(boolean b);

    @AboutConfig
    @DefaultIntValue(0)
    @SpinnerValidator(min = 0, max = 72, step = 1)
    @Order(50)
    @TakeValueFromSubconfig("DOWNLOADLINK_GENERATION_LIMIT")
    @DescriptionForConfigEntry("Allow new downloadlink generation every X hours (default = 0 = unlimited/disabled)\r\nThis can save traffic but this can also slow down the download process [Does NOT work in API only mode]")
    int getAllowDownloadlinkGenerationOnlyEveryXHours();

    void setAllowDownloadlinkGenerationOnlyEveryXHours(int hours);
}