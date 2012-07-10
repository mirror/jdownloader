package org.jdownloader.extensions.vlcstreaming;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.Description;

public interface VLCStreamingConfig extends ExtensionConfigInterface {

    @AboutConfig
    @DefaultStringValue("")
    @Description("customized path to vlc binary")
    String getVLCCommand();

    void setVLCCommand(String vlc);
}
