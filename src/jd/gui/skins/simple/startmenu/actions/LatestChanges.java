package jd.gui.skins.simple.startmenu.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import jd.gui.skins.simple.components.JLinkButton;

public class LatestChanges extends StartAction {

    /**
     * 
     */
    private static final long serialVersionUID = 2705114922279833817L;

    @Override
    public void init() {
        this.setIconDim(new Dimension(24, 24));
        this.setIcon("gui.images.help");
        this.setShortDescription("gui.menu.action.changes.desc");
        this.setName("gui.menu.action.changes.name");
        this.setMnemonic("gui.menu.action.changes.mnem", "gui.menu.action.changes.name");
        this.setAccelerator("gui.menu.action.changes.accel");
    }

    public void actionPerformed(ActionEvent e) {
       try {
        JLinkButton.openURL("http://jdownloader.org/changes/index");
    } catch (Exception e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
    }
        
    }

}
