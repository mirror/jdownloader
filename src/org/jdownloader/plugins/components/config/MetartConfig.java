package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "metart.com", type = Type.HOSTER)
public interface MetartConfig extends PluginConfigInterface {
    public static enum PhotoCrawlMode implements LabelInterface {
        ZIP_BEST {
            @Override
            public String getLabel() {
                return ".zip BEST quality";
            }
        },
        PHOTOS_BEST {
            @Override
            public String getLabel() {
                return "Loose photos BEST quality";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("PHOTOS_BEST")
    @Order(20)
    @DescriptionForConfigEntry("Select preferred photo crawl mode")
    PhotoCrawlMode getPhotoCrawlMode();

    void setPhotoCrawlMode(PhotoCrawlMode pcm);
}
