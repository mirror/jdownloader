package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;

public class DeleteSelectedOfflineLinksAction extends AppAction {

    private SelectionInfo<FilePackage, DownloadLink> si;

    public DeleteSelectedOfflineLinksAction(SelectionInfo<FilePackage, DownloadLink> si) {

        this.si = si;
        setName(_GUI._.DeleteOfflineAction_DeleteOfflineAction_object_());
        setIconKey("remove_offline");
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        List<DownloadLink> nodesToDelete = new ArrayList<DownloadLink>();
        for (DownloadLink dl : si.getChildren()) {
            if (dl.getAvailableStatus() == AvailableStatus.FALSE || dl.getLinkStatus().hasStatus(LinkStatus.ERROR_FILE_NOT_FOUND)) {
                nodesToDelete.add(dl);
            }
        }
        DownloadController.deleteLinksRequest(new SelectionInfo<FilePackage, DownloadLink>(null, nodesToDelete, null, null, e, DownloadsTableModel.getInstance().getTable()), _GUI._.DeleteSelectedOfflineLinksAction_actionPerformed());

    }

}
