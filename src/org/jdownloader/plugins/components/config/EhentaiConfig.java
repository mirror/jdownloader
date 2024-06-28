package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultOnNull;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.TakeValueFromSubconfig;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "e-hentai.org", type = Type.HOSTER)
public interface EhentaiConfig extends PluginConfigInterface {
    public static final TRANSLATION TRANSLATION                                = new TRANSLATION();
    final String                    text_AccountDownloadsPreferOriginalQuality = "Account downloads: Prefer original quality (better quality, counts towards image points limit)?";
    final String                    text_PreferOriginalFilename                = "Prefer original file name?";
    final String                    text_GalleryCrawlMode                      = "Gallery crawl mode";

    public static class TRANSLATION {
        public String getAccountDownloadsPreferOriginalQuality_label() {
            return text_AccountDownloadsPreferOriginalQuality;
        }

        public String getPreferOriginalFilename_label() {
            return text_PreferOriginalFilename;
        }

        public String getGalleryCrawlMode_label() {
            return text_GalleryCrawlMode;
        }
    }

    @DefaultBooleanValue(true)
    @TakeValueFromSubconfig("PREFER_ORIGINAL_QUALITY") // Legacy compatibility
    @AboutConfig
    @DescriptionForConfigEntry(text_AccountDownloadsPreferOriginalQuality)
    @Order(20)
    boolean isAccountDownloadsPreferOriginalQuality();

    void setAccountDownloadsPreferOriginalQuality(boolean b);

    @DefaultBooleanValue(true)
    @TakeValueFromSubconfig("PREFER_ORIGINAL_FILENAME") // Legacy compatibility
    @AboutConfig
    @DescriptionForConfigEntry(text_PreferOriginalFilename)
    @Order(30)
    boolean isPreferOriginalFilename();

    void setPreferOriginalFilename(boolean b);

    public static enum GalleryCrawlMode implements LabelInterface {
        ZIP_AND_IMAGES {
            @Override
            public String getLabel() {
                return "Crawl original zip archive and images";
            }
        },
        ZIP_ONLY {
            @Override
            public String getLabel() {
                return "Crawl zip archive only";
            }
        },
        ZIP_DISABLED {
            @Override
            public String getLabel() {
                return "Do not crawl original zip archive: Crawl images only";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("ZIP_AND_IMAGES")
    @Order(40)
    @DescriptionForConfigEntry(text_GalleryCrawlMode)
    @DefaultOnNull
    GalleryCrawlMode getGalleryCrawlMode();

    void setGalleryCrawlMode(final GalleryCrawlMode mode);
}