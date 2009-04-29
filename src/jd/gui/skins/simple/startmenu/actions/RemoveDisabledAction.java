package jd.gui.skins.simple.startmenu.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Vector;

import jd.controlling.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public class RemoveDisabledAction extends StartAction {
    /**
     * 
     */
    private static final long serialVersionUID = -5335194420202699757L;

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
        DownloadController dlc = DownloadController.getInstance();
        Vector<DownloadLink> downloadstodelete = new Vector<DownloadLink>();
        synchronized (dlc.getPackages()) {
            for (FilePackage fp : dlc.getPackages()) {
                synchronized (fp.getDownloadLinks()) {
                    for (DownloadLink dl : fp.getDownloadLinks()) {
                        if (!dl.isEnabled()) downloadstodelete.add(dl);
                    }
                }
            }
        }
        for (DownloadLink dl : downloadstodelete) {
            dl.getFilePackage().remove(dl);
        }
    }
}
