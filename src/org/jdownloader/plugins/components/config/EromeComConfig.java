package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "erome.com", type = Type.CRAWLER)
public interface EromeComConfig extends PluginConfigInterface {
    final String                    text_AddThumbnail = "Add thumbnail for videos?";
    public static final TRANSLATION TRANSLATION       = new TRANSLATION();

    public static class TRANSLATION {
        public String getAddThumbnail_label() {
            return text_AddThumbnail;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_AddThumbnail)
    @Order(10)
    boolean isAddThumbnail();

    void setAddThumbnail(boolean b);
}