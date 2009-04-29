package jd.gui.skins.simple.startmenu.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.JMenuItem;

import jd.gui.skins.simple.JDAboutDialog;

public class AboutAction extends StartAction {

    @Override
    public void init() {
        this.setIconDim(new Dimension(24, 24));
        this.setIcon("gui.images.about");
        this.setShortDescription("gui.menu.action.about.desc");
        this.setName("gui.menu.action.about.name");
        this.setMnemonic("gui.menu.action.about.mnem", "gui.menu.action.about.name");
        this.setAccelerator("gui.menu.action.about.accel");
    }

    public void actionPerformed(ActionEvent e) {
        JDAboutDialog.showDialog();
        
    }

}
