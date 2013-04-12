package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;

import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class DeleteSelectedLinks extends AppAction {

    private SelectionInfo<FilePackage, DownloadLink> si;

    public DeleteSelectedLinks(SelectionInfo<FilePackage, DownloadLink> si) {

        this.si = si;
        setName(_GUI._.DeleteAllAction_DeleteAllAction_object_());
        setIconKey("remove");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        DownloadController.deleteLinksRequest(si, _GUI._.RemoveSelectionAction_actionPerformed_());
    }

    @Override
    public boolean isEnabled() {
        return si != null && si.getChildren().size() > 0;
    }

}
