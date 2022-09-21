package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "flimmit.com", type = Type.HOSTER)
public interface FlimmitComConfig extends PluginConfigInterface {
    final String                    text_PreferBest    = "Only add best quality?";
    // final String text_CrawlThumbnail = "Crawl thumbnail?";
    final String                    text_CrawlSubtitle = "Crawl subtitle?";
    final String                    text_CrawlPoster   = "Crawl poster?";
    public static final TRANSLATION TRANSLATION        = new TRANSLATION();

    public static class TRANSLATION {
        public String getPreferBest_label() {
            return text_PreferBest;
        }
        // public String getCrawlThumbnail_label() {
        // return text_CrawlThumbnail;
        // }

        public String getCrawlSubtitle_label() {
            return text_CrawlSubtitle;
        }

        public String getCrawlPoster_label() {
            return text_CrawlPoster;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_PreferBest)
    @Order(20)
    boolean isPreferBest();

    void setPreferBest(boolean b);
    // @AboutConfig
    // @DefaultBooleanValue(true)
    // @DescriptionForConfigEntry(text_CrawlThumbnail)
    // @Order(30)
    // boolean isCrawlThumbnail();
    //
    // void setCrawlThumbnail(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlSubtitle)
    @Order(40)
    boolean isCrawlSubtitle();

    void setCrawlSubtitle(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_CrawlPoster)
    @Order(50)
    boolean isCrawlPoster();

    void setCrawlPoster(boolean b);
}