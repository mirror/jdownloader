package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "bayfiles.com", type = Type.HOSTER)
public interface AnonFilesComConfigBayfiles extends AnonFilesComConfig {
    @AboutConfig
    @DescriptionForConfigEntry("Set max simultaneous free downloads")
    @DefaultIntValue(4)
    @SpinnerValidator(min = 1, max = 20, step = 1)
    @Order(10)
    int getMaxSimultaneousFreeDownloads();

    void setMaxSimultaneousFreeDownloads(int maxFree);
}