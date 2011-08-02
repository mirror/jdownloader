package jd.controlling.linkchecker;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.Description;
import org.jdownloader.settings.annotations.AboutConfig;
import org.jdownloader.settings.annotations.RequiresRestart;

public interface LinkCheckerConfig extends ConfigInterface {

    @DefaultIntValue(4)
    @AboutConfig
    @RequiresRestart
    @Description("max. number of threads used linkchecking")
    int getMaxThreads();

    void setMaxThreads(int i);

    @DefaultIntValue(250)
    @AboutConfig
    @RequiresRestart
    @Description("max. time in ms before killing an idle linkcheck thread")
    int getThreadKeepAlive();

    void setThreadKeepAlive(int i);

}
