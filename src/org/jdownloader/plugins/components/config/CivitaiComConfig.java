package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "civitai.com", type = Type.HOSTER)
public interface CivitaiComConfig extends PluginConfigInterface {
    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    public static class TRANSLATION {
        public String getProfileCrawlerMaxPaginationItems_label() {
            return "Profile crawler: Pagination size";
        }

        public String getProfileCrawlerPaginationSleepMillis_label() {
            return "Profile crawler: Wait time between pagination requests";
        }
    }

    @AboutConfig
    @SpinnerValidator(min = 10, max = 200, step = 1)
    @DefaultIntValue(10)
    @DescriptionForConfigEntry("Internal value to limit max number of items per page. Lower value = More requests needed to crawl a profile. See: https://github.com/civitai/civitai/wiki/REST-API-Reference#get-apiv1images")
    @Order(10)
    int getProfileCrawlerMaxPaginationItems();

    void setProfileCrawlerMaxPaginationItems(int num);

    @AboutConfig
    @SpinnerValidator(min = 0, max = 20000, step = 1000)
    @DefaultIntValue(1000)
    @DescriptionForConfigEntry("Wait time between pagination requests.")
    @Order(20)
    int getProfileCrawlerPaginationSleepMillis();

    void setProfileCrawlerPaginationSleepMillis(int millis);
}