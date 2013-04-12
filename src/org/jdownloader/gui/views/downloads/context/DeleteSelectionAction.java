package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;

import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class DeleteSelectionAction extends AppAction {

    private static final long                        serialVersionUID = -5721724901676405104L;

    private SelectionInfo<FilePackage, DownloadLink> si;

    public DeleteSelectionAction(SelectionInfo<FilePackage, DownloadLink> si) {

        this.si = si;
        setIconKey("delete");
        setName(_GUI._.gui_table_contextmenu_deletelist2());

    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;

        final SelectionInfo<FilePackage, DownloadLink> si = this.si;
        String msg = _GUI._.RemoveSelectionAction_actionPerformed_();
        DownloadController.deleteLinksRequest(si, msg);

    }

    @Override
    public boolean isEnabled() {

        return !si.isEmpty();
    }

}