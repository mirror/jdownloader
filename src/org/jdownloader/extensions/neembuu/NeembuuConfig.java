package org.jdownloader.extensions.neembuu;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.Description;
import org.appwork.storage.config.annotations.PlainStorage;

@PlainStorage
public interface NeembuuConfig extends ExtensionConfigInterface {
    // adds the enbtry to jd advanced config table

    @Description("System dependent path of vlc binary")
    String getVLCPath();

    void setVLCPath(String vlcPath);

    @Description("Basic mount location folder path")
    String getBasicMountLocation();

    void setBasicMountLocation(String basicMountLocation);

}
