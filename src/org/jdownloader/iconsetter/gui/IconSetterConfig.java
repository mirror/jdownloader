package org.jdownloader.iconsetter.gui;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultIntValue;

public interface IconSetterConfig extends ConfigInterface {
    @DefaultIntValue(0)
    public int getColor();

    public void setColor(int i);
}
