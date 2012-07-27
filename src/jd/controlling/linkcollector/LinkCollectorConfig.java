package jd.controlling.linkcollector;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;

public interface LinkCollectorConfig extends ConfigInterface {

    @DefaultBooleanValue(true)
    @AboutConfig
    @RequiresRestart
    @DescriptionForConfigEntry("check links for on/offline status")
    boolean getDoLinkCheck();

    void setDoLinkCheck(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    @RequiresRestart
    @DescriptionForConfigEntry("use top(true) or bottom(false) position for merge")
    boolean getDoMergeTopBottom();

    void setDoMergeTopBottom(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry("autoexpand packages in linkcollector")
    boolean isPackageAutoExpanded();

    void setPackageAutoExpanded(boolean b);

}