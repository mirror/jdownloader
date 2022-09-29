package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "webshare.cz", type = Type.HOSTER)
public interface WebshareCzConfig extends PluginConfigInterface {
    final String              TEXT_MaxSimultaneousFreeOrFreeAccountDownloads = "Max simultaneous for free- and free-account downloads";
    public static TRANSLATION TRANSLATION                                    = new TRANSLATION();

    public static class TRANSLATION {
        public String getMaxSimultaneousFreeOrFreeAccountDownloads_label() {
            return TEXT_MaxSimultaneousFreeOrFreeAccountDownloads;
        }
    }

    /** 2022-09-28: Set default to 5 as according to my tests it looks like more than 5 is not possible. */
    @AboutConfig
    @DefaultIntValue(5)
    @SpinnerValidator(min = 1, max = 20, step = 1)
    @Order(10)
    @DescriptionForConfigEntry(TEXT_MaxSimultaneousFreeOrFreeAccountDownloads)
    int getMaxSimultaneousFreeOrFreeAccountDownloads();

    void setMaxSimultaneousFreeOrFreeAccountDownloads(int maxdls);
}
