package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;

public class DeleteSelectedFinishedLinksAction extends DeleteAppAction {

    public DeleteSelectedFinishedLinksAction(SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);
        setName(_GUI._.DeleteSuccessFulAction_DeleteSuccessFulAction_object_());
        setIconKey("remove_ok");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        List<DownloadLink> nodesToDelete = new ArrayList<DownloadLink>();
        for (DownloadLink dl : getSelection().getChildren()) {
            if (dl.getLinkStatus().isFinished()) {
                nodesToDelete.add(dl);
            }
        }
        deleteLinksRequest(new SelectionInfo<FilePackage, DownloadLink>(null, nodesToDelete, null, null, e, DownloadsTableModel.getInstance().getTable()), _GUI._.DeleteSelectedFinishedLinksAction_actionPerformed());
    }

    @Override
    public boolean isEnabled() {
        if (super.isEnabled()) {
            for (DownloadLink dl : getSelection().getChildren()) {
                if (dl.getLinkStatus().isFinished()) return true;
            }
        }
        return false;
    }

}
