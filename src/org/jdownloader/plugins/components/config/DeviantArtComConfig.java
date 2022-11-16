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

@PluginHost(host = "deviantart.com", type = Type.HOSTER)
public interface DeviantArtComConfig extends PluginConfigInterface {
    final String                    text_FastLinkcheckForSingleItems = "Fast linkcheck for single files (filesize might not be shown until dl is started)?";
    // final String text_PreferServerFilename = "Prefer server filename?";
    final String                    text_DownloadMode                = "Download mode:";
    public static final TRANSLATION TRANSLATION                      = new TRANSLATION();

    public static class TRANSLATION {
        public String getFastLinkcheckForSingleItems_label() {
            return text_FastLinkcheckForSingleItems;
        }
        // public String getPreferServerFilename_label() {
        // return text_PreferServerFilename;
        // }

        public String getDownloadMode_label() {
            return text_DownloadMode;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("SKIP_FILESIZECHECK") // backward compatibility
    @DescriptionForConfigEntry(text_FastLinkcheckForSingleItems)
    @Order(10)
    boolean isFastLinkcheckForSingleItems();

    void setFastLinkcheckForSingleItems(boolean b);
    // @AboutConfig
    // @DefaultBooleanValue(false)
    // @TakeValueFromSubconfig("FilenameFromServer") // backward compatibility
    // @DescriptionForConfigEntry(text_PreferServerFilename)
    // @Order(20)
    // boolean isPreferServerFilename();
    //
    // void setPreferServerFilename(boolean b);

    public static enum DownloadMode implements LabelInterface {
        OFFICIAL_DOWNLOAD_ELSE_PREVIEW {
            @Override
            public String getLabel() {
                return "Prefer official download, download preview as fallback";
            }
        },
        OFFICIAL_DOWNLOAD_ONLY {
            @Override
            public String getLabel() {
                return "Official download only (account required)";
            }
        },
        HTML {
            @Override
            public String getLabel() {
                return "Download HTML of webpage instead of media";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("OFFICIAL_DOWNLOAD_ELSE_PREVIEW")
    @Order(30)
    @DescriptionForConfigEntry(text_DownloadMode)
    @DefaultOnNull
    DownloadMode getDownloadMode();

    void setDownloadMode(final DownloadMode mediaQualityDownloadMode);
}