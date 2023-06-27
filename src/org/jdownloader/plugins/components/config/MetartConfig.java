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
    final String              text_PhotoCrawlMode = "Photo crawl mode";
    final String              text_VideoCrawlMode = "Video crawl mode";
    public static TRANSLATION TRANSLATION         = new TRANSLATION();

    public static class TRANSLATION {
        public String getPhotoCrawlMode_label() {
            return text_PhotoCrawlMode;
        }

        public String getVideoCrawlMode_label() {
            return text_VideoCrawlMode;
        }
    }

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
    @DescriptionForConfigEntry(text_PhotoCrawlMode)
    PhotoCrawlMode getPhotoCrawlMode();

    void setPhotoCrawlMode(PhotoCrawlMode mode);

    public static enum VideoCrawlMode implements LabelInterface {
        ALL {
            @Override
            public String getLabel() {
                return "All qualities";
            }
        },
        BEST {
            @Override
            public String getLabel() {
                return "Best quality only";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("ALL")
    @Order(30)
    @DescriptionForConfigEntry(text_VideoCrawlMode)
    VideoCrawlMode getVideoCrawlMode();

    void setVideoCrawlMode(VideoCrawlMode mode);
}
