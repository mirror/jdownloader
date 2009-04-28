package jd.gui.skins.simple.startmenu.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.JMenuItem;

import jd.gui.UserIO;

public class AddUrlAction extends StartAction {

    @Override
    public void init() {
        this.setIconDim(new Dimension(24, 24));
        this.setIcon("gui.images.url");
        this.setShortDescription("gui.menu.action.addurl.desc");
        this.setName("gui.menu.action.addurl.name");
        this.setMnemonic("gui.menu.addurl.action.mnem", "gui.menu.action.addurl.name");
        this.setAccelerator("gui.menu.action.addurl.accel");
        
    }

    public void actionPerformed(ActionEvent e) {
     
        
    }

}
