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

public class DeleteSelectedAndFailedLinksAction extends AppAction {

    private SelectionInfo<FilePackage, DownloadLink> si;

    public DeleteSelectedAndFailedLinksAction(SelectionInfo<FilePackage, DownloadLink> si) {

        this.si = si;
        setName(_GUI._.DeleteFailedAction_DeleteFailedAction_object_());
        setIconKey("remove_failed");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        
        List<DownloadLink> nodesToDelete = new ArrayList<DownloadLink>();
        for (DownloadLink dl : si.getChildren()) {
            if (dl.getLinkStatus().isFailed()) {
                nodesToDelete.add(dl);
            }
        }
        DownloadController.deleteLinksRequest(new SelectionInfo<FilePackage, DownloadLink>(null, nodesToDelete), _GUI._.DeleteFailedFromListAndDiskAction_actionPerformed());

    }

}
