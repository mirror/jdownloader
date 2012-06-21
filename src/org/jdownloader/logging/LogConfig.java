package org.jdownloader.logging;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.Description;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;

public interface LogConfig extends ConfigInterface {

    @AboutConfig
    @DefaultIntValue(1024 * 1024)
    @SpinnerValidator(min = 100 * 1024, max = Integer.MAX_VALUE)
    @Description("Max logfile size in bytes")
    @RequiresRestart
    int getMaxLogFileSize();

    void setMaxLogFileSize(int s);

    @AboutConfig
    @DefaultIntValue(5)
    @SpinnerValidator(min = 1, max = Integer.MAX_VALUE)
    @Description("Max number of logfiles for each logger")
    @RequiresRestart
    int getMaxLogFiles();

    void setMaxLogFiles(int m);

    @AboutConfig
    @DefaultIntValue(60)
    @SpinnerValidator(min = 30, max = Integer.MAX_VALUE)
    @Description("Timeout in secs after which the logger will be flushed/closed")
    @RequiresRestart
    int getLogFlushTimeout();

    void setLogFlushTimeout(int t);

    @AboutConfig
    @DefaultBooleanValue(false)
    @Description("Enable debug mode, nearly everything will be logged!")
    @RequiresRestart
    boolean isDebugModeEnabled();

    void setDebugModeEnabled(boolean b);
}
