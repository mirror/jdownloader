package jd.gui.skins.simple.startmenu.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Vector;

import jd.controlling.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;

public class RemoveDupesAction extends StartAction {
    /**
     * 
     */
    private static final long serialVersionUID = -4068088102973973923L;

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
        DownloadController dlc = DownloadController.getInstance();
        Vector<DownloadLink> downloadstodelete = new Vector<DownloadLink>();
        synchronized (dlc.getPackages()) {
            for (FilePackage fp : dlc.getPackages()) {
                downloadstodelete.addAll(fp.getLinksWithStatus(LinkStatus.ERROR_ALREADYEXISTS));
            }
        }
        for (DownloadLink dl : downloadstodelete) {
            dl.getFilePackage().remove(dl);
        }
    }
}
