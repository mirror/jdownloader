package jd.gui.skins.simple.startmenu.actions;

import java.awt.event.ActionEvent;

import jd.gui.skins.simple.JDAboutDialog;

public class AboutAction extends StartAction {

    private static final long serialVersionUID = -353145605693194634L;

    public AboutAction() {
        super("action.about", "gui.images.about");
    }

    public void actionPerformed(ActionEvent e) {
        JDAboutDialog.showDialog();

    }

}
