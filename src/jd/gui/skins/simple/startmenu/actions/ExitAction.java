package jd.gui.skins.simple.startmenu.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

public class ExitAction extends StartAction {

    @Override
    public void init() {
        setIconDim(new Dimension(24,24));
        setIcon("gui.images.exit");
        this.setShortDescription("gui.menu.action.exit.desc");
        this.setName("gui.menu.action.exit.name");
        this.setMnemonic("gui.menu.exit.action.mnem", "gui.menu.action.exit.name");
        this.setAccelerator("gui.menu.action.exit.accel");

    }

    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub

    }

}
