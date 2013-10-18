package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.plugins.FinalLinkState;

public class DeleteSelectedFinishedLinksAction extends AbstractDeleteSelectionFromDownloadlistAction {

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
            if (FinalLinkState.CheckFinished(dl.getFinalLinkState())) {
                nodesToDelete.add(dl);
            }
        }
        DownloadTabActionUtils.deleteLinksRequest(new SelectionInfo<FilePackage, DownloadLink>(null, nodesToDelete, null, null, e, DownloadsTableModel.getInstance().getTable()), _GUI._.DeleteSelectedFinishedLinksAction_actionPerformed());
    }

    @Override
    public boolean isEnabled() {
        if (super.isEnabled()) {
            for (DownloadLink dl : getSelection().getChildren()) {
                if (FinalLinkState.CheckFinished(dl.getFinalLinkState())) return true;
            }
        }
        return false;
    }

}
