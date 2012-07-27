package org.jdownloader.api;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;

public interface RemoteAPIConfig extends ConfigInterface {

    @DefaultIntValue(3128)
    @AboutConfig
    @RequiresRestart
    @DescriptionForConfigEntry("remoteapi will listen on this port")
    int getAPIPort();

    void setAPIPort(int i);

    @DefaultBooleanValue(true)
    @AboutConfig
    @RequiresRestart
    @DescriptionForConfigEntry("enable/disable the remoteapi")
    boolean getAPIEnabled();

    void setAPIEnabled(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    @RequiresRestart
    @DescriptionForConfigEntry("remoteapi will listen on localhost only")
    boolean getAPIlocalhost();

    void setAPIlocalhost(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    @RequiresRestart
    @DescriptionForConfigEntry("ExternInterface(Cnl2,Flashgot) will listen on 9666")
    boolean getExternInterfaceEnabled();

    void setExternInterfaceEnabled(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("ExternInterface(Cnl2,Flashgot) Authorized Websites")
    ArrayList<String> getExternInterfaceAuth();

    void setExternInterfaceAuth(ArrayList<String> auth);
}
