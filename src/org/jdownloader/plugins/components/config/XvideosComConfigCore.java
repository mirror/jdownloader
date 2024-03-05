package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.TakeValueFromSubconfig;

public interface XvideosComConfigCore extends PluginConfigInterface {
    final String                    text_EnableFastLinkcheckForHostPlugin = "Enable fast linkcheck for host plugin? If enabled, filesize won't be displayed until download is started!";
    final String                    text_PreferHLSStreamDownload          = "Prefer HLS download?";
    final String                    text_PreferredHLSQuality              = "Select preferred HLS download quality. If your preferred HLS quality is not found, best quality will be downloaded instead.";
    final String                    text_PreferredHTTPQuality             = "Select preferred HTTP download quality. If your preferred HTTP quality is not found, best quality will be downloaded instead.";
    final String                    text_TryToRecognizeLimit              = "Try to recognize limit?";
    final String                    text_PreferredOfficialDownloadQuality = "Select preferred official download quality ('download' button). If your preferred quality is not found, best quality will be downloaded instead.";
    final String                    text_PluginContentURLExposeDirecturls = "Display video direct-URLs in GUI as 'Custom URL' if available?";
    public static final TRANSLATION TRANSLATION                           = new TRANSLATION();

    public static class TRANSLATION {
        public String getEnableFastLinkcheckForHostPlugin_label() {
            return text_EnableFastLinkcheckForHostPlugin;
        }

        public String getPreferHLSStreamDownload_label() {
            return text_PreferHLSStreamDownload;
        }

        public String getPreferredHLSQuality_label() {
            return text_PreferredHLSQuality;
        }

        public String getPreferredHTTPQuality_label() {
            return text_PreferredHTTPQuality;
        }

        public String getPreferredOfficialDownloadQuality_label() {
            return text_PreferredOfficialDownloadQuality;
        }

        public String getTryToRecognizeLimit_label() {
            return text_TryToRecognizeLimit;
        }

        public String getPluginContentURLExposeDirecturls_label() {
            return text_PluginContentURLExposeDirecturls;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_EnableFastLinkcheckForHostPlugin)
    @Order(15)
    boolean isEnableFastLinkcheckForHostPlugin();

    void setEnableFastLinkcheckForHostPlugin(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @TakeValueFromSubconfig("Prefer HLS")
    @DescriptionForConfigEntry(text_PreferHLSStreamDownload)
    @Order(30)
    boolean isPreferHLSStreamDownload();

    void setPreferHLSStreamDownload(boolean b);

    public static enum PreferredHLSQuality implements LabelInterface {
        Q2160P {
            @Override
            public String getLabel() {
                return "2160p (4k)";
            }
        },
        Q1080P {
            @Override
            public String getLabel() {
                return "1080p";
            }
        },
        Q720P {
            @Override
            public String getLabel() {
                return "720p";
            }
        },
        Q480P {
            @Override
            public String getLabel() {
                return "480p";
            }
        },
        Q360P {
            @Override
            public String getLabel() {
                return "360p";
            }
        };
    }

    public static enum PreferredHTTPQuality implements LabelInterface {
        HIGH {
            @Override
            public String getLabel() {
                return "High quality";
            }
        },
        LOW {
            @Override
            public String getLabel() {
                return "Low quality";
            }
        };
    }

    public static enum PreferredOfficialDownloadQuality implements LabelInterface {
        Q2160P {
            @Override
            public String getLabel() {
                return "2160p (4k)";
            }
        },
        Q1080P {
            @Override
            public String getLabel() {
                return "1080p";
            }
        },
        Q360P {
            @Override
            public String getLabel() {
                return "360p";
            }
        },
        Q240P {
            @Override
            public String getLabel() {
                return "240p";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("Q2160P")
    @DescriptionForConfigEntry(text_PreferredHLSQuality)
    @Order(100)
    PreferredHLSQuality getPreferredHLSQuality();

    void setPreferredHLSQuality(PreferredHLSQuality quality);

    @AboutConfig
    @DefaultEnumValue("HIGH")
    @DescriptionForConfigEntry(text_PreferredHTTPQuality)
    @Order(120)
    PreferredHTTPQuality getPreferredHTTPQuality();

    void setPreferredHTTPQuality(PreferredHTTPQuality quality);

    @AboutConfig
    @DefaultEnumValue("Q2160P")
    @DescriptionForConfigEntry(text_PreferredOfficialDownloadQuality)
    @Order(130)
    PreferredOfficialDownloadQuality getPreferredOfficialDownloadQuality();

    void setPreferredOfficialDownloadQuality(PreferredOfficialDownloadQuality quality);

    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("xvideos.com/xvideos.red/xnxx.cc can 'shadow ban' users who download a lot. This will limit the max. available quality to 240p. This experimental setting will make JD try to detect this limit.")
    @Order(140)
    boolean isTryToRecognizeLimit();

    void setTryToRecognizeLimit(boolean b);

    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_PluginContentURLExposeDirecturls)
    @Order(150)
    boolean isPluginContentURLExposeDirecturls();

    void setPluginContentURLExposeDirecturls(boolean b);
}