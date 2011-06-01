package org.jdownloader.controlling;

import java.util.ArrayList;

import jd.gui.swing.jdgui.views.settings.components.LinkgrabberFilter.LinkFilter;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultObjectValue;
import org.jdownloader.settings.AboutConfig;

public interface LinkFilterSettings extends ConfigInterface {
    @DefaultObjectValue("[]")
    @AboutConfig
    ArrayList<LinkFilter> getFilterList();

    void setFilterList(ArrayList<LinkFilter> list);

    @DefaultBooleanValue(true)
    @AboutConfig
    boolean isBlackList();

    void setBlackList(boolean b);

}
