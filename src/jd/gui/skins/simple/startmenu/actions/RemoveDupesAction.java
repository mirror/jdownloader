package jd.gui.skins.simple.startmenu.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.JMenuItem;

public class RemoveDupesAction extends StartAction {
    @Override
    public void init() {
        this.setIconDim(new Dimension(24, 24));
        this.setIcon("gui.images.remove_dupes");
        this.setShortDescription("gui.menu.action.remove_dupes.desc");
        this.setName("gui.menu.action.remove_dupes.name");
        this.setMnemonic("gui.menu.action.addurl.mnem", "gui.menu.action.remove_dupes.name");
        this.setAccelerator("gui.menu.action.remove_dupes.accel");
        
    }

    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        
    }
}
