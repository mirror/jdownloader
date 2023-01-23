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

@PluginHost(host = "erome.com", type = Type.CRAWLER)
public interface EromeComConfig extends PluginConfigInterface {
    final String                    text_AddThumbnail             = "Add thumbnail for videos?";
    final String                    text_MaxSimultaneousDownloads = "Max simultaneous downloads (values greater than 5 may cause errors!)";
    public static final TRANSLATION TRANSLATION                   = new TRANSLATION();

    public static class TRANSLATION {
        public String getAddThumbnail_label() {
            return text_AddThumbnail;
        }

        public String getMaxSimultaneousDownloads_label() {
            return text_MaxSimultaneousDownloads;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_AddThumbnail)
    @Order(10)
    boolean isAddThumbnail();

    void setAddThumbnail(boolean b);

    @AboutConfig
    @SpinnerValidator(min = 1, max = 20, step = 1)
    @DefaultIntValue(5)
    @DescriptionForConfigEntry(text_MaxSimultaneousDownloads)
    @Order(20)
    int getMaxSimultaneousDownloads();

    void setMaxSimultaneousDownloads(int i);
}