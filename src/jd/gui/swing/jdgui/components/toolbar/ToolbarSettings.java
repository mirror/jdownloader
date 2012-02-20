package jd.gui.swing.jdgui.components.toolbar;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;

public interface ToolbarSettings extends ConfigInterface {
    @AboutConfig
    public ArrayList<ActionConfig> getSetup();

    public void setSetup(ArrayList<ActionConfig> list);
}
