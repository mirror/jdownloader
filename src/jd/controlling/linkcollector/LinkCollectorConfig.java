package jd.controlling.linkcollector;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.Description;
import org.jdownloader.settings.annotations.AboutConfig;
import org.jdownloader.settings.annotations.RequiresRestart;

public interface LinkCollectorConfig extends ConfigInterface {

    @DefaultBooleanValue(true)
    @AboutConfig
    @RequiresRestart
    @Description("check links for on/offline status")
    boolean getDoLinkCheck();

    void setDoLinkCheck(boolean b);
}