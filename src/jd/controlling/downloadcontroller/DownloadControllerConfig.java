package jd.controlling.downloadcontroller;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultLongValue;
import org.appwork.storage.config.annotations.RequiresRestart;

public interface DownloadControllerConfig extends ConfigInterface {
    @AboutConfig
    @DefaultLongValue(5000)
    @RequiresRestart("A JDownloader Restart is Required")
    long getMinimumSaveDelay();

    void setMinimumSaveDelay(long delay);

    @AboutConfig
    @DefaultLongValue(60000)
    @RequiresRestart("A JDownloader Restart is Required")
    long getMaximumSaveDelay();

    void setMaximumSaveDelay(long delay);
}
