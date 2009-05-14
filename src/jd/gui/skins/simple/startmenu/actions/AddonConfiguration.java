package jd.gui.skins.simple.startmenu.actions;

import java.awt.event.ActionEvent;

import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.gui.skins.simple.tasks.ConfigTaskPane;

public class AddonConfiguration extends StartAction {

    public AddonConfiguration() {
        super("action.addonconfig", "gui.images.taskpanes.addons");
    }

    public void actionPerformed(ActionEvent e) {
        SimpleGuiConstants.GUI_CONFIG.setProperty("LAST_CONFIG_PANEL", ConfigTaskPane.ACTION_ADDONS);
        SimpleGUI.CURRENTGUI.getTaskPane().switcher(SimpleGUI.CURRENTGUI.getCfgTskPane());
    }

}
