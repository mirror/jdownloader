package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;

public class DeleteDisabledSelectedLinks extends AbstractDeleteSelectionFromDownloadlistAction {

    /**
     * 
     */
    private static final long serialVersionUID = 5081124258090555549L;

    public DeleteDisabledSelectedLinks(SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);
        setName(_GUI._.DeleteDisabledLinks_DeleteDisabledLinks_object_());
        setIconKey("remove_disabled");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        List<DownloadLink> nodesToDelete = new ArrayList<DownloadLink>();
        for (DownloadLink dl : getSelection().getChildren()) {
            if (!dl.isEnabled()) {
                nodesToDelete.add(dl);
            }
        }

        DownloadTabActionUtils.deleteLinksRequest(new SelectionInfo<FilePackage, DownloadLink>(null, nodesToDelete, null, null, e, DownloadsTableModel.getInstance().getTable()), _GUI._.DeleteDisabledLinksFromListAndDiskAction_actionPerformed_object_());
    }

    @Override
    public boolean isEnabled() {
        if (super.isEnabled()) {
            for (DownloadLink dl : getSelection().getChildren()) {
                if (!dl.isEnabled()) return true;
            }
        }
        return false;
    }

}
