package org.jdownloader.extensions.infobar;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;

import jd.plugins.ExtensionConfigInterface;

public interface InfoBarConfig extends ExtensionConfigInterface {
    @DefaultBooleanValue(true)
    @AboutConfig
    public boolean isDragAndDropEnabled();

    public void setDragAndDropEnabled(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    public boolean isWindowVisible();

    public void setWindowVisible(boolean b);

    @DefaultIntValue(80)
    @DescriptionForConfigEntry("Java 1.7 or higher required!")
    @AboutConfig
    public int getTransparency();

    public void setTransparency(int i);

}
