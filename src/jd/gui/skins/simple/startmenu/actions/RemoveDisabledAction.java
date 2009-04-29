package jd.gui.skins.simple.startmenu.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.JMenuItem;

public class RemoveDisabledAction extends StartAction {
    @Override
    public void init() {
        this.setIconDim(new Dimension(24, 24));
        this.setIcon("gui.images.remove_disabled");
        this.setShortDescription("gui.menu.action.remove_disabled.desc");
        this.setName("gui.menu.action.remove_disabled.name");
        this.setMnemonic("gui.menu.action.remove_disabled.mnem", "gui.menu.action.remove_disabled.name");
        this.setAccelerator("gui.menu.action.remove_disabled.accel");
        
    }

    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        
    }
}
