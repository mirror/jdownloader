package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "8muses.com", type = Type.HOSTER)
public interface EightMusesComConfig extends PluginConfigInterface {
    public static enum CrawlMode implements LabelInterface {
        SINGLE_PAGE {
            @Override
            public String getLabel() {
                return "Always only crawl the added page";
            }
        },
        ALL_PAGES {
            @Override
            public String getLabel() {
                return "Crawl all pages starting from the page added";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("SINGLE_PAGE")
    @DescriptionForConfigEntry("Select preferred HLS download quality. If your preferred HLS quality is not found, best quality will be downloaded instead.")
    @Order(10)
    CrawlMode getCrawlMode();

    void setCrawlMode(CrawlMode quality);
}