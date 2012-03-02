package org.jdownloader.update;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultLongValue;
import org.appwork.storage.config.annotations.Description;
import org.appwork.storage.config.annotations.RequiresRestart;

public interface WebupdateSettings extends ConfigInterface {

    @DefaultLongValue(30 * 60 * 1000)
    @AboutConfig
    @Description("[MS] How often shall JD do a silent Updatecheck.")
    long getUpdateInterval();

    void setUpdateInterval(long intervalMS);

    @AboutConfig
    @DefaultBooleanValue(true)
    @RequiresRestart
    @Description("If true, JDownloader will check for updates in an interval(see UpdateIntercal)")
    boolean isAutoUpdateCheckEnabled();

    void setAutoUpdateCheckEnabled(boolean enabled);
}
