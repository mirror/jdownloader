package jd.gui.skins.simple.startmenu.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

public class RestartAction extends StartAction {

    private static final long serialVersionUID = 1333126351380171619L;

    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void init() {
        setIconDim(new Dimension(24, 24));
        setIcon("gui.images.restart");
        this.setShortDescription("gui.menu.action.restart.desc");
        this.setName("gui.menu.action.restart.name");
        this.setMnemonic("gui.menu.action.restart.mnem", "gui.menu.restart.name");
        this.setAccelerator("gui.menu.action.restart.accel");

    }

}
