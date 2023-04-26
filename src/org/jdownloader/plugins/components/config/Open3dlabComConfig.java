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

@PluginHost(host = "open3dlab.com", type = Type.HOSTER)
public interface Open3dlabComConfig extends PluginConfigInterface {
    final String                    text_MirrorPriorityString        = "Define priority of mirrors e.g. 'ams1, us'.";
    final String                    text_MirrorFallbackMode          = "What to do if none of the preferred mirrors are found?";
    final String                    text_CrawlThumbnail              = "Crawl thumbnail from right side of '/project/...' page?";
    final String                    text_CrawlPreviewSlashPromoMedia = "Crawl preview/promo media from middle of '/project/...' page?";
    public static final TRANSLATION TRANSLATION                      = new TRANSLATION();

    public static class TRANSLATION {
        public String getMirrorPriorityString_label() {
            return text_MirrorPriorityString;
        }

        public String getMirrorFallbackMode_label() {
            return text_MirrorFallbackMode;
        }

        public String getCrawlPreviewSlashPromoMedia_label() {
            return text_CrawlPreviewSlashPromoMedia;
        }
    }

    @AboutConfig
    @DescriptionForConfigEntry(text_MirrorPriorityString)
    @Order(10)
    String getMirrorPriorityString();

    void setMirrorPriorityString(String str);

    public static enum MirrorFallbackMode implements LabelInterface {
        ONE {
            @Override
            public String getLabel() {
                return "Return one random mirror";
            }
        },
        ALL {
            @Override
            public String getLabel() {
                return "Return all mirrors";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("ONE")
    @Order(20)
    @DescriptionForConfigEntry(text_MirrorFallbackMode)
    MirrorFallbackMode getMirrorFallbackMode();

    void setMirrorFallbackMode(final MirrorFallbackMode mode);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlThumbnail)
    @Order(30)
    boolean isCrawlThumbnail();

    void setCrawlThumbnail(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlPreviewSlashPromoMedia)
    @Order(40)
    boolean isCrawlPreviewSlashPromoMedia();

    void setCrawlPreviewSlashPromoMedia(boolean b);
}