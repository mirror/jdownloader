package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "pixiv.net", type = Type.HOSTER)
public interface PixivNetConfig extends PluginConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Crawl animations metadata as .json file?")
    @Order(20)
    boolean isCrawlAnimationsMetadata();

    void setCrawlAnimationsMetadata(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Crawl user works individually?")
    @Order(30)
    boolean isCrawlUserWorksIndividually();

    void setCrawlUserWorksIndividually(boolean b);
}