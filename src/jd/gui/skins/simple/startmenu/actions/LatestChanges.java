package jd.gui.skins.simple.startmenu.actions;

import java.awt.event.ActionEvent;

import jd.gui.skins.simple.components.JLinkButton;

public class LatestChanges extends StartAction {

    private static final long serialVersionUID = 2705114922279833817L;

    public LatestChanges() {
        super("action.changes", "gui.images.help");
    }

    public void actionPerformed(ActionEvent e) {
        try {
            JLinkButton.openURL("http://jdownloader.org/changes/index");
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

}
