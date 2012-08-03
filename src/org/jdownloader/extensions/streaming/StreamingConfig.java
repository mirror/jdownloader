package org.jdownloader.extensions.streaming;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;

public interface StreamingConfig extends ExtensionConfigInterface {

    @AboutConfig
    @DefaultStringValue("")
    @DescriptionForConfigEntry("customized path to vlc binary")
    String getVLCCommand();

    void setVLCCommand(String vlc);
}
