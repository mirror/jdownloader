package jd.gui.skins.simple.startmenu.actions;

import java.awt.event.ActionEvent;
import java.util.Vector;

import jd.controlling.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;

public class RemoveDupesAction extends StartAction {

    private static final long serialVersionUID = -4068088102973973923L;

    public RemoveDupesAction() {
        super("action.remove_dupes", "gui.images.remove_dupes");
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
