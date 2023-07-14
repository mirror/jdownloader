package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;

public interface XFSConfigVideo extends XFSConfig {
    final String                    text_PreferredStreamQuality   = "Preferred stream download quality";
    final String                    text_PreferredDownloadQuality = "Preferred original download quality";
    final String                    text_PreferredDownloadMode    = "Preferred download mode";
    public static final TRANSLATION TRANSLATION                   = new TRANSLATION();

    public static class TRANSLATION {
        public String getPreferredStreamQuality_label() {
            return text_PreferredStreamQuality;
        }

        public String getPreferredDownloadQuality_label() {
            return text_PreferredDownloadQuality;
        }

        public String getPreferredDownloadMode_label() {
            return text_PreferredDownloadMode;
        }
    }

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

    @AboutConfig
    @DefaultEnumValue("BEST")
    @DescriptionForConfigEntry(text_PreferredStreamQuality)
    @Order(100)
    PreferredStreamQuality getPreferredStreamQuality();

    void setPreferredStreamQuality(PreferredStreamQuality quality);

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
    @DescriptionForConfigEntry(text_PreferredDownloadQuality)
    @Order(120)
    PreferredDownloadQuality getPreferredDownloadQuality();

    void setPreferredDownloadQuality(PreferredDownloadQuality quality);

    public static enum DownloadMode implements LabelInterface {
        ORIGINAL {
            @Override
            public String getLabel() {
                return "Prefer official file download";
            }
        },
        STREAM {
            @Override
            public String getLabel() {
                return "Prefer stream download";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("ORIGINAL")
    @DescriptionForConfigEntry(text_PreferredDownloadMode)
    @Order(130)
    DownloadMode getPreferredDownloadMode();

    void setPreferredDownloadMode(DownloadMode mode);
}