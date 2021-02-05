package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "anonfiles.com", type = Type.HOSTER)
public interface AnonFilesComConfig extends PluginConfigInterface {
    @AboutConfig
    @DescriptionForConfigEntry("Set max simultaneous free downloads")
    @DefaultIntValue(2)
    @SpinnerValidator(min = 1, max = 20, step = 1)
    @Order(10)
    int getMaxSimultaneousFreeDownloads();

    void setMaxSimultaneousFreeDownloads(int maxFree);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Allow fallback to lower quality video download if download of original quality fails?")
    @Order(20)
    boolean isAllowFallbackToLowerQuality();

    void setAllowFallbackToLowerQuality(boolean b);
}