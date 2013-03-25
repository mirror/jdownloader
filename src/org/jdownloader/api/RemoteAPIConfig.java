package org.jdownloader.api;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;

public interface RemoteAPIConfig extends ConfigInterface {

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
