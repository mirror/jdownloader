package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "recurbate.com", type = Type.HOSTER)
public interface RecurbateComConfig extends PluginConfigInterface {
    final String              TEXT_MaxSimultanPaidAccountDownloads = "Max simultaneous downloads for paid account downloads:";
    public static TRANSLATION TRANSLATION                          = new TRANSLATION();

    public static class TRANSLATION {
        public String getMaxSimultanPaidAccountDownloads_label() {
            return TEXT_MaxSimultanPaidAccountDownloads;
        }
    }

    @AboutConfig
    @DefaultIntValue(1)
    @SpinnerValidator(min = 1, max = 10, step = 1)
    @Order(10)
    @DescriptionForConfigEntry(TEXT_MaxSimultanPaidAccountDownloads)
    /* 2022-11-11: Default = 1 because starting 10 downloads at the same time triggered Cloudflare pretty fast. */
    int getMaxSimultanPaidAccountDownloads();

    void setMaxSimultanPaidAccountDownloads(int number);
}
