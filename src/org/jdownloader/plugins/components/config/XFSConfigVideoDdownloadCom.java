package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.TakeValueFromSubconfig;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "ddownload.com", type = Type.HOSTER)
public interface XFSConfigVideoDdownloadCom extends XFSConfigVideo {
    @AboutConfig
    @DefaultIntValue(1)
    @SpinnerValidator(min = 1, max = 10, step = 1)
    @Order(40)
    @TakeValueFromSubconfig("MaxSimultaneousDownloads_LIMIT_2019_06")
    @DescriptionForConfigEntry("Max. simultaneous downloads (Free & Free account)")
    int getMaxSimultaneousFreeDownloads();

    void setMaxSimultaneousFreeDownloads(int maxFree);
}