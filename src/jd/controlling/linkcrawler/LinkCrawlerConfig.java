package jd.controlling.linkcrawler;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;

public interface LinkCrawlerConfig extends ConfigInterface {

    @DefaultIntValue(12)
    @AboutConfig
    @RequiresRestart
    @DescriptionForConfigEntry("max. number of linkcrawler threads")
    int getMaxThreads();

    void setMaxThreads(int i);

    @DefaultIntValue(20000)
    @AboutConfig
    @RequiresRestart
    @DescriptionForConfigEntry("max. time in ms before killing an idle linkcrawler thread")
    int getThreadKeepAlive();

    void setThreadKeepAlive(int i);

    @DefaultIntValue(2 * 1024 * 1024)
    @AboutConfig
    @RequiresRestart
    @DescriptionForConfigEntry("max. bytes for page request during deep decrypt")
    @SpinnerValidator(min = 1 * 1024 * 1024, max = 5 * 1024 * 1024)
    int getDeepDecryptLoadLimit();

    void setDeepDecryptLoadLimit(int l);

}
