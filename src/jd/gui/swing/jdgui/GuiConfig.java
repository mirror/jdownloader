package jd.gui.swing.jdgui;

import org.appwork.storage.config.ConfigInterface;

public interface GuiConfig extends ConfigInterface {

    void setActiveConfigPanel(String name);

    String getActiveConfigPanel();
}
