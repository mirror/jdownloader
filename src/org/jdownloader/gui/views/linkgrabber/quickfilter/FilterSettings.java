package org.jdownloader.gui.views.linkgrabber.quickfilter;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultBooleanValue;

public interface FilterSettings extends ConfigInterface {
    @DefaultBooleanValue(true)
    public boolean isEnabled();

    public void setEnabled(boolean b);
}
