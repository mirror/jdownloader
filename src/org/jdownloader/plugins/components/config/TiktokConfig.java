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

@PluginHost(host = "tiktok.com", type = Type.HOSTER)
public interface TiktokConfig extends PluginConfigInterface {
    public static final TiktokConfig.TRANSLATION TRANSLATION = new TRANSLATION();

    public static class TRANSLATION {
        public String getEnableFastLinkcheck_label() {
            return "Enable fast linkcheck? If enabled, filenames may contain less information and filesize will be missing until download is started.";
        }

        public String getMaxSimultaneousDownloads_label() {
            return "Set max. simultaneous downloads. Don't set this value too much otherwise you might get blocked.";
        }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Enable fast linkcheck? If enabled, filenames may contain less information and filesize will be missing until download is started.")
    @Order(10)
    boolean isEnableFastLinkcheck();

    void setEnableFastLinkcheck(boolean b);

    @AboutConfig
    @DefaultIntValue(1)
    @SpinnerValidator(min = 1, max = 20, step = 1)
    @Order(20)
    @DescriptionForConfigEntry("Set max. simultaneous downloads. Don't set this value too much otherwise you might get blocked.")
    int getMaxSimultaneousDownloads();

    void setMaxSimultaneousDownloads(int i);
}