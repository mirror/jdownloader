package jd.controlling.linkcrawler;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.Description;
import org.jdownloader.settings.annotations.AboutConfig;
import org.jdownloader.settings.annotations.RequiresRestart;

public interface LinkCrawlerConfig extends ConfigInterface {

    @DefaultIntValue(12)
    @AboutConfig
    @RequiresRestart
    @Description("max. number of linkcrawler threads")
    int getMaxThreads();

    void setMaxThreads(int i);

    @DefaultIntValue(20000)
    @AboutConfig
    @RequiresRestart
    @Description("max. time in ms before killing an idle linkcrawler thread")
    int getThreadKeepAlive();

    void setThreadKeepAlive(int i);

}
