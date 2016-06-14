package org.jdownloader.phantomjs;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;

public interface PhantomJSConfig extends ConfigInterface {
    @AboutConfig
    @DescriptionForConfigEntry("Set a custom absolute Path to PhantomJS Binaries")
    public String getCustomBinaryPath();

    public void setCustomBinaryPath(String path);

}
