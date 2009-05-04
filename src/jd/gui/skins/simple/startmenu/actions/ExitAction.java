package jd.gui.skins.simple.startmenu.actions;

import java.awt.event.ActionEvent;

import jd.gui.skins.simple.SimpleGUI;

public class ExitAction extends StartAction {

    private static final long serialVersionUID = -1428029294638573437L;

    public ExitAction() {
        super("action.exit", "gui.images.exit");
    }

    public void actionPerformed(ActionEvent e) {
        SimpleGUI.CURRENTGUI.closeWindow();
    }

}
