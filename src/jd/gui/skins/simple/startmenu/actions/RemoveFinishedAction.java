package jd.gui.skins.simple.startmenu.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.JMenuItem;

public class RemoveFinishedAction extends StartAction {

    @Override
    public void init() {
        this.setIconDim(new Dimension(24, 24));
        this.setIcon("gui.images.remove_ok");
        this.setShortDescription("gui.menu.action.remove_ok.desc");
        this.setName("gui.menu.action.remove_ok.name");
        this.setMnemonic("gui.menu.action.addurl.mnem", "gui.menu.action.remove_ok.name");
        this.setAccelerator("gui.menu.action.remove_ok.accel");
        
    }

    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        
    }

}
