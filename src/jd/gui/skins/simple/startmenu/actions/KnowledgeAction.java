package jd.gui.skins.simple.startmenu.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import jd.gui.skins.simple.components.JLinkButton;

public class KnowledgeAction extends StartAction {

    /**
     * 
     */
    private static final long serialVersionUID = 2227665710503234763L;

    @Override
    public void init() {
        this.setIconDim(new Dimension(24, 24));
        this.setIcon("gui.images.help");
        this.setShortDescription("gui.menu.action.help.desc");
        this.setName("gui.menu.action.help.name");
        this.setMnemonic("gui.menu.action.about.mnem", "gui.menu.action.help.name");
        this.setAccelerator("gui.menu.action.help.accel");
    }

    public void actionPerformed(ActionEvent e) {
       try {
        JLinkButton.openURL("http://jdownloader.org/knowledge/index");
    } catch (Exception e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
    }
        
    }

}
