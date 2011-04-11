package jd.gui.swing.jdgui;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultStringValue;

public interface GuiConfig extends ConfigInterface {

    void setActiveConfigPanel(String name);

    String getActiveConfigPanel();

    @DefaultStringValue("standard")
    String getThemeID();
}
