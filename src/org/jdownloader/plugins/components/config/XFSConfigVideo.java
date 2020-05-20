package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;

public interface XFSConfigVideo extends XFSConfig {
    public static enum PreferredStreamQuality implements LabelInterface {
        BEST {
            @Override
            public String getLabel() {
                return "Best";
            }
        },
        Q360P {
            @Override
            public String getLabel() {
                return "360p";
            }
        },
        Q480P {
            @Override
            public String getLabel() {
                return "480p";
            }
        },
        Q720P {
            @Override
            public String getLabel() {
                return "720p";
            }
        },
        Q1080P {
            @Override
            public String getLabel() {
                return "1080p";
            }
        },
        Q2160P {
            @Override
            public String getLabel() {
                return "2160p (4k)";
            }
        };
    }

    public static enum PreferredDownloadQuality implements LabelInterface {
        BEST {
            @Override
            public String getLabel() {
                return "Original/Best";
            }
        },
        HIGH {
            @Override
            public String getLabel() {
                return "High quality";
            }
        },
        NORMAL {
            @Override
            public String getLabel() {
                return "Normal quality";
            }
        },
        LOW {
            @Override
            public String getLabel() {
                return "Low quality";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("BEST")
    @DescriptionForConfigEntry("If your preferred stream quality is not found, best quality will be downloaded instead.")
    @Order(100)
    PreferredStreamQuality getPreferredStreamQuality();

    void setPreferredStreamQuality(PreferredStreamQuality quality);

    @AboutConfig
    @DefaultEnumValue("BEST")
    @DescriptionForConfigEntry("If your preferred download quality is not found, best quality will be downloaded instead.")
    @Order(120)
    PreferredDownloadQuality getPreferredDownloadQuality();

    void setPreferredDownloadQuality(PreferredDownloadQuality quality);
}