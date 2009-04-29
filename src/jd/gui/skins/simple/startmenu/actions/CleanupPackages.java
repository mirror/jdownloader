package jd.gui.skins.simple.startmenu.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import jd.controlling.DownloadController;

public class CleanupPackages extends StartAction {

    private static final long serialVersionUID = -7185006215784212976L;

    @Override
    public void init() {
        this.setIconDim(new Dimension(24, 24));
        this.setIcon("gui.images.delete");
        this.setShortDescription("gui.menu.action.remove.packages.desc");
        this.setName("gui.menu.action.remove.packages.name");
        this.setMnemonic("gui.menu.action.remove.packages.mnem", "gui.menu.action.remove.packages.name");
        this.setAccelerator("gui.menu.action.remove.packages.accel");
    }

    public void actionPerformed(ActionEvent e) {
        DownloadController.getInstance().removeCompletedPackages();
    }

    protected void interrupt() {       

    }

}
