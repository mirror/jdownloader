package jd.gui.skins.simple.startmenu.actions;

import java.awt.event.ActionEvent;

import jd.gui.skins.simple.components.JLinkButton;

public class KnowledgeAction extends StartAction {

    private static final long serialVersionUID = 2227665710503234763L;

    public KnowledgeAction() {
        super("action.help", "gui.images.help");
    }

    public void actionPerformed(ActionEvent e) {
        try {
            JLinkButton.openURL("http://jdownloader.org/knowledge/index");
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

}
