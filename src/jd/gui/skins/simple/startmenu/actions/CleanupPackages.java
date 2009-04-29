package jd.gui.skins.simple.startmenu.actions;

import java.awt.event.ActionEvent;
import java.util.Vector;

import jd.controlling.DownloadController;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;

public class CleanupPackages extends StartAction {

    private static final long serialVersionUID = -7185006215784212976L;

    public CleanupPackages() {
        super("action.remove.packages", "gui.images.delete");
    }

    public void actionPerformed(ActionEvent e) {
        DownloadController dlc = DownloadController.getInstance();
        Vector<FilePackage> packagestodelete = new Vector<FilePackage>();
        synchronized (dlc.getPackages()) {
            for (FilePackage fp : dlc.getPackages()) {
                if (fp.getLinksWithStatus(LinkStatus.FINISHED).size() == fp.size()) packagestodelete.add(fp);
            }
        }
        for (FilePackage fp : packagestodelete) {
            dlc.removePackage(fp);
        }
    }

}
