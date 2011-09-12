package org.jdownloader.api;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.Description;
import org.jdownloader.settings.annotations.AboutConfig;
import org.jdownloader.settings.annotations.RequiresRestart;

public interface RemoteAPIConfig extends ConfigInterface {

    @DefaultIntValue(3128)
    @AboutConfig
    @RequiresRestart
    @Description("remoteapi will listen on this port")
    int getAPIPort();

    void setAPIPort(int i);

    @DefaultBooleanValue(true)
    @AboutConfig
    @RequiresRestart
    @Description("enable/disable the remoteapi")
    boolean getAPIEnabled();

    void setAPIEnabled(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    @RequiresRestart
    @Description("remoteapi will listen on localhost only")
    boolean getAPIlocalhost();

    void setAPIlocalhost(boolean b);
}
