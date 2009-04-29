package jd.gui.skins.simple.startmenu.actions;

import java.awt.event.ActionEvent;
import java.util.Vector;

import jd.controlling.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;

public class CleanupDownloads extends StartAction {

    private static final long serialVersionUID = -7185006215784212976L;

    public CleanupDownloads() {
        super("action.remove.links", "gui.images.delete");
    }

    public void actionPerformed(ActionEvent e) {
        DownloadController dlc = DownloadController.getInstance();
        Vector<DownloadLink> downloadstodelete = new Vector<DownloadLink>();
        synchronized (dlc.getPackages()) {
            for (FilePackage fp : dlc.getPackages()) {
                downloadstodelete.addAll(fp.getLinksWithStatus(LinkStatus.FINISHED));
            }
        }
        for (DownloadLink dl : downloadstodelete) {
            dl.getFilePackage().remove(dl);
        }
    }

}
