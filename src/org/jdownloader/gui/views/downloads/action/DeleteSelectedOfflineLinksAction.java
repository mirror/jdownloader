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

public class DeleteSelectedOfflineLinksAction extends AbstractDeleteSelectionFromDownloadlistAction {

    private SelectionInfo<FilePackage, DownloadLink> si;

    public DeleteSelectedOfflineLinksAction(SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);
        setName(_GUI._.DeleteOfflineAction_DeleteOfflineAction_object_());
        setIconKey("remove_offline");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        List<DownloadLink> nodesToDelete = new ArrayList<DownloadLink>();
        for (DownloadLink dl : getSelection().getChildren()) {
            if (dl.getFinalLinkState() == FinalLinkState.OFFLINE) {
                nodesToDelete.add(dl);
            }
        }
        DownloadTabActionUtils.deleteLinksRequest(new SelectionInfo<FilePackage, DownloadLink>(null, nodesToDelete, null, null, e, DownloadsTableModel.getInstance().getTable()), _GUI._.DeleteSelectedOfflineLinksAction_actionPerformed());

    }

    @Override
    public boolean isEnabled() {
        if (super.isEnabled()) {
            for (DownloadLink dl : getSelection().getChildren()) {
                if (dl.getFinalLinkState() == FinalLinkState.OFFLINE) { return true; }
            }
        }
        return false;
    }

}
