package jd.gui.skins.simple.startmenu.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.JMenuItem;

public class RemoveAllAction extends StartAction {

    @Override
    public void init() {
        this.setIconDim(new Dimension(24, 24));
        this.setIcon("gui.images.remove_all");
        this.setShortDescription("gui.menu.action.removeall.desc");
        this.setName("gui.menu.action.removeall.name");
        this.setMnemonic("gui.menu.action.removeall.mnem", "gui.menu.action.removeall.name");
        this.setAccelerator("gui.menu.action.removeall.accel");
        
    }

    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        
    }

}
