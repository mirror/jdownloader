package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "flash-files.com", type = Type.HOSTER)
public interface FlashfilesComConfig extends PluginConfigInterface {
    final String              text_MaxSimultaneousFreeDownloads = "Max simultaneous free downloads";
    public static TRANSLATION TRANSLATION                       = new TRANSLATION();

    public static class TRANSLATION {
        public String getMaxSimultaneousFreeDownloads_label() {
            return text_MaxSimultaneousFreeDownloads;
        }
    }

    @AboutConfig
    @DefaultIntValue(1)
    @SpinnerValidator(min = 1, max = 20, step = 1)
    @Order(10)
    @DescriptionForConfigEntry(text_MaxSimultaneousFreeDownloads)
    int getMaxSimultaneousFreeDownloads();

    void setMaxSimultaneousFreeDownloads(int maxNumberofFreeDownloads);
}
