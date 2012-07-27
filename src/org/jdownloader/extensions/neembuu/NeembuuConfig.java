package org.jdownloader.extensions.neembuu;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.PlainStorage;

@PlainStorage
public interface NeembuuConfig extends ExtensionConfigInterface {
    // adds the enbtry to jd advanced config table

    @DescriptionForConfigEntry("System dependent path of vlc binary")
    String getVLCPath();

    void setVLCPath(String vlcPath);

    @DescriptionForConfigEntry("Basic mount location folder path")
    String getBasicMountLocation();

    void setBasicMountLocation(String basicMountLocation);

}
