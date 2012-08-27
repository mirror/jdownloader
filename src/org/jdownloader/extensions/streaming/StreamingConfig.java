package org.jdownloader.extensions.streaming;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.extensions.streaming.gui.bottombar.MediaArchiveSearchCategory;

public interface StreamingConfig extends ExtensionConfigInterface {

    @AboutConfig
    @DefaultStringValue("")
    @DescriptionForConfigEntry("customized path to vlc binary")
    String getVLCCommand();

    void setVLCCommand(String vlc);

    @DefaultEnumValue("FILENAME")
    MediaArchiveSearchCategory getSelectedSearchCategory();

    void setSelectedSearchCategory(MediaArchiveSearchCategory selectedCategory);

    @DefaultIntValue(3128)
    @AboutConfig
    void setStreamServerPort(int value);

    int getStreamServerPort();

    @DefaultIntValue(8896)
    int getUpnpHttpServerPort();

    void setUpnpHttpServerPort(int value);

}
