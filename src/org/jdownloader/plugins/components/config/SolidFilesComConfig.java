package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "solidfiles.com", type = Type.HOSTER)
public interface SolidFilesComConfig extends PluginConfigInterface {
    public static final SolidFilesComConfig.TRANSLATION TRANSLATION                       = new TRANSLATION();
    final String                                        text_FolderCrawlerCrawlSubfolders = "Folder crawler: Crawl subfolders?";

    public static class TRANSLATION {
        public String getCrawlSubfolders_label() {
            return text_FolderCrawlerCrawlSubfolders;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_FolderCrawlerCrawlSubfolders)
    @Order(10)
    boolean isFolderCrawlerCrawlSubfolders();

    void setFolderCrawlerCrawlSubfolders(boolean b);
}