package jd.gui.skins.simple.startmenu.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import jd.gui.skins.simple.SimpleGUI;

public class AddLinksAction extends StartAction {

    private static final long serialVersionUID = -6021279859279954997L;

    @Override
    public void init() {
        this.setIconDim(new Dimension(24, 24));
        this.setIcon("gui.images.addlinktext");
        this.setShortDescription("gui.menu.action.addlinktext.desc");
        this.setName("gui.menu.action.addlinktext.name");
        this.setMnemonic("gui.menu.action.addlinktext.mnem", "gui.menu.action.addlinktext.name");
        this.setAccelerator("gui.menu.action.addlinktext.accel");
    }

    public void actionPerformed(ActionEvent e) {
SimpleGUI.CURRENTGUI.getTaskPane().switcher(SimpleGUI.CURRENTGUI.getLgTaskPane());
    }

}
