package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class DeleteSelectedFinishedLinksAction extends AppAction {

    private SelectionInfo<FilePackage, DownloadLink> si;

    public DeleteSelectedFinishedLinksAction(SelectionInfo<FilePackage, DownloadLink> si) {

        this.si = si;
        setName(_GUI._.DeleteSuccessFulAction_DeleteSuccessFulAction_object_());
        setIconKey("remove_ok");

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        List<DownloadLink> nodesToDelete = new ArrayList<DownloadLink>();

        if (si != null) {
            for (DownloadLink dl : si.getChildren()) {
                if (dl.getLinkStatus().isFinished()) {
                    nodesToDelete.add(dl);
                }
            }
        } else {
            DownloadController dlc = DownloadController.getInstance();
            final boolean readL = dlc.readLock();
            try {
                for (FilePackage fp : dlc.getPackages()) {
                    synchronized (fp) {
                        for (DownloadLink dl : fp.getChildren()) {
                            if (dl.getLinkStatus().isFinished())
                                nodesToDelete.add(dl);
                        }
                    }
                }
            } finally {
                dlc.readUnlock(readL);
            }
        }

        DownloadController.deleteLinksRequest(new SelectionInfo<FilePackage, DownloadLink>(null, nodesToDelete),
                _GUI._.DeleteSelectedFinishedLinksAction_actionPerformed(si != null ? "selected & " : ""));
    }

}
