package jd.gui.skins.simple.startmenu.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import jd.controlling.DownloadController;

public class CleanupDownloads extends StartAction {

    private static final long serialVersionUID = -7185006215784212976L;

    @Override
    public void init() {
        this.setIconDim(new Dimension(24, 24));
        this.setIcon("gui.images.delete");
        this.setShortDescription("gui.menu.action.remove.links.desc");
        this.setName("gui.menu.action.remove.links.name");
        this.setMnemonic("gui.menu.action.remove.links.mnem", "gui.menu.action.remove.links.name");
        this.setAccelerator("gui.menu.action.remove.links.accel");
    }

    public void actionPerformed(ActionEvent e) {
        DownloadController.getInstance().removeCompletedDownloadLinks();
    }

    protected void interrupt() {       

    }

}
