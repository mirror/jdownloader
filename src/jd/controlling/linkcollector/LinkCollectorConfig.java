package jd.controlling.linkcollector;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultLongValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;

public interface LinkCollectorConfig extends ConfigInterface {

    @AboutConfig
    @DefaultBooleanValue(true)
    @RequiresRestart("A JDownloader Restart is Required")
    boolean isDupeManagerEnabled();

    void setDupeManagerEnabled(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("check links for on/offline status")
    boolean getDoLinkCheck();

    void setDoLinkCheck(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("use top(true) or bottom(false) position for merge")
    boolean getDoMergeTopBottom();

    void setDoMergeTopBottom(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry("autoexpand packages in linkcollector")
    boolean isPackageAutoExpanded();

    void setPackageAutoExpanded(boolean b);

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