package jd.controlling.linkchecker;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;

public interface LinkCheckerConfig extends ConfigInterface {

    @DefaultIntValue(4)
    @AboutConfig
    @RequiresRestart
    @DescriptionForConfigEntry("max. number of linkchecking threads")
    int getMaxThreads();

    void setMaxThreads(int i);

    @DefaultIntValue(250)
    @AboutConfig
    @RequiresRestart
    @DescriptionForConfigEntry("max. time in ms before killing an idle linkcheck thread")
    int getThreadKeepAlive();

    void setThreadKeepAlive(int i);

}
