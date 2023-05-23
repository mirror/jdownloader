package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "uptobox.com", type = Type.HOSTER)
public interface UpToBoxComConfig extends PluginConfigInterface {
    final String                    text_PreferredQuality     = "If your preferred quality is not found, original/best will be downloaded instead. Only works for content also available on uptostream! Only works if you own a premium account!";
    final String                    text_GrabSubtitle         = "[Premium only] Crawl subtitle?";
    final String                    text_UseHTTPSForDownloads = "Use https for final downloadurls?";
    final String                    text_PreferredDomain      = "Preferred domain";
    final String                    text_CustomDomain         = "Define custom preferred domain. If given this will be preferred over the above selection";
    public static final TRANSLATION TRANSLATION               = new TRANSLATION();

    public static class TRANSLATION {
        public String getPreferredQuality_label() {
            return text_PreferredQuality;
        }

        public String getGrabSubtitle_label() {
            return text_GrabSubtitle;
        }

        public String getUseHTTPSForDownloads_label() {
            return text_UseHTTPSForDownloads;
        }

        public String getPreferredDomain_label() {
            return text_PreferredDomain;
        }

        public String getCustomDomain_label() {
            return text_CustomDomain;
        }
    }

    public static enum PreferredQuality implements LabelInterface {
        DEFAULT {
            @Override
            public String getLabel() {
                return "Source/Original/Best";
            }
        },
        QUALITY1 {
            @Override
            public String getLabel() {
                return "360p";
            }
        },
        QUALITY2 {
            @Override
            public String getLabel() {
                return "480p";
            }
        },
        QUALITY3 {
            @Override
            public String getLabel() {
                return "720p";
            }
        },
        QUALITY4 {
            @Override
            public String getLabel() {
                return "1080p";
            }
        },
        QUALITY5 {
            @Override
            public String getLabel() {
                return "2160p (4k)";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DEFAULT")
    @DescriptionForConfigEntry(text_PreferredQuality)
    @Order(40)
    PreferredQuality getPreferredQuality();

    void setPreferredQuality(PreferredQuality domain);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_GrabSubtitle)
    @Order(50)
    boolean isGrabSubtitle();

    void setGrabSubtitle(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_UseHTTPSForDownloads)
    @Order(60)
    boolean isUseHTTPSForDownloads();

    void setUseHTTPSForDownloads(boolean b);

    public static enum PreferredDomain implements LabelInterface {
        DEFAULT {
            @Override
            public String getLabel() {
                return "default (= uptobox.eu)";
            }
        },
        DOMAIN1 {
            @Override
            public String getLabel() {
                return "uptobox.com";
            }
        },
        DOMAIN2 {
            @Override
            public String getLabel() {
                return "uptobox.fr";
            }
        },
        DOMAIN3 {
            @Override
            public String getLabel() {
                return "uptostream.com";
            }
        },
        DOMAIN4 {
            @Override
            public String getLabel() {
                return "uptostream.fr";
            }
        },
        DOMAIN5 {
            @Override
            public String getLabel() {
                return "uptostream.eu";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DEFAULT")
    @DescriptionForConfigEntry(text_PreferredDomain)
    @Order(70)
    PreferredDomain getPreferredDomain();

    void setPreferredDomain(PreferredDomain domain);

    @AboutConfig
    // @DefaultStringValue("")
    @DescriptionForConfigEntry(text_CustomDomain)
    @Order(80)
    String getCustomDomain();

    void setCustomDomain(String str);
}