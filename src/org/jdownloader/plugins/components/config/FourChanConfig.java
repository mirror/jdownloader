package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "boards.4chan.org", type = Type.CRAWLER)
public interface FourChanConfig extends PluginConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Prefer server filenames over plugins' default filenames?")
    @Order(30)
    boolean isPreferServerFilenamesOverPluginDefaultFilenames();

    void setPreferServerFilenamesOverPluginDefaultFilenames(boolean b);

    @AboutConfig
    @DefaultIntValue(1)
    @DescriptionForConfigEntry("How many pages should be crawled when adding a category containing threads?")
    @SpinnerValidator(min = 1, max = 20, step = 1)
    @Order(40)
    int getCategoryCrawlerPageLimit();

    void setCategoryCrawlerPageLimit(int pageLimit);
}